package liquidjava.rj_language.opt;

import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Enum;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.Ite;
import liquidjava.rj_language.ast.LiteralBoolean;
import liquidjava.rj_language.ast.LiteralInt;
import liquidjava.rj_language.ast.LiteralReal;
import liquidjava.rj_language.ast.UnaryExpression;

/**
 * Simplifies VCImplication chains by folding constant expressions and other foldable patterns inside refinements
 */
public class VCFolding extends VCExpressionSimplificationPass<Void> {

    @Override
    protected Expression simplify(Expression expression, Void context) {
        return fold(expression);
    }

    /**
     * Folds the first foldable expression found
     */
    private Expression fold(Expression expression) {
        // enum constant -> literal
        if (expression instanceof Enum en && en.getResolvedLiteral() != null)
            return en.getResolvedLiteral().clone();
        if (expression instanceof BinaryExpression binary)
            return foldBinary(binary);
        if (expression instanceof UnaryExpression unary)
            return foldUnary(unary);
        if (expression instanceof Ite ite)
            return foldIte(ite);
        return expression.clone();
    }

    /**
     * Folds a binary expression and its operands
     */
    private Expression foldBinary(BinaryExpression binary) {
        Expression left = binary.getFirstOperand();
        Expression foldedLeft = fold(left);
        if (!left.equals(foldedLeft))
            return new BinaryExpression(foldedLeft, binary.getOperator(), binary.getSecondOperand().clone());

        Expression right = binary.getSecondOperand();
        Expression foldedRight = fold(right);
        if (!right.equals(foldedRight))
            return new BinaryExpression(left.clone(), binary.getOperator(), foldedRight);

        String op = binary.getOperator();

        Expression foldedBinary = foldLiteralBinary(left, right, op);
        if (foldedBinary != null)
            return foldedBinary;

        Expression foldedAdjacentInts = foldAdjacentInts(left, right, op);
        if (foldedAdjacentInts != null)
            return foldedAdjacentInts;

        return new BinaryExpression(left, op, right);
    }

    /**
     * Folds a unary expression and its operand
     */
    private Expression foldUnary(UnaryExpression unary) {
        Expression operand = unary.getExpression();
        Expression foldedOperand = fold(operand);
        if (!operand.equals(foldedOperand))
            return new UnaryExpression(unary.getOp(), foldedOperand);

        String op = unary.getOp();

        // !true -> false
        // !false -> true
        if ("!".equals(op) && operand instanceof LiteralBoolean literal)
            return new LiteralBoolean(!literal.isBooleanTrue());

        if ("-".equals(op)) {
            // -(x) -> -x
            if (operand instanceof LiteralInt literal)
                return new LiteralInt(-literal.getValue());
            if (operand instanceof LiteralReal literal)
                return new LiteralReal(-literal.getValue());
        }

        return new UnaryExpression(op, operand);
    }

    /**
     * Folds a conditional expression and its branches
     */
    private Expression foldIte(Ite ite) {
        Expression condition = ite.getCondition();
        Expression foldedCondition = fold(condition);
        if (!condition.equals(foldedCondition))
            return new Ite(foldedCondition, ite.getThen().clone(), ite.getElse().clone());

        Expression thenExpression = ite.getThen();
        Expression foldedThen = fold(thenExpression);
        if (!thenExpression.equals(foldedThen))
            return new Ite(condition.clone(), foldedThen, ite.getElse().clone());

        Expression elseExpression = ite.getElse();
        Expression foldedElse = fold(elseExpression);
        if (!elseExpression.equals(foldedElse))
            return new Ite(condition.clone(), thenExpression.clone(), foldedElse);

        // true ? x : y -> x
        // false ? x : y -> y
        if (condition instanceof LiteralBoolean literal)
            return literal.isBooleanTrue() ? thenExpression : elseExpression;

        // y ? x : x -> x
        if (thenExpression.equals(elseExpression))
            return thenExpression;

        return new Ite(condition, thenExpression, elseExpression);
    }

