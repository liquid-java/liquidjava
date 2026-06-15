package liquidjava.smt;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.BitVecNum;
import com.microsoft.z3.BitVecSort;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.EnumSort;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FPExpr;
import com.microsoft.z3.FPSort;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.IntNum;
import com.microsoft.z3.RealExpr;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Sort;
import com.microsoft.z3.Symbol;
import com.microsoft.z3.Model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import liquidjava.diagnostics.errors.LJError;
import liquidjava.diagnostics.errors.NotFoundError;
import liquidjava.processor.context.AliasWrapper;
import liquidjava.utils.Pair;
import liquidjava.utils.Utils;
import liquidjava.utils.constants.Formats;
import liquidjava.utils.constants.Keys;
import com.microsoft.z3.enumerations.Z3_sort_kind;

import org.apache.commons.lang3.NotImplementedException;

public class TranslatorToZ3 implements AutoCloseable {

    private final com.microsoft.z3.Context z3 = new com.microsoft.z3.Context();
    private final Map<String, Expr<?>> varTranslation = new HashMap<>();
    private final Map<String, List<Expr<?>>> varSuperTypes = new HashMap<>();
    private final Map<String, AliasWrapper> aliasTranslation = new HashMap<>(); // this is not being used
    private final Map<String, FuncDecl<?>> funcTranslation = new HashMap<>();
    private final Map<String, Expr<?>> funcAppTranslation = new HashMap<>();
    private final Map<Expr<?>, String> exprToNameTranslation = new HashMap<>();
    /**
     * Z3 constants for resolved {@code static final} references (e.g. {@code Integer.MAX_VALUE}). Keyed by the dotted
     * symbolic name so the same constant is reused across the query and the binding axiom is emitted once.
     */
    private final Map<String, Expr<?>> staticConstantTranslation = new HashMap<>();
    /**
     * Equality axioms binding each {@link #staticConstantTranslation} entry to its compile-time literal value.
     * Conjoined into every solver built by {@link #makeSolverForExpression} so the constant is always pinned but still
     * appears symbolically in counterexample models.
     */
    private final List<BoolExpr> staticConstantAxioms = new ArrayList<>();

    public TranslatorToZ3(liquidjava.processor.context.Context context) {
        TranslatorContextToZ3.translateVariables(z3, context.getContext(), varTranslation);
        TranslatorContextToZ3.storeVariablesSubtypes(z3, context.getAllVariablesWithSupertypes(), varSuperTypes);
        TranslatorContextToZ3.addAliases(context.getAliases(), aliasTranslation);
        TranslatorContextToZ3.addGhostFunctions(z3, context.getGhosts(), funcTranslation);
        TranslatorContextToZ3.addGhostStates(z3, context.getGhostStates(), funcTranslation);
    }

    @SuppressWarnings("unchecked")
    public Solver makeSolverForExpression(Expr<?> e) {
        Solver solver = z3.mkSolver();
        for (BoolExpr axiom : staticConstantAxioms)
            solver.add(axiom);
        solver.add((BoolExpr) e);
        return solver;
    }

    /**
     * Extracts the counterexample from the Z3 model
     */
    public Counterexample getCounterexample(Model model) {
        List<Pair<String, String>> assignments = new ArrayList<>();
        // Extract constant variable assignments
        for (FuncDecl<?> decl : model.getDecls()) {
            if (decl.getArity() == 0) {
                Symbol name = decl.getName();
                Expr<?> value = model.getConstInterp(decl);
                Pair<String, String> assignment = new Pair<>(name.toString(), value.toString());
                // Skip values of uninterpreted sorts
                if (value.getSort().getSortKind() != Z3_sort_kind.Z3_UNINTERPRETED_SORT)
                    assignments.add(assignment);
            }
        }
        // Extract function application values
        for (Map.Entry<String, Expr<?>> entry : funcAppTranslation.entrySet()) {
            String name = entry.getKey();
            Expr<?> application = entry.getValue();
            Expr<?> value = model.eval(application, true);
            Pair<String, String> assignment = new Pair<>(name, value.toString());
            assignments.add(assignment);
        }
        return new Counterexample(assignments);
    }

