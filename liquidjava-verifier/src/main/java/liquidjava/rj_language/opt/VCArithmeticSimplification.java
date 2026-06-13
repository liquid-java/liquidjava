package liquidjava.rj_language.opt;

import java.util.ArrayList;
import java.util.List;

import liquidjava.processor.SimplifiedVCImplication;
import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.GroupExpression;
import liquidjava.rj_language.ast.Ite;
import liquidjava.rj_language.ast.LiteralInt;
import liquidjava.rj_language.ast.LiteralReal;
import liquidjava.rj_language.ast.UnaryExpression;

/**
 * Simplifies VCImplication chains by applying arithmetic identities inside refinements
 */
public class VCArithmeticSimplification {

    /**
     * Applies the first arithmetic simplification available in a VC chain
     */
    public static VCImplication apply(VCImplication implication) {
        if (implication == null)
            return null;

        return apply(implication, List.of());
    }

    private static VCImplication apply(VCImplication implication, List<Expression> nonZeroExpressions) {
        if (implication == null)
            return null;

        Expression expression = implication.getRefinement().getExpression();
        Expression simplified = simplify(expression, nonZeroExpressions);
        if (!expression.equals(simplified)) {
            VCImplication result = new SimplifiedVCImplication(implication, new Predicate(simplified),
                    implication.getOrigin());
            result.setNext(implication.getNext() == null ? null : implication.getNext().clone());
            return result;
        }

        List<Expression> nextNoneZeroExpressions = new ArrayList<>(nonZeroExpressions);
        addNonZeroExpression(implication.getRefinement().getExpression(), nextNoneZeroExpressions);

        VCImplication next = apply(implication.getNext(), nextNoneZeroExpressions);
        if (implication.getNext() == null || implication.getNext().equals(next))
            return implication;

        VCImplication result = implication.copyWithRefinement(implication.getRefinement().clone());
        result.setNext(next);
        return result;
    }

    /**
     * Simplifies the first arithmetic identity found inside an expression
     */
    private static Expression simplify(Expression expression, List<Expression> nonZeroExpressions) {
        if (expression instanceof BinaryExpression binary)
            return simplifyBinary(binary, nonZeroExpressions);
        if (expression instanceof UnaryExpression unary)
            return simplifyUnary(unary, nonZeroExpressions);
        if (expression instanceof Ite ite)
            return simplifyIte(ite, nonZeroExpressions);
        if (expression instanceof GroupExpression group)
            return simplifyGroup(group, nonZeroExpressions);
        return expression.clone();
    }

    /**
     * Simplifies a binary expression by visiting operands before the current node
     */
    private static Expression simplifyBinary(BinaryExpression binary, List<Expression> nonZeroExpressions) {
        Expression left = binary.getFirstOperand();
        Expression simplifiedLeft = simplify(left, nonZeroExpressions);
        if (!left.equals(simplifiedLeft))
            return new BinaryExpression(simplifiedLeft, binary.getOperator(), binary.getSecondOperand().clone());

        Expression right = binary.getSecondOperand();
        Expression simplifiedRight = simplify(right, nonZeroExpressions);
        if (!right.equals(simplifiedRight))
            return new BinaryExpression(left.clone(), binary.getOperator(), simplifiedRight);

        Expression simplifiedBinary = simplifyLocalBinary(left, right, binary.getOperator(), nonZeroExpressions);
        if (simplifiedBinary != null)
            return simplifiedBinary;

        return new BinaryExpression(left.clone(), binary.getOperator(), right.clone());
    }

    /**
     * Simplifies a unary expression by visiting its operand before the current node
     */
    private static Expression simplifyUnary(UnaryExpression unary, List<Expression> nonZeroExpressions) {
        Expression operand = unary.getExpression();
        Expression simplifiedOperand = simplify(operand, nonZeroExpressions);
        if (!operand.equals(simplifiedOperand))
            return new UnaryExpression(unary.getOp(), simplifiedOperand);

        // -(-x) -> x
        if ("-".equals(unary.getOp()) && isNegation(operand))
            return negatedExpression(operand).clone();

        return new UnaryExpression(unary.getOp(), operand.clone());
    }

    /**
     * Simplifies a ternary expression by visiting condition, then branch, and else branch
     */
    private static Expression simplifyIte(Ite ite, List<Expression> nonZeroExpressions) {
        Expression condition = ite.getCondition();
        Expression simplifiedCondition = simplify(condition, nonZeroExpressions);
        if (!condition.equals(simplifiedCondition))
            return new Ite(simplifiedCondition, ite.getThen().clone(), ite.getElse().clone());

        Expression thenExpression = ite.getThen();
        Expression simplifiedThen = simplify(thenExpression, nonZeroExpressions);
        if (!thenExpression.equals(simplifiedThen))
            return new Ite(condition.clone(), simplifiedThen, ite.getElse().clone());

        Expression elseExpression = ite.getElse();
        Expression simplifiedElse = simplify(elseExpression, nonZeroExpressions);
        if (!elseExpression.equals(simplifiedElse))
            return new Ite(condition.clone(), thenExpression.clone(), simplifiedElse);

        return new Ite(condition.clone(), thenExpression.clone(), elseExpression.clone());
    }