    /**
     * Folds a binary expression whose operands are both literals
     */
    private Expression foldLiteralBinary(Expression left, Expression right, String op) {
        if (left instanceof LiteralInt leftInt && right instanceof LiteralInt rightInt)
            return foldInts(leftInt.getValue(), rightInt.getValue(), op);

        if (left instanceof LiteralReal leftReal && right instanceof LiteralReal rightReal)
            return foldReals(leftReal.getValue(), rightReal.getValue(), op);

        if (isMixedNumeric(left, right)) {
            double l = numericValue(left);
            double r = numericValue(right);
            return foldReals(l, r, op);
        }

        if (left instanceof LiteralBoolean leftBool && right instanceof LiteralBoolean rightBool)
            return foldBooleans(leftBool.isBooleanTrue(), rightBool.isBooleanTrue(), op);

        if (left instanceof Enum leftEnum && right instanceof Enum rightEnum
                && leftEnum.getTypeName().equals(rightEnum.getTypeName())) {
            boolean equal = leftEnum.getConstName().equals(rightEnum.getConstName());
            // Enum.A == Enum.A -> true
            // Enum.A != Enum.B -> true
            return switch (op) {
            case "==" -> new LiteralBoolean(equal);
            case "!=" -> new LiteralBoolean(!equal);
            default -> null;
            };
        }

        return null;
    }

    /**
     * Combines adjacent integer constants in additions and subtractions
     */
    private Expression foldAdjacentInts(Expression left, Expression right, String op) {
        if (!"+".equals(op) && !"-".equals(op))
            return null;
        if (!(right instanceof LiteralInt rightLiteral))
            return null;
        if (!(left instanceof BinaryExpression leftBinary))
            return null;
        if (!"+".equals(leftBinary.getOperator()) && !"-".equals(leftBinary.getOperator()))
            return null;
        if (!(leftBinary.getSecondOperand()instanceof LiteralInt leftLiteral))
            return null;

        // treat subtraction as adding a negative constant and then add the two
        int signedLeft = "+".equals(leftBinary.getOperator()) ? leftLiteral.getValue() : -leftLiteral.getValue();
        int signedRight = "+".equals(op) ? rightLiteral.getValue() : -rightLiteral.getValue();
        int constant = signedLeft + signedRight;
        Expression base = leftBinary.getFirstOperand().clone();
        // x + n - n -> x
        if (constant == 0)
            return base;
        // x + n + m -> x + k
        if (constant > 0)
            return new BinaryExpression(base, "+", new LiteralInt(constant));
        // x + n - m -> x - k
        return new BinaryExpression(base, "-", new LiteralInt(-constant));
    }

    /**
     * Folds integer operations
     */
    private Expression foldInts(int left, int right, String op) {
        return switch (op) {
        case "+" -> new LiteralInt(left + right);
        case "-" -> new LiteralInt(left - right);
        case "*" -> new LiteralInt(left * right);
        case "/" -> right != 0 ? new LiteralInt(left / right) : null;
        case "%" -> right != 0 ? new LiteralInt(left % right) : null;
        case "<" -> new LiteralBoolean(left < right);
        case "<=" -> new LiteralBoolean(left <= right);
        case ">" -> new LiteralBoolean(left > right);
        case ">=" -> new LiteralBoolean(left >= right);
        case "==" -> new LiteralBoolean(left == right);
        case "!=" -> new LiteralBoolean(left != right);
        default -> null;
        };
    }

    /**
     * Folds real number operations
     */
    private Expression foldReals(double left, double right, String op) {
        return switch (op) {
        case "+" -> new LiteralReal(left + right);
        case "-" -> new LiteralReal(left - right);
        case "*" -> new LiteralReal(left * right);
        case "/" -> right != 0.0 ? new LiteralReal(left / right) : null;
        case "%" -> right != 0.0 ? new LiteralReal(left % right) : null;
        case "<" -> new LiteralBoolean(left < right);
        case "<=" -> new LiteralBoolean(left <= right);
        case ">" -> new LiteralBoolean(left > right);
        case ">=" -> new LiteralBoolean(left >= right);
        case "==" -> new LiteralBoolean(left == right);
        case "!=" -> new LiteralBoolean(left != right);
        default -> null;
        };
    }

    /**
     * Folds boolean operations
     */
    private Expression foldBooleans(boolean left, boolean right, String op) {
        return switch (op) {
        case "&&" -> new LiteralBoolean(left && right);
        case "||" -> new LiteralBoolean(left || right);
        case "-->" -> new LiteralBoolean(!left || right);
        case "==" -> new LiteralBoolean(left == right);
        case "!=" -> new LiteralBoolean(left != right);
        default -> null;
        };
    }

    /**
     * Checks whether two expressions mix integer and real literals
     */
    private boolean isMixedNumeric(Expression left, Expression right) {
        return left instanceof LiteralInt && right instanceof LiteralReal
                || left instanceof LiteralReal && right instanceof LiteralInt;
    }

    /**
     * Reads a numeric literal as a double
     */
    private double numericValue(Expression expression) {
        if (expression instanceof LiteralInt literal)
            return literal.getValue();
        return ((LiteralReal) expression).getValue();
    }
}
