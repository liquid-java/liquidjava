package liquidjava.rj_language;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import liquidjava.diagnostics.errors.ArgumentMismatchError;
import liquidjava.diagnostics.errors.LJError;
import liquidjava.diagnostics.errors.SyntaxError;
import liquidjava.processor.context.AliasWrapper;
import liquidjava.processor.context.Context;
import liquidjava.processor.context.GhostFunction;
import liquidjava.processor.context.GhostState;
import liquidjava.processor.facade.AliasDTO;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.FunctionInvocation;
import liquidjava.rj_language.ast.GroupExpression;
import liquidjava.rj_language.ast.Ite;
import liquidjava.rj_language.ast.LiteralBoolean;
import liquidjava.rj_language.ast.LiteralInt;
import liquidjava.rj_language.ast.LiteralReal;
import liquidjava.rj_language.ast.UnaryExpression;
import liquidjava.rj_language.ast.Var;
import liquidjava.rj_language.opt.derivation_node.ValDerivationNode;
import liquidjava.rj_language.opt.ExpressionSimplifier;
import liquidjava.rj_language.parsing.RefinementsParser;
import liquidjava.utils.Utils;
import liquidjava.utils.constants.Keys;
import liquidjava.utils.constants.Ops;
import liquidjava.utils.constants.Types;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;

/**
 * Acts as a wrapper for Expression AST
 *
 * @author cgamboa
 */
public class Predicate {

    protected Expression exp;
    protected String prefix;

    /** Create a predicate with the expression true */
    public Predicate() {
        exp = new LiteralBoolean(true);
    }

    /**
     * Create a new predicate with a refinement
     *
     * @param ref
     * @param element
     */
    public Predicate(String ref, CtElement element) throws LJError {
        this(ref, element, element.getParent(CtType.class).getQualifiedName());
    }

    /**
     * Create a new predicate with a refinement and a given prefix for the class
     * 
     * @param ref
     * @param element
     * @param prefix
     */
    public Predicate(String ref, CtElement element, String prefix) throws LJError {
        this.prefix = prefix;
        exp = parse(ref, element);
        if (!(exp instanceof GroupExpression)) {
            exp = new GroupExpression(exp);
        }
    }

    /** Create a predicate with the expression true */
    public Predicate(Expression e) {
        exp = e;
    }

    protected Expression parse(String ref, CtElement element) throws LJError {
        try {
            return RefinementsParser.createAST(ref, prefix);
        } catch (LJError e) {
            // add location info to error
            SourcePosition pos = Utils.getRefinementAnnotationPosition(element, ref);
            e.setPosition(pos);
            throw e;
        }
    }

    protected Expression innerParse(String ref, String prefix) throws LJError {
        return RefinementsParser.createAST(ref, prefix);
    }

    public Predicate changeAliasToRefinement(Context context, Factory f) throws LJError {
        Expression ref = getExpression();

        Map<String, AliasDTO> alias = new HashMap<>();
        for (AliasWrapper aw : context.getAlias()) {
            alias.put(aw.getName(), aw.createAliasDTO());
        }

        ref = ref.changeAlias(alias, context, f);
        ref.validateGhostInvocations(context, f);

        return new Predicate(ref);
    }

    public Predicate negate() {
        return new Predicate(new UnaryExpression("!", exp));
    }

    public Predicate substituteVariable(String from, String to) {
        Expression ec = exp.clone();
        ec = ec.substitute(new Var(from), new Var(to));
        return new Predicate(ec);
    }

    public List<String> getVariableNames() {
        List<String> l = new ArrayList<>();
        exp.getVariableNames(l);
        return l;
    }

    public List<GhostState> getStateInvocations(List<GhostState> lgs) {
        if (lgs == null)
            return new ArrayList<>();
        List<String> all = lgs.stream().map(GhostFunction::getQualifiedName).collect(Collectors.toList());
        List<String> toAdd = new ArrayList<>();
        exp.getStateInvocations(toAdd, all);

        List<GhostState> gh = new ArrayList<>();
        for (String n : toAdd) {
            for (GhostState g : lgs)
                if (g.matches(n))
                    gh.add(g);
        }

        return gh;
    }