    /**
     * Simplifies an expression wrapped in parentheses while preserving the group node
     */
    private static Expression simplifyGroup(GroupExpression group, List<Expression> nonZeroExpressions) {
        Expression expression = group.getExpression();
        Expression simplified = simplify(expression, nonZeroExpressions);
        if (!expression.equals(simplified))
            return new GroupExpression(simplified);
        return group.clone();
    }

    /**
     * Dispatches a local binary arithmetic identity by operator
     */
    private static Expression simplifyLocalBinary(Expression left, Expression right, String op,
            List<Expression> nonZeroExpressions) {
        return switch (op) {
        case "+" -> simplifyAddition(left, right);
        case "-" -> simplifySubtraction(left, right);
        case "*" -> simplifyMultiplication(left, right);
        case "/" -> simplifyDivision(left, right, nonZeroExpressions);
        case "%" -> simplifyModulo(left, right, nonZeroExpressions);
        default -> null;
        };
    }

    /**
     * Applies addition identities involving zero and unary negation
     */
    private static Expression simplifyAddition(Expression left, Expression right) {
        // x + 0 -> x
        if (isZero(right))
            return left.clone();
        // 0 + x -> x
        if (isZero(left))
            return right.clone();
        // x + (-x) -> 0
        if (isNegation(right) && left.equals(negatedExpression(right)))
            return new LiteralInt(0);
        // (-x) + x -> 0
        if (isNegation(left) && negatedExpression(left).equals(right))
            return new LiteralInt(0);
        // x + (-y) -> x - y
        if (isNegation(right))
            return new BinaryExpression(left.clone(), "-", negatedExpression(right).clone());
        return null;
    }

    /**
     * Applies subtraction identities involving zero, same operands, and unary negation
     */
    private static Expression simplifySubtraction(Expression left, Expression right) {
        // x - 0 -> x
        if (isZero(right))
            return left.clone();
        // 0 - x -> -x
        if (isZero(left))
            return new UnaryExpression("-", right.clone());
        // x - x -> 0
        if (left.equals(right))
            return new LiteralInt(0);
        // x - (-y) -> x + y
        if (isNegation(right))
            return new BinaryExpression(left.clone(), "+", negatedExpression(right).clone());
        return null;
    }

    /**
     * Applies multiplication identities involving one and zero
     */
    private static Expression simplifyMultiplication(Expression left, Expression right) {
        // x * 1 -> x
        if (isOne(right))
            return left.clone();
        // 1 * x -> x
        if (isOne(left))
            return right.clone();
        // x * 0 -> 0
        if (isZero(right))
            return right.clone();
        // 0 * x -> 0
        if (isZero(left))
            return left.clone();
        return null;
    }

    /**
     * Applies division identities, using prior non-zero premises when needed
     */
    private static Expression simplifyDivision(Expression left, Expression right, List<Expression> nonZeroExpressions) {
        // x / 1 -> x
        if (isOne(right))
            return left.clone();
        // 0 / x -> 0 (x != 0)
        if (isZero(left) && isNonZero(right, nonZeroExpressions))
            return left.clone();
        // x / x -> 1 (x != 0)
        if (left.equals(right) && isNonZero(right, nonZeroExpressions))
            return new LiteralInt(1);
        return null;
    }

    /**
     * Applies modulo identities, using prior non-zero premises when needed
     */
    private static Expression simplifyModulo(Expression left, Expression right, List<Expression> nonZeroExpressions) {
        // x % 1 -> 0
        if (isOne(right))
            return new LiteralInt(0);
        // x % x -> 0 (x != 0)
        if (left.equals(right) && isNonZero(right, nonZeroExpressions))
            return new LiteralInt(0);
        return null;
    }

    /**
     * Records direct non-zero premises shaped as x != 0 or 0 != x
     */
    private static void addNonZeroExpression(Expression expression, List<Expression> nonZeroExpressions) {
        if (!(expression instanceof BinaryExpression binary) || !"!=".equals(binary.getOperator()))
            return;

        Expression left = binary.getFirstOperand();
        Expression right = binary.getSecondOperand();
        if (isZero(left))
            nonZeroExpressions.add(right.clone());
        if (isZero(right))
            nonZeroExpressions.add(left.clone());
    }

    /**
     * Checks whether a previous premise recorded an expression as non-zero
     */
    private static boolean isNonZero(Expression expression, List<Expression> nonZeroExpressions) {
        return nonZeroExpressions.stream().anyMatch(e -> e.equals(expression));
    }

    /**
     * Checks whether an expression is a numeric zero literal
     */
    private static boolean isZero(Expression expression) {
        if (expression instanceof LiteralInt literal)
            return literal.getValue() == 0;
        if (expression instanceof LiteralReal literal)
            return literal.getValue() == 0.0;
        return false;
    }

    /**
     * Checks whether an expression is a numeric one literal
     */
    private static boolean isOne(Expression expression) {
        if (expression instanceof LiteralInt literal)
            return literal.getValue() == 1;
        if (expression instanceof LiteralReal literal)
            return literal.getValue() == 1.0;
        return false;
    }

    /**
     * Checks whether an expression is unary negation
     */
    private static boolean isNegation(Expression expression) {
        return expression instanceof UnaryExpression unary && "-".equals(unary.getOp());
    }

    /**
     * Returns the operand of a unary negation expression
     */
    private static Expression negatedExpression(Expression expression) {
        return ((UnaryExpression) expression).getExpression();
    }
}
