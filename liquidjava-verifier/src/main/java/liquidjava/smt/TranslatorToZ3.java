package liquidjava.smt;

import com.martiansoftware.jsap.SyntaxException;
import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FPExpr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.IntNum;
import com.microsoft.z3.RealExpr;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Sort;
import com.microsoft.z3.Status;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import liquidjava.diagnostics.errors.LJError;
import liquidjava.diagnostics.errors.NotFoundError;
import liquidjava.processor.context.AliasWrapper;
import liquidjava.utils.Utils;
import liquidjava.utils.constants.Keys;

import org.apache.commons.lang3.NotImplementedException;

public class TranslatorToZ3 implements AutoCloseable {

    private final com.microsoft.z3.Context z3 = new com.microsoft.z3.Context();
    private final Map<String, Expr<?>> varTranslation = new HashMap<>();
    private final Map<String, List<Expr<?>>> varSuperTypes = new HashMap<>();
    private final Map<String, AliasWrapper> aliasTranslation = new HashMap<>(); // this is not being used
    private final Map<String, FuncDecl<?>> funcTranslation = new HashMap<>();

    public TranslatorToZ3(liquidjava.processor.context.Context c) {
        TranslatorContextToZ3.translateVariables(z3, c.getContext(), varTranslation);
        TranslatorContextToZ3.storeVariablesSubtypes(z3, c.getAllVariablesWithSupertypes(), varSuperTypes);
        TranslatorContextToZ3.addAlias(c.getAlias(), aliasTranslation);
        TranslatorContextToZ3.addGhostFunctions(z3, c.getGhosts(), funcTranslation);
        TranslatorContextToZ3.addGhostStates(z3, c.getGhostState(), funcTranslation);
    }

    @SuppressWarnings("unchecked")
    public Status verifyExpression(Expr<?> e) {
        Solver s = z3.mkSolver();
        s.add((BoolExpr) e);
        return s.check();
    }

    // #####################Literals and Variables#####################
    public Expr<?> makeIntegerLiteral(int value) {
        return z3.mkInt(value);
    }

    public Expr<?> makeLongLiteral(long value) {
        return z3.mkReal(value);
    }

    public Expr<?> makeDoubleLiteral(double value) {
        return z3.mkFP(value, z3.mkFPSort64());
    }

    public Expr<?> makeString(String s) {
        return z3.mkString(s);
    }

    public Expr<?> makeBooleanLiteral(boolean value) {
        return z3.mkBool(value);
    }

    private Expr<?> getVariableTranslation(String name) throws LJError {
        if (!varTranslation.containsKey(name))
            throw new NotFoundError(name, Keys.VARIABLE);
        Expr<?> e = varTranslation.get(name);
        if (e == null)
            e = varTranslation.get(String.format("this#%s", name));
        if (e == null)
            throw new NotFoundError(name, Keys.VARIABLE);
        return e;
    }

    public Expr<?> makeVariable(String name) throws LJError {
        return getVariableTranslation(name); // int[] not in varTranslation
    }

    public Expr<?> makeFunctionInvocation(String name, Expr<?>[] params) throws LJError {
        if (name.equals("addToIndex"))
            return makeStore(params);
        if (name.equals("getFromIndex"))
            return makeSelect(params);
        FuncDecl<?> fd = funcTranslation.get(name);
        if (fd == null)
            fd = resolveFunctionDeclFallback(name, params);

        Sort[] s = fd.getDomain();
        for (int i = 0; i < s.length; i++) {
            Expr<?> param = params[i];
            if (!s[i].equals(param.getSort())) {
                // Look if the function type is a supertype of this
                List<Expr<?>> le = varSuperTypes.get(param.toString().replace("|", ""));
                if (le != null)
                    for (Expr<?> e : le)
                        if (e.getSort().equals(s[i]))
                            params[i] = e;
            }
        }
        return z3.mkApp(fd, params);
    }

