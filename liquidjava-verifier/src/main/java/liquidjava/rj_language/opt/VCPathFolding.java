package liquidjava.rj_language.opt;

import static liquidjava.rj_language.opt.VCSimplificationUtils.factsOf;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Enum;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.Ite;
import liquidjava.rj_language.ast.LiteralBoolean;
import liquidjava.rj_language.ast.LiteralChar;
import liquidjava.rj_language.ast.LiteralInt;
import liquidjava.rj_language.ast.LiteralLong;
import liquidjava.rj_language.ast.LiteralReal;
import liquidjava.rj_language.ast.UnaryExpression;
import liquidjava.rj_language.ast.Var;
import liquidjava.utils.constants.Formats;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.reference.CtTypeReference;

/**
 * Folds the conditions of the conditional facts of a VC chain under the facts {@code x == c}, with {@code c} a literal
 * or a constant of an enum the chain binds (written {@code Mode.ON}), that the chain states: each condition is written
 * with {@code c} for {@code x} and folded, and a branch is dropped only when its condition folds to a constant that
 * rules it out. Every fact of the chain holds wherever the others do, so the chain stays equivalent, and the facts
 * {@code x == c} themselves stay as they are
 */
public class VCPathFolding implements VCSimplificationPass {

    private static final VCSimplificationPass FOLDING = new VCFolding();
    private static final VCSimplificationPass LOGIC = new VCLogicalSimplification();

    private final Set<String> enumConstants;

    /**
     * @param bound
     *            a chain that binds the enum constants that may occur, such as the chain before its unused binders were
     *            dropped
     */
    public VCPathFolding(VCImplication bound) {
        this.enumConstants = enumConstants(bound);
    }

    @Override
    public VCImplication apply(VCImplication implication) {
        Map<Var, Expression> constants = constants(implication, enumConstants);
        if (constants.isEmpty())
            return implication;
        VCImplication result = implication.clone();
        for (VCImplication node = result; node != null; node = node.getNext())
            if (node.getRefinement() != null)
                node.setRefinement(
                        new Predicate(foldConditions(node.getRefinement().getExpression(), constants, enumConstants)));
        return result;
    }

    /** The enum constants the chain binds, each a distinct value of its enum */
    private static Set<String> enumConstants(VCImplication implication) {
        Set<String> constants = new HashSet<>();
        for (VCImplication node = implication; node != null; node = node.getNext())
            if (node.hasBinder() && isEnumConstant(node.getName(), node.getType()))
                constants.add(node.getName());
        return constants;
    }

    private static boolean isEnumConstant(String name, CtTypeReference<?> type) {
        return type != null && type.getDeclaration()instanceof CtEnum<?> declaration
                && declaration.getEnumValues().stream().anyMatch(value -> name
                        .equals(String.format(Formats.ENUM, declaration.getSimpleName(), value.getSimpleName())));
    }

    /** The variables the chain states equal to a constant, with that constant */
    private static Map<Var, Expression> constants(VCImplication implication, Set<String> enumConstants) {
        Map<Var, Expression> constants = new LinkedHashMap<>();
        for (VCImplication node = implication; node != null; node = node.getNext()) {
            if (node.getRefinement() == null)
                continue;
            for (Expression fact : factsOf(node.getRefinement().getExpression()))
                if (fact instanceof BinaryExpression equality && "==".equals(equality.getOperator())) {
                    addConstant(equality.getFirstOperand(), equality.getSecondOperand(), constants, enumConstants);
                    addConstant(equality.getSecondOperand(), equality.getFirstOperand(), constants, enumConstants);
                }
        }
        return constants;
    }

    private static void addConstant(Expression variable, Expression value, Map<Var, Expression> constants,
            Set<String> enumConstants) {
        if (variable instanceof Var var && !enumConstants.contains(var.getName()) && isConstant(value, enumConstants))
            constants.putIfAbsent(var, value);
    }

    private static boolean isConstant(Expression expression, Set<String> enumConstants) {
        return expression instanceof Var var && enumConstants.contains(var.getName()) || expression instanceof Enum
                || expression instanceof LiteralInt || expression instanceof LiteralLong
                || expression instanceof LiteralReal || expression instanceof LiteralBoolean
                || expression instanceof LiteralChar;
    }

    private static Expression foldConditions(Expression expression, Map<Var, Expression> constants,
            Set<String> enumConstants) {
        if (expression instanceof Ite ite) {
            Expression condition = fold(compareEnumConstants(substitute(ite.getCondition(), constants), enumConstants));
            if (condition instanceof LiteralBoolean settled)
                return foldConditions(settled.isBooleanTrue() ? ite.getThen() : ite.getElse(), constants,
                        enumConstants);
            return new Ite(condition, foldConditions(ite.getThen(), constants, enumConstants),
                    foldConditions(ite.getElse(), constants, enumConstants));
        }
        if (expression instanceof BinaryExpression binary)
            return new BinaryExpression(foldConditions(binary.getFirstOperand(), constants, enumConstants),
                    binary.getOperator(), foldConditions(binary.getSecondOperand(), constants, enumConstants));
        if (expression instanceof UnaryExpression unary)
            return new UnaryExpression(unary.getOp(), foldConditions(unary.getExpression(), constants, enumConstants));
        return expression.clone();
    }

    /** {@code A == B} and {@code A != B} of two enum constants, as the literal they are */
    private static Expression compareEnumConstants(Expression expression, Set<String> enumConstants) {
        if (expression instanceof BinaryExpression binary && binary.getFirstOperand()instanceof Var left
                && binary.getSecondOperand()instanceof Var right && enumConstants.contains(left.getName())
                && enumConstants.contains(right.getName())
                && ("==".equals(binary.getOperator()) || "!=".equals(binary.getOperator())))
            return new LiteralBoolean(left.getName().equals(right.getName()) == "==".equals(binary.getOperator()));
        for (int i = 0; i < expression.getChildren().size(); i++)
            expression.setChild(i, compareEnumConstants(expression.getChildren().get(i), enumConstants));
        return expression;
    }

    private static Expression substitute(Expression expression, Map<Var, Expression> constants) {
        Expression substituted = expression.clone();
        for (Map.Entry<Var, Expression> constant : constants.entrySet())
            substituted = substituted.substitute(constant.getKey(), constant.getValue().clone());
        return substituted;
    }

    /** The condition folded with the constant and logical identities, until neither applies */
    private static Expression fold(Expression condition) {
        VCImplication current = new VCImplication(new Predicate(condition));
        while (true) {
            VCImplication next = LOGIC.apply(FOLDING.apply(current));
            if (next.equals(current))
                return current.getRefinement().getExpression();
            current = next;
        }
    }

    @Override
    public String getName() {
        return "Path Folding";
    }
}
