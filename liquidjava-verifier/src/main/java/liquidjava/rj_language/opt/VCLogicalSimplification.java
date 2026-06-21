package liquidjava.rj_language.opt;

import liquidjava.processor.SimplifiedVCImplication;
import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.GroupExpression;
import liquidjava.rj_language.ast.Ite;
import liquidjava.rj_language.ast.LiteralBoolean;
import liquidjava.rj_language.ast.UnaryExpression;

/**
 * Simplifies VCImplication chains by applying logical identities inside refinements
 */
public class VCLogicalSimplification {

    /**
     * Applies the first logical simplification available in a VC chain
     */
    public static VCImplication apply(VCImplication implication) {
        if (implication == null)
            return null;

        Expression expression = implication.getRefinement().getExpression();
        Expression simplified = simplify(expression);
        if (!expression.equals(simplified)) {
            VCImplication result = new SimplifiedVCImplication(implication, new Predicate(simplified), implication);
            result.setNext(implication.getNext() == null ? null : implication.getNext().clone());
            return result;
        }

        VCImplication next = apply(implication.getNext());
        if (implication.getNext() == null || implication.getNext().equals(next))
            return implication;

        VCImplication result = implication.copyWithRefinement(implication.getRefinement().clone());
        result.setNext(next);
        return result;
    }

    /**
     * Simplifies the first logical identity found inside an expression
     */
    private static Expression simplify(Expression expression) {
        if (expression instanceof BinaryExpression binary)
            return simplifyBinary(binary);
        if (expression instanceof UnaryExpression unary)
            return simplifyUnary(unary);
        if (expression instanceof Ite ite)
            return simplifyIte(ite);
        if (expression instanceof GroupExpression group)
            return simplifyGroup(group);
        return expression.clone();
    }

    /**
     * Simplifies a binary expression by visiting operands before the current node
     */
    private static Expression simplifyBinary(BinaryExpression binary) {
        Expression left = binary.getFirstOperand();
        Expression simplifiedLeft = simplify(left);
        if (!left.equals(simplifiedLeft))
            return new BinaryExpression(simplifiedLeft, binary.getOperator(), binary.getSecondOperand().clone());

        Expression right = binary.getSecondOperand();
        Expression simplifiedRight = simplify(right);
        if (!right.equals(simplifiedRight))
            return new BinaryExpression(left.clone(), binary.getOperator(), simplifiedRight);

        Expression simplifiedBinary = simplifyLocalBinary(left, right, binary.getOperator());
        if (simplifiedBinary != null)
            return simplifiedBinary;

        return new BinaryExpression(left.clone(), binary.getOperator(), right.clone());
    }

    /**
     * Simplifies a unary expression by visiting its operand before the current node
     */
    private static Expression simplifyUnary(UnaryExpression unary) {
        Expression operand = unary.getExpression();
        Expression simplifiedOperand = simplify(operand);
        if (!operand.equals(simplifiedOperand))
            return new UnaryExpression(unary.getOp(), simplifiedOperand);

        // !!x -> x
        if ("!".equals(unary.getOp()) && isNot(operand))
            return negatedExpression(operand).clone();

        return new UnaryExpression(unary.getOp(), operand.clone());
    }

    /**
     * Simplifies a ternary expression by visiting condition, then branch, and else branch
     */
    private static Expression simplifyIte(Ite ite) {
        Expression condition = ite.getCondition();
        Expression simplifiedCondition = simplify(condition);
        if (!condition.equals(simplifiedCondition))
            return new Ite(simplifiedCondition, ite.getThen().clone(), ite.getElse().clone());

        Expression thenExpression = ite.getThen();
        Expression simplifiedThen = simplify(thenExpression);
        if (!thenExpression.equals(simplifiedThen))
            return new Ite(condition.clone(), simplifiedThen, ite.getElse().clone());

        Expression elseExpression = ite.getElse();
        Expression simplifiedElse = simplify(elseExpression);
        if (!elseExpression.equals(simplifiedElse))
            return new Ite(condition.clone(), thenExpression.clone(), simplifiedElse);

        return new Ite(condition.clone(), thenExpression.clone(), elseExpression.clone());
    }

    /**
     * Simplifies an expression wrapped in parentheses while preserving the group node
     */
    private static Expression simplifyGroup(GroupExpression group) {
        Expression expression = group.getExpression();
        Expression simplified = simplify(expression);
        if (!expression.equals(simplified))
            return new GroupExpression(simplified);
        return group.clone();
    }

    /**
     * Dispatches a local binary logical identity by operator
     */
    private static Expression simplifyLocalBinary(Expression left, Expression right, String op) {
        return switch (op) {
        case "&&" -> simplifyConjunction(left, right);
        case "||" -> simplifyDisjunction(left, right);
        case "==" -> simplifyEquality(left, right);
        case "!=" -> simplifyInequality(left, right);
        case "-->" -> simplifyImplication(left, right);
        default -> null;
        };
    }

    /**
     * Applies conjunction identities involving boolean literals and same operands
     */
    private static Expression simplifyConjunction(Expression left, Expression right) {
        // x && true -> x
        if (isTrue(right))
            return left.clone();
        // true && x -> x
        if (isTrue(left))
            return right.clone();
        // x && false -> false
        if (isFalse(right))
            return right.clone();
        // false && x -> false
        if (isFalse(left))
            return left.clone();
        // p && p -> p
        if (left.equals(right))
            return left.clone();
        return null;
    }

    /**
     * Applies disjunction identities involving boolean literals and same operands
     */
    private static Expression simplifyDisjunction(Expression left, Expression right) {
        // x || true -> true
        if (isTrue(right))
            return right.clone();
        // true || x -> true
        if (isTrue(left))
            return left.clone();
        // x || false -> x
        if (isFalse(right))
            return left.clone();
        // false || x -> x
        if (isFalse(left))
            return right.clone();
        // p || p -> p
        if (left.equals(right))
            return left.clone();
        return null;
    }

    /**
     * Applies equality identity for same operands
     */
    private static Expression simplifyEquality(Expression left, Expression right) {
        // x == x -> true
        if (left.equals(right))
            return new LiteralBoolean(true);
        return null;
    }

    /**
     * Applies inequality identity for same operands
     */
    private static Expression simplifyInequality(Expression left, Expression right) {
        // x != x -> false
        if (left.equals(right))
            return new LiteralBoolean(false);
        return null;
    }

    /**
     * Applies implication identities involving boolean literals and same operands
     */
    private static Expression simplifyImplication(Expression left, Expression right) {
        // x --> true -> true
        if (isTrue(right))
            return right.clone();
        // false --> x -> true
        if (isFalse(left))
            return new LiteralBoolean(true);
        // true --> x -> x
        if (isTrue(left))
            return right.clone();
        // x --> x -> true
        if (left.equals(right))
            return new LiteralBoolean(true);
        return null;
    }

    /**
     * Checks whether an expression is true
     */
    private static boolean isTrue(Expression expression) {
        return expression instanceof LiteralBoolean literal && literal.isBooleanTrue();
    }

    /**
     * Checks whether an expression is false
     */
    private static boolean isFalse(Expression expression) {
        return expression instanceof LiteralBoolean literal && !literal.isBooleanTrue();
    }

    /**
     * Checks whether an expression is unary logical negation
     */
    private static boolean isNot(Expression expression) {
        return expression instanceof UnaryExpression unary && "!".equals(unary.getOp());
    }

    /**
     * Returns the operand of a unary logical negation expression
     */
    private static Expression negatedExpression(Expression expression) {
        return ((UnaryExpression) expression).getExpression();
    }
}