    /**
     * Fallback resolver for function declarations when an exact qualified name lookup fails. Tries to match by simple
     * name and number of parameters, preferring an exact qualified-name match if found among candidates; otherwise
     * returns the first compatible candidate and relies on later coercion via var supertypes.
     */
    private FuncDecl<?> resolveFunctionDeclFallback(String name, Expr<?>[] params) throws LJError {
        String simple = Utils.getSimpleName(name);
        FuncDecl<?> candidate = null;
        for (Map.Entry<String, FuncDecl<?>> entry : funcTranslation.entrySet()) {
            String k = entry.getKey();
            String simpleK = Utils.getSimpleName(k);
            if (simple.equals(simpleK)) {
                FuncDecl<?> fTry = entry.getValue();
                Sort[] dom = fTry.getDomain();
                if (dom.length == params.length) {
                    // Prefer exact qualified name match if available
                    if (k.equals(name)) {
                        candidate = fTry;
                        break;
                    }
                    // Otherwise first compatible match
                    candidate = fTry;
                }
            }
        }
        if (candidate != null) {
            return candidate;
        }
        throw new NotFoundError(name, Keys.GHOST);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Expr<?> makeSelect(Expr<?>[] params) {
        if (params.length == 2 && params[0] instanceof ArrayExpr)
            return z3.mkSelect((ArrayExpr) params[0], params[1]);
        return null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Expr<?> makeStore(Expr<?>[] params) {
        if (params.length == 3 && params[0] instanceof ArrayExpr)
            return z3.mkStore((ArrayExpr) params[0], params[1], params[2]);
        return null;
    }

    // #####################Boolean Operations#####################
    public Expr<?> makeEquals(Expr<?> e1, Expr<?> e2) {
        if (e1 instanceof FPExpr || e2 instanceof FPExpr)
            return z3.mkFPEq(toFP(e1), toFP(e2));

        return z3.mkEq(e1, e2);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Expr<?> makeLt(Expr<?> e1, Expr<?> e2) {
        if (e1 instanceof FPExpr || e2 instanceof FPExpr)
            return z3.mkFPLt(toFP(e1), toFP(e2));

        return z3.mkLt((ArithExpr) e1, (ArithExpr) e2);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Expr<?> makeLtEq(Expr<?> e1, Expr<?> e2) {
        if (e1 instanceof FPExpr || e2 instanceof FPExpr)
            return z3.mkFPLEq(toFP(e1), toFP(e2));

        return z3.mkLe((ArithExpr) e1, (ArithExpr) e2);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Expr<?> makeGt(Expr<?> e1, Expr<?> e2) {
        if (e1 instanceof FPExpr || e2 instanceof FPExpr)
            return z3.mkFPGt(toFP(e1), toFP(e2));

        return z3.mkGt((ArithExpr) e1, (ArithExpr) e2);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Expr<?> makeGtEq(Expr<?> e1, Expr<?> e2) {
        if (e1 instanceof FPExpr || e2 instanceof FPExpr)
            return z3.mkFPGEq(toFP(e1), toFP(e2));

        return z3.mkGe((ArithExpr) e1, (ArithExpr) e2);
    }

    public Expr<?> makeImplies(Expr<?> e1, Expr<?> e2) {
        return z3.mkImplies((BoolExpr) e1, (BoolExpr) e2);
    }

    public Expr<?> makeBiconditional(Expr<?> eval, Expr<?> eval2) {
        return z3.mkIff((BoolExpr) eval, (BoolExpr) eval2);
    }

    public Expr<?> makeAnd(Expr<?> eval, Expr<?> eval2) {
        return z3.mkAnd((BoolExpr) eval, (BoolExpr) eval2);
    }

    public Expr<?> mkNot(Expr<?> e1) {
        return z3.mkNot((BoolExpr) e1);
    }

    public Expr<?> makeOr(Expr<?> eval, Expr<?> eval2) {
        return z3.mkOr((BoolExpr) eval, (BoolExpr) eval2);
    }

    // ##################### Unary Operations #####################
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Expr<?> makeMinus(Expr<?> eval) {
        if (eval instanceof FPExpr)
            return z3.mkFPNeg((FPExpr) eval);
        return z3.mkUnaryMinus((ArithExpr) eval);
    }

    // #####################Arithmetic Operations#####################
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Expr<?> makeAdd(Expr<?> eval, Expr<?> eval2) {
        if (eval instanceof FPExpr || eval2 instanceof FPExpr)
            return z3.mkFPAdd(z3.mkFPRoundNearestTiesToEven(), toFP(eval), toFP(eval2));

        return z3.mkAdd((ArithExpr) eval, (ArithExpr) eval2);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Expr<?> makeSub(Expr<?> eval, Expr<?> eval2) {
        if (eval instanceof FPExpr || eval2 instanceof FPExpr)
            return z3.mkFPSub(z3.mkFPRoundNearestTiesToEven(), toFP(eval), toFP(eval2));

        return z3.mkSub((ArithExpr) eval, (ArithExpr) eval2);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Expr<?> makeMul(Expr<?> eval, Expr<?> eval2) {
        if (eval instanceof FPExpr || eval2 instanceof FPExpr)
            return z3.mkFPMul(z3.mkFPRoundNearestTiesToEven(), toFP(eval), toFP(eval2));

        return z3.mkMul((ArithExpr) eval, (ArithExpr) eval2);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Expr<?> makeDiv(Expr<?> eval, Expr<?> eval2) {
        if (eval instanceof FPExpr || eval2 instanceof FPExpr)
            return z3.mkFPDiv(z3.mkFPRoundNearestTiesToEven(), toFP(eval), toFP(eval2));

        return z3.mkDiv((ArithExpr) eval, (ArithExpr) eval2);
    }

    public Expr<?> makeMod(Expr<?> eval, Expr<?> eval2) {
        if (eval instanceof FPExpr || eval2 instanceof FPExpr)
            return z3.mkFPRem(toFP(eval), toFP(eval2));
        return z3.mkMod((IntExpr) eval, (IntExpr) eval2);
    }

    private FPExpr toFP(Expr<?> e) {
        FPExpr f;
        if (e instanceof FPExpr) {
            f = (FPExpr) e;
        } else if (e instanceof IntNum)
            f = z3.mkFP(((IntNum) e).getInt(), z3.mkFPSort64());
        else if (e instanceof IntExpr ee) {
            RealExpr re = z3.mkInt2Real(ee);
            f = z3.mkFPToFP(z3.mkFPRoundNearestTiesToEven(), re, z3.mkFPSort64());
        } else if (e instanceof RealExpr) {
            f = z3.mkFPToFP(z3.mkFPRoundNearestTiesToEven(), (RealExpr) e, z3.mkFPSort64());
        } else {
            throw new NotImplementedException();
        }
        return f;
    }

    public Expr<?> makeIte(Expr<?> c, Expr<?> t, Expr<?> e) {
        if (c instanceof BoolExpr)
            return z3.mkITE((BoolExpr) c, t, e);
        throw new RuntimeException("Condition is not a boolean expression");
    }

    @Override
    public void close() throws Exception {
        z3.close();
    }
}