    /** Change old mentions of previous name to the new name e.g., old(previousName) -> newName */
    public Predicate changeOldMentions(String previousName, String newName) {
        Expression e = exp.clone();
        Expression prev = createVar(previousName).getExpression();
        List<Expression> le = new ArrayList<>();
        le.add(createVar(newName).getExpression());
        e.substituteFunction(Keys.OLD, le, prev);
        return new Predicate(e);
    }

    public Predicate changeStatesToRefinements(List<GhostState> ghostState, String[] toChange) throws LJError {
        Map<String, Expression> nameRefinementMap = new HashMap<>();
        for (GhostState gs : ghostState) {
            if (gs.getRefinement() != null) { // is a state and not a ghost state
                String name = gs.getQualifiedName();
                Expression exp = innerParse(gs.getRefinement().toString(), gs.getPrefix());
                nameRefinementMap.put(name, exp);
                // Also allow simple name lookup to enable hierarchy matching
                String simple = Utils.getSimpleName(name);
                nameRefinementMap.putIfAbsent(simple, exp);
            }
        }
        Expression e = exp.substituteState(nameRefinementMap, toChange);
        return new Predicate(e);
    }

    public boolean isBooleanTrue() {
        return exp.isBooleanTrue();
    }

    @Override
    public String toString() {
        return exp.toString();
    }

    @Override
    public Predicate clone() {
        return new Predicate(exp.clone());
    }

    public Expression getExpression() {
        return exp;
    }

    public ValDerivationNode simplify() {
        return ExpressionSimplifier.simplify(exp.clone());
    }

    private static boolean isBooleanLiteral(Expression expr, boolean value) {
        return expr instanceof LiteralBoolean && expr.isBooleanTrue() == value;
    }

    public static Predicate createConjunction(Predicate c1, Predicate c2) {
        // simplification: (true && x) = x, (false && x) = false
        if (isBooleanLiteral(c1.getExpression(), true))
            return c2;
        if (isBooleanLiteral(c2.getExpression(), true))
            return c1;
        if (isBooleanLiteral(c1.getExpression(), false))
            return c1;
        if (isBooleanLiteral(c2.getExpression(), false))
            return c2;
        return new Predicate(new BinaryExpression(c1.getExpression(), Ops.AND, c2.getExpression()));
    }

    public static Predicate createDisjunction(Predicate c1, Predicate c2) {
        // simplification: (false || x) = x, (true || x) = true
        if (isBooleanLiteral(c1.getExpression(), false))
            return c2;
        if (isBooleanLiteral(c2.getExpression(), false))
            return c1;
        if (isBooleanLiteral(c1.getExpression(), true))
            return c1;
        if (isBooleanLiteral(c2.getExpression(), true))
            return c2;
        return new Predicate(new BinaryExpression(c1.getExpression(), Ops.OR, c2.getExpression()));
    }

    public static Predicate createEquals(Predicate c1, Predicate c2) {
        return new Predicate(new BinaryExpression(c1.getExpression(), Ops.EQ, c2.getExpression()));
    }

    public static Predicate createITE(Predicate c1, Predicate c2, Predicate c3) {
        return new Predicate(new Ite(c1.getExpression(), c2.getExpression(), c3.getExpression()));
    }

    public static Predicate createLit(String value, String type) {
        Expression exp = switch (type) {
        case Types.BOOLEAN -> new LiteralBoolean(value);
        case Types.INT, Types.SHORT -> new LiteralInt(value);
        case Types.DOUBLE, Types.LONG, Types.FLOAT -> new LiteralReal(value);
        default -> throw new IllegalArgumentException("Unsupported literal type: " + type);
        };
        return new Predicate(exp);
    }

    public static Predicate createOperation(Predicate c1, String op, Predicate c2) {
        return new Predicate(new BinaryExpression(c1.getExpression(), op, c2.getExpression()));
    }

    public static Predicate createVar(String name) {
        return new Predicate(new Var(name));
    }

    public static Predicate createInvocation(String name, Predicate... Predicates) {
        List<Expression> le = new ArrayList<>();
        for (Predicate c : Predicates)
            le.add(c.getExpression());
        return new Predicate(new FunctionInvocation(name, le));
    }
}
