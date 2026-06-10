package liquidjava.rj_language.opt;

import liquidjava.processor.SimplifiedVCImplication;
import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Enum;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.GroupExpression;
import liquidjava.rj_language.ast.Ite;
import liquidjava.rj_language.ast.LiteralBoolean;
import liquidjava.rj_language.ast.LiteralInt;
import liquidjava.rj_language.ast.LiteralReal;
import liquidjava.rj_language.ast.UnaryExpression;

/**
 * Simplifies VCImplication chains by folding constant expressions and other foldable patterns inside refinements
 */
public class VCFolding {

    /**
     * A folded expression and whether the fold changed the original expression
     */
    private record Folding(Expression folded, boolean changed) {
    }

    /**
     * Applies folding to the first foldable predicate in a VC chain
     */
    public static VCImplication apply(VCImplication implication) {
        if (implication == null)
            return null;

        Folding folding = fold(implication.getRefinement().getExpression());
        if (folding.changed()) {
            VCImplication result = new SimplifiedVCImplication(implication, new Predicate(folding.folded()),
                    implication.getOrigin());
            result.setNext(implication.getNext() == null ? null : implication.getNext().clone());
            return result;
        }

        VCImplication next = apply(implication.getNext());
        if (implication.getNext() == null || implication.getNext().equals(next))
            return implication.clone();

        VCImplication result = implication.copyWithRefinement(implication.getRefinement().clone());
        result.setNext(next);
        return result;
    }

    /**
     * Folds an expression
     */
    private static Folding fold(Expression expression) {
        if (expression instanceof BinaryExpression binary)
            return foldBinary(binary);
        if (expression instanceof UnaryExpression unary)
            return foldUnary(unary);
        if (expression instanceof Ite ite)
            return foldIte(ite);
        if (expression instanceof GroupExpression group && group.getChildren().size() == 1) {
            Folding child = fold(group.getExpression());
            return new Folding(child.folded(), true);
        }
        return new Folding(expression.clone(), false);
    }

    /**
     * Folds a binary expression and its operands
     */
    private static Folding foldBinary(BinaryExpression binary) {
        Folding leftFolded = fold(binary.getFirstOperand());
        Folding rightFolded = fold(binary.getSecondOperand());

        Expression leftExpression = leftFolded.folded();
        Expression rightExpression = rightFolded.folded();
        Expression left = resolvedLiteral(leftExpression);
        Expression right = resolvedLiteral(rightExpression);
        boolean childChanged = leftFolded.changed() || rightFolded.changed() || left != leftExpression
                || right != rightExpression;
        String op = binary.getOperator();

        Expression foldedBinary = foldLiteralBinary(left, right, op);
        if (foldedBinary != null)
            return new Folding(foldedBinary, true);

        Expression foldedAdjacentInts = foldAdjacentInts(left, right, op);
        if (foldedAdjacentInts != null)
            return new Folding(foldedAdjacentInts, true);

        if (childChanged)
            return new Folding(new BinaryExpression(left, op, right), true);
        return new Folding(binary.clone(), false);
    }

    /**
     * Folds a unary expression and its operand
     */
    private static Folding foldUnary(UnaryExpression unary) {
        Folding operandFolded = fold(unary.getExpression());
        Expression operand = operandFolded.folded();
        String op = unary.getOp();

        if ("!".equals(op) && operand instanceof LiteralBoolean literal)
            return new Folding(new LiteralBoolean(!literal.isBooleanTrue()), true);

        if ("-".equals(op)) {
            if (operand instanceof LiteralInt literal)
                return new Folding(new LiteralInt(-literal.getValue()), true);
            if (operand instanceof LiteralReal literal)
                return new Folding(new LiteralReal(-literal.getValue()), true);
        }

        if (operandFolded.changed())
            return new Folding(new UnaryExpression(op, operand), true);
        return new Folding(unary.clone(), false);
    }

    /**
     * Folds a conditional expression and its branches
     */
    private static Folding foldIte(Ite ite) {
        Folding conditionFolded = fold(ite.getCondition());
        Folding thenFolded = fold(ite.getThen());
        Folding elseFolded = fold(ite.getElse());

        Expression condition = conditionFolded.folded();
        Expression thenExpression = thenFolded.folded();
        Expression elseExpression = elseFolded.folded();

        if (condition instanceof LiteralBoolean literal)
            return new Folding(literal.isBooleanTrue() ? thenExpression : elseExpression, true);

        if (thenExpression.equals(elseExpression))
            return new Folding(thenExpression, true);

        if (conditionFolded.changed() || thenFolded.changed() || elseFolded.changed())
            return new Folding(new Ite(condition, thenExpression, elseExpression), true);
        return new Folding(ite.clone(), false);
    }

    /**
     * Folds a binary expression whose operands are both literals
     */
    private static Expression foldLiteralBinary(Expression left, Expression right, String op) {
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
    private static Expression foldAdjacentInts(Expression left, Expression right, String op) {
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
        if (constant == 0)
            return base;
        if (constant > 0)
            return new BinaryExpression(base, "+", new LiteralInt(constant));
        return new BinaryExpression(base, "-", new LiteralInt(-constant));
    }

    /**
     * Folds integer operations
     */
    private static Expression foldInts(int left, int right, String op) {
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
    private static Expression foldReals(double left, double right, String op) {
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
    private static Expression foldBooleans(boolean left, boolean right, String op) {
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
     * Replaces a resolved enum constant with its literal value
     */
    private static Expression resolvedLiteral(Expression expression) {
        if (expression instanceof Enum en && en.getResolvedLiteral() != null)
            return en.getResolvedLiteral().clone();
        return expression;
    }

    /**
     * Checks whether two expressions mix integer and real literals
     */
    private static boolean isMixedNumeric(Expression left, Expression right) {
        return left instanceof LiteralInt && right instanceof LiteralReal
                || left instanceof LiteralReal && right instanceof LiteralInt;
    }

    /**
     * Reads a numeric literal as a double
     */
    private static double numericValue(Expression expression) {
        if (expression instanceof LiteralInt literal)
            return literal.getValue();
        return ((LiteralReal) expression).getValue();
    }
}