    // #####################Literals and Variables#####################
    public Expr<?> makeIntegerLiteral(int value) {
        // Java int/short/char/byte literals are 32-bit two's-complement values.
        return z3.mkBV(value, 32);
    }

    public Expr<?> makeLongLiteral(long value) {
        // Java long literals are 64-bit two's-complement values.
        return z3.mkBV(value, 64);
    }

    public Expr<?> makeDoubleLiteral(double value) {
        return z3.mkFP(value, z3.mkFPSort64());
    }

    /**
     * Java {@code float} literals are single-precision (binary32). The caller has already rounded the value to a
     * {@code float}, so emitting it into an {@link com.microsoft.z3.Context#mkFPSort32() FPSort32} constant preserves
     * the single-precision rounding that a binary64 model would silently lose.
     */
    public Expr<?> makeFloatLiteral(float value) {
        return z3.mkFP(value, z3.mkFPSort32());
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
        Expr<?> expr = getVariableTranslation(name); // int[] not in varTranslation
        exprToNameTranslation.put(expr, name); // Track for readable labels
        return expr;
    }

    public Expr<?> makeEnum(String enumTypeName, String enumConstantName) throws LJError {
        String varName = String.format(Formats.ENUM, enumTypeName, enumConstantName);
        return getVariableTranslation(varName);
    }

    /**
     * Declare (or look up) a Z3 constant named {@code Type.CONST} for a resolved Java {@code static final} reference,
     * and emit a single equality axiom binding it to its compile-time literal value. Subsequent calls with the same
     * name reuse the existing constant. The axiom is added to every solver built via {@link #makeSolverForExpression},
     * so the constant always evaluates to its literal value while still appearing symbolically in error messages and
     * counterexample models.
     */
    public Expr<?> makeStaticConstant(String typeName, String constName, Expr<?> literalValue) {
        String name = String.format(Formats.ENUM, typeName, constName);
        Expr<?> cached = staticConstantTranslation.get(name);
        if (cached != null)
            return cached;
        Expr<?> constant = z3.mkConst(name, literalValue.getSort());
        staticConstantTranslation.put(name, constant);
        staticConstantAxioms.add(z3.mkEq(constant, literalValue));
        return constant;
    }

    public Expr<?> makeFunctionInvocation(String name, Expr<?>[] params) throws LJError {
        if (name.equals("addToIndex"))
            return makeStore(params);
        if (name.equals("getFromIndex"))
            return makeSelect(params);
        if (name.equals(Keys.TRUNCATE))
            return makeTruncateToZero(params[0]);
        FuncDecl<?> fd = funcTranslation.get(name);
        if (fd == null)
            fd = resolveFunctionDecl(name, params);

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
        String label = buildFunctionLabel(name, params);
        Expr<?> app = z3.mkApp(fd, params);
        funcAppTranslation.put(label, app);
        return app;
    }

    /**
     * Gets function declarations when an exact qualified name lookup fails. Tries to match by simple name and number of
     * parameters, preferring an exact qualified-name match if found among candidates; otherwise returns the first
     * compatible candidate and relies on later coercion via var supertypes.
     */
    private FuncDecl<?> resolveFunctionDecl(String name, Expr<?>[] params) throws LJError {
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
        if (e1 instanceof FPExpr || e2 instanceof FPExpr) {
            FPSort s = commonFPSort(e1, e2);
            return z3.mkFPEq(toFP(e1, s), toFP(e2, s));
        }
        if (bothBitVec(e1, e2)) {
            BitVecExpr[] u = unifyBV(e1, e2);
            return z3.mkEq(u[0], u[1]);
        }
        if (e1 instanceof RealExpr || e2 instanceof RealExpr)
            return z3.mkEq(toReal(e1), toReal(e2));
        // Booleans, enums, strings, and ill-typed comparisons (e.g. a boolean against an integral literal) fall
        // here; mkEq surfaces a proper sort-mismatch error rather than silently coercing.
        return z3.mkEq(e1, e2);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Expr<?> makeLt(Expr<?> e1, Expr<?> e2) {
        if (e1 instanceof FPExpr || e2 instanceof FPExpr) {
            FPSort s = commonFPSort(e1, e2);
            return z3.mkFPLt(toFP(e1, s), toFP(e2, s));
        }
        if (bothBitVec(e1, e2)) {
            BitVecExpr[] u = unifyBV(e1, e2);
            return z3.mkBVSLT(u[0], u[1]);
        }
        if (e1 instanceof RealExpr || e2 instanceof RealExpr)
            return z3.mkLt(toReal(e1), toReal(e2));
        return z3.mkLt((ArithExpr) e1, (ArithExpr) e2);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Expr<?> makeLtEq(Expr<?> e1, Expr<?> e2) {
        if (e1 instanceof FPExpr || e2 instanceof FPExpr) {
            FPSort s = commonFPSort(e1, e2);
            return z3.mkFPLEq(toFP(e1, s), toFP(e2, s));
        }
        if (bothBitVec(e1, e2)) {
            BitVecExpr[] u = unifyBV(e1, e2);
            return z3.mkBVSLE(u[0], u[1]);
        }
        if (e1 instanceof RealExpr || e2 instanceof RealExpr)
            return z3.mkLe(toReal(e1), toReal(e2));
        return z3.mkLe((ArithExpr) e1, (ArithExpr) e2);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Expr<?> makeGt(Expr<?> e1, Expr<?> e2) {
        if (e1 instanceof FPExpr || e2 instanceof FPExpr) {
            FPSort s = commonFPSort(e1, e2);
            return z3.mkFPGt(toFP(e1, s), toFP(e2, s));
        }
        if (bothBitVec(e1, e2)) {
            BitVecExpr[] u = unifyBV(e1, e2);
            return z3.mkBVSGT(u[0], u[1]);
        }
        if (e1 instanceof RealExpr || e2 instanceof RealExpr)
            return z3.mkGt(toReal(e1), toReal(e2));
        return z3.mkGt((ArithExpr) e1, (ArithExpr) e2);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Expr<?> makeGtEq(Expr<?> e1, Expr<?> e2) {
        if (e1 instanceof FPExpr || e2 instanceof FPExpr) {
            FPSort s = commonFPSort(e1, e2);
            return z3.mkFPGEq(toFP(e1, s), toFP(e2, s));
        }
        if (bothBitVec(e1, e2)) {
            BitVecExpr[] u = unifyBV(e1, e2);
            return z3.mkBVSGE(u[0], u[1]);
        }
        if (e1 instanceof RealExpr || e2 instanceof RealExpr)
            return z3.mkGe(toReal(e1), toReal(e2));
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
        if (eval instanceof BitVecExpr bv)
            // Two's-complement negation: wraps like Java unary minus (e.g. -Integer.MIN_VALUE == Integer.MIN_VALUE).
            return z3.mkBVNeg(bv);
        return z3.mkUnaryMinus((ArithExpr) eval);
    }

    // #####################Arithmetic Operations#####################
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Expr<?> makeAdd(Expr<?> eval, Expr<?> eval2) {
        if (eval instanceof FPExpr || eval2 instanceof FPExpr) {
            FPSort s = commonFPSort(eval, eval2);
            return z3.mkFPAdd(z3.mkFPRoundNearestTiesToEven(), toFP(eval, s), toFP(eval2, s));
        }
        if (bothBitVec(eval, eval2)) {
            BitVecExpr[] u = unifyBV(eval, eval2);
            return z3.mkBVAdd(u[0], u[1]); // wraps on overflow, exactly like Java +
        }
        if (eval instanceof RealExpr || eval2 instanceof RealExpr)
            return z3.mkAdd(toReal(eval), toReal(eval2));
        return z3.mkAdd((ArithExpr) eval, (ArithExpr) eval2);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Expr<?> makeSub(Expr<?> eval, Expr<?> eval2) {
        if (eval instanceof FPExpr || eval2 instanceof FPExpr) {
            FPSort s = commonFPSort(eval, eval2);
            return z3.mkFPSub(z3.mkFPRoundNearestTiesToEven(), toFP(eval, s), toFP(eval2, s));
        }
        if (bothBitVec(eval, eval2)) {
            BitVecExpr[] u = unifyBV(eval, eval2);
            return z3.mkBVSub(u[0], u[1]); // wraps on overflow, exactly like Java -
        }
        if (eval instanceof RealExpr || eval2 instanceof RealExpr)
            return z3.mkSub(toReal(eval), toReal(eval2));
        return z3.mkSub((ArithExpr) eval, (ArithExpr) eval2);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Expr<?> makeMul(Expr<?> eval, Expr<?> eval2) {
        if (eval instanceof FPExpr || eval2 instanceof FPExpr) {
            FPSort s = commonFPSort(eval, eval2);
            return z3.mkFPMul(z3.mkFPRoundNearestTiesToEven(), toFP(eval, s), toFP(eval2, s));
        }
        if (bothBitVec(eval, eval2)) {
            BitVecExpr[] u = unifyBV(eval, eval2);
            return z3.mkBVMul(u[0], u[1]); // low-order bits wrap, exactly like Java *
        }
        if (eval instanceof RealExpr || eval2 instanceof RealExpr)
            return z3.mkMul(toReal(eval), toReal(eval2));
        return z3.mkMul((ArithExpr) eval, (ArithExpr) eval2);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Expr<?> makeDiv(Expr<?> eval, Expr<?> eval2) {
        if (eval instanceof FPExpr || eval2 instanceof FPExpr) {
            FPSort s = commonFPSort(eval, eval2);
            return z3.mkFPDiv(z3.mkFPRoundNearestTiesToEven(), toFP(eval, s), toFP(eval2, s));
        }
        if (bothBitVec(eval, eval2)) {
            BitVecExpr[] u = unifyBV(eval, eval2);
            return z3.mkBVSDiv(u[0], u[1]); // signed division truncates toward zero, like Java /
        }
        if (eval instanceof RealExpr || eval2 instanceof RealExpr)
            return z3.mkDiv(toReal(eval), toReal(eval2));
        return z3.mkDiv((ArithExpr) eval, (ArithExpr) eval2);
    }

    public Expr<?> makeMod(Expr<?> eval, Expr<?> eval2) {
        if (eval instanceof FPExpr || eval2 instanceof FPExpr) {
            // Java `%` on floating point is fmod (truncated remainder, sign of dividend), NOT the
            // IEEE-754 remainder (mkFPRem, which rounds the quotient to nearest). Encode fmod as
            // r = a - b * trunc(a / b), where trunc rounds the quotient toward zero.
            FPSort s = commonFPSort(eval, eval2);
            FPExpr a = toFP(eval, s);
            FPExpr b = toFP(eval2, s);
            FPExpr q = z3.mkFPRoundToIntegral(z3.mkFPRoundTowardZero(), z3.mkFPDiv(z3.mkFPRoundTowardZero(), a, b));
            return z3.mkFPSub(z3.mkFPRoundNearestTiesToEven(), a, z3.mkFPMul(z3.mkFPRoundNearestTiesToEven(), b, q));
        }
        if (bothBitVec(eval, eval2)) {
            BitVecExpr[] u = unifyBV(eval, eval2);
            // Java `%` takes the sign of the dividend; mkBVSRem is the signed remainder (sign of dividend),
            // unlike mkBVSMod (sign of divisor) or Z3's Euclidean mkMod (always non-negative).
            return z3.mkBVSRem(u[0], u[1]);
        }
        if (eval instanceof RealExpr || eval2 instanceof RealExpr)
            return z3.mkMod(toInt(eval), toInt(eval2));
        return z3.mkMod((IntExpr) eval, (IntExpr) eval2);
    }

    private RealExpr toReal(Expr<?> e) {
        if (e instanceof RealExpr)
            return (RealExpr) e;
        if (e instanceof IntExpr)
            return z3.mkInt2Real((IntExpr) e);
        if (e instanceof BitVecExpr bv)
            return z3.mkInt2Real(z3.mkBV2Int(bv, true)); // signed BV -> int -> real
        throw new NotImplementedException();
    }

    private IntExpr toInt(Expr<?> e) {
        if (e instanceof IntExpr)
            return (IntExpr) e;
        if (e instanceof RealExpr)
            return z3.mkReal2Int((RealExpr) e);
        if (e instanceof BitVecExpr bv)
            return z3.mkBV2Int(bv, true); // interpret the BitVector as a signed integer
        throw new NotImplementedException();
    }

    /**
     * Width in bits of a BitVector expression's sort.
     */
    private int bvWidth(BitVecExpr e) {
        return ((BitVecSort) e.getSort()).getSize();
    }

    /**
     * Coerces a BitVector to {@code width} bits, mirroring Java integral conversions: widening sign-extends (Java
     * widens {@code int} to {@code long} preserving sign, JLS §5.1.2) and narrowing keeps the low-order bits
     * ({@code mkExtract} = Java narrowing primitive conversion, JLS §5.1.3). Same width is returned unchanged.
     */
    private BitVecExpr toBV(BitVecExpr e, int width) {
        int w = bvWidth(e);
        if (w == width)
            return e;
        if (w < width)
            return z3.mkSignExt(width - w, e);
        return z3.mkExtract(width - 1, 0, e);
    }

    /**
     * Brings two operands to a common BitVector width before a binary operation, mirroring Java binary numeric
     * promotion (JLS §5.6.2): if either operand is a {@code long} (64-bit) both become 64-bit, otherwise both stay
     * 32-bit. A non-BitVector operand (e.g. an {@code IntExpr} literal that surfaced from a constant fold) is first
     * converted to a BitVector. At least one operand is a {@link BitVecExpr} when this is called.
     */
    private BitVecExpr[] unifyBV(Expr<?> e1, Expr<?> e2) {
        BitVecExpr b1 = asBV(e1);
        BitVecExpr b2 = asBV(e2);
        int width = Math.max(bvWidth(b1), bvWidth(b2));
        return new BitVecExpr[] { toBV(b1, width), toBV(b2, width) };
    }

    /**
     * Interprets {@code e} as a BitVector. BitVectors are returned as-is; an integer (which can appear if a fold
     * produced a plain {@link IntExpr}) is reduced into a 32-bit BitVector via {@code mkInt2BV}.
     */
    private BitVecExpr asBV(Expr<?> e) {
        if (e instanceof BitVecExpr bv)
            return bv;
        if (e instanceof IntExpr ie)
            return z3.mkInt2BV(32, ie);
        throw new NotImplementedException("Cannot interpret " + e.getSort() + " as a BitVector");
    }

    /**
     * True when both operands can take the BitVector arithmetic/comparison path: at least one is a {@link BitVecExpr}
     * (an actual integral value) and neither is a non-integral sort. A non-integral operand (boolean, enum, string,
     * uninterpreted) makes this false so the caller falls through to the generic path, where Z3 raises a proper
     * sort-mismatch error instead of this method silently coercing an ill-typed comparison.
     */
    private boolean bothBitVec(Expr<?> e1, Expr<?> e2) {
        return (e1 instanceof BitVecExpr || e2 instanceof BitVecExpr) && isBVCompatible(e1) && isBVCompatible(e2);
    }

    private boolean isBVCompatible(Expr<?> e) {
        return e instanceof BitVecExpr || e instanceof IntExpr;
    }

    /**
     * Models a Java floating-point-to-integral narrowing cast, e.g. {@code (int) d}: the operand is rounded toward zero
     * to a whole number (JLS §5.1.3 truncation). Returns an {@link IntExpr} so the cast result compares as an integer.
     *
     * <p>
     * For a floating-point operand the value is first rounded toward zero to an integral FP, converted to a real, then
     * to an int (the real is whole, so {@code mkReal2Int}'s floor is exact). A real operand is truncated toward zero by
     * flooring its magnitude and restoring the sign (plain {@code mkReal2Int} would floor toward negative infinity,
     * which is wrong for negatives). An integer (already-integral BitVector) operand is returned unchanged. The result
     * is a 32-bit BitVector so it compares directly against the integral cast target.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Expr<?> makeTruncateToZero(Expr<?> e) {
        if (e instanceof BitVecExpr bv)
            return toBV(bv, 32);
        if (e instanceof FPExpr fp) {
            FPExpr integral = z3.mkFPRoundToIntegral(z3.mkFPRoundTowardZero(), fp);
            return z3.mkInt2BV(32, z3.mkReal2Int(z3.mkFPToReal(integral)));
        }
        if (e instanceof RealExpr r) {
            IntExpr floor = z3.mkReal2Int(r);
            // floor == trunc for non-negative values; for negatives trunc = floor + 1 (unless already integral).
            IntExpr trunc = (IntExpr) z3.mkITE(z3.mkGe(r, z3.mkReal(0)), floor,
                    z3.mkITE(z3.mkEq(z3.mkInt2Real(floor), r), floor, z3.mkAdd(floor, z3.mkInt(1))));
            return z3.mkInt2BV(32, trunc);
        }
        throw new NotImplementedException();
    }

    /**
     * Common floating-point sort for a binary operation, mirroring Java binary numeric promotion: if either operand is
     * a {@code double} (binary64) the result is binary64, otherwise binary32 (a lone {@code float} operand, possibly
     * mixed with an integral operand that Java would convert to {@code float}). At least one operand must be an
     * {@link FPExpr} for this to be called.
     */
    private FPSort commonFPSort(Expr<?> e1, Expr<?> e2) {
        if (isDoubleSort(e1) || isDoubleSort(e2))
            return z3.mkFPSort64();
        return z3.mkFPSort32();
    }

    private boolean isDoubleSort(Expr<?> e) {
        return e instanceof FPExpr && ((FPExpr) e).getSort().equals(z3.mkFPSort64());
    }

    /**
     * Coerces {@code e} to the floating-point sort {@code target}. Float (binary32) operands meeting a binary64 sort
     * are widened with {@link com.microsoft.z3.Context#mkFPToFP} so the cast mirrors Java's float-to-double promotion;
     * integral and real operands are converted into {@code target} directly.
     */
    private FPExpr toFP(Expr<?> e, FPSort target) {
        FPExpr f;
        if (e instanceof FPExpr fe) {
            f = fe.getSort().equals(target) ? fe : z3.mkFPToFP(z3.mkFPRoundNearestTiesToEven(), fe, target);
        } else if (e instanceof BitVecNum bvn) {
            // A literal integral value (e.g. `5` widened to float in `5 + 1.0f`): use its signed long value
            // directly so the rounded FP constant matches what Java's int/long-to-float promotion produces.
            f = z3.mkFP(bvn.getLong(), target);
        } else if (e instanceof BitVecExpr bv) {
            // A symbolic integral value mixed with floating point: reinterpret as a signed integer, then a real,
            // then round into the target FP sort (Java's integral-to-floating promotion, JLS §5.1.2).
            RealExpr re = z3.mkInt2Real(z3.mkBV2Int(bv, true));
            f = z3.mkFPToFP(z3.mkFPRoundNearestTiesToEven(), re, target);
        } else if (e instanceof IntNum)
            f = z3.mkFP(((IntNum) e).getInt(), target);
        else if (e instanceof IntExpr ee) {
            RealExpr re = z3.mkInt2Real(ee);
            f = z3.mkFPToFP(z3.mkFPRoundNearestTiesToEven(), re, target);
        } else if (e instanceof RealExpr) {
            f = z3.mkFPToFP(z3.mkFPRoundNearestTiesToEven(), (RealExpr) e, target);
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

    private String buildFunctionLabel(String functionName, Expr<?>[] params) {
        String simpleName = Utils.getSimpleName(functionName);
        String argsString = Arrays.stream(params).map(p -> exprToNameTranslation.getOrDefault(p, p.toString()))
                .collect(Collectors.joining(", "));
        return simpleName + "(" + argsString + ")";
    }

    @Override
    public void close() throws Exception {
        z3.close();
    }
}
