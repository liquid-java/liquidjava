package liquidjava.rj_language.opt;

import java.util.Optional;

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
     * Applies folding to the first foldable predicate in a VC chain
     */
    public static VCImplication apply(VCImplication implication) {
        if (implication == null)
            return null;

        Optional<Expression> folded = fold(implication.getRefinement().getExpression());
        if (folded.isPresent()) {
            VCImplication result = new SimplifiedVCImplication(implication, new Predicate(folded.get()),
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
    private static Optional<Expression> fold(Expression expression) {
        if (expression instanceof BinaryExpression binary)
            return foldBinary(binary);
        if (expression instanceof UnaryExpression unary)
            return foldUnary(unary);
        if (expression instanceof Ite ite)
            return foldIte(ite);
        if (expression instanceof GroupExpression group && group.getChildren().size() == 1) {
            Optional<Expression> child = fold(group.getExpression());
            return Optional.of(child.orElseGet(() -> group.getExpression().clone()));
        }
        return Optional.empty();
    }

    /**
     * Folds a binary expression and its operands
     */
    private static Optional<Expression> foldBinary(BinaryExpression binary) {
        Optional<Expression> leftFolded = fold(binary.getFirstOperand());
        Optional<Expression> rightFolded = fold(binary.getSecondOperand());

        Expression leftExpression = leftFolded.orElseGet(() -> binary.getFirstOperand().clone());
        Expression rightExpression = rightFolded.orElseGet(() -> binary.getSecondOperand().clone());
        Expression left = resolvedLiteral(leftExpression);
        Expression right = resolvedLiteral(rightExpression);
        boolean childChanged = leftFolded.isPresent() || rightFolded.isPresent() || left != leftExpression
                || right != rightExpression;
        String op = binary.getOperator();

        Expression foldedBinary = foldLiteralBinary(left, right, op);
        if (foldedBinary != null)
            return Optional.of(foldedBinary);

        Optional<Expression> foldedAdjacentInts = foldAdjacentInts(left, right, op);
        if (foldedAdjacentInts.isPresent())
            return foldedAdjacentInts;

        if (childChanged)
            return Optional.of(new BinaryExpression(left, op, right));
        return Optional.empty();
    }

    /**
     * Folds a unary expression and its operand
     */
    private static Optional<Expression> foldUnary(UnaryExpression unary) {
        Optional<Expression> operandFolded = fold(unary.getExpression());
        Expression operand = operandFolded.orElseGet(() -> unary.getExpression().clone());
        String op = unary.getOp();

        if ("!".equals(op) && operand instanceof LiteralBoolean literal)
            return Optional.of(new LiteralBoolean(!literal.isBooleanTrue()));

        if ("-".equals(op)) {
            if (operand instanceof LiteralInt literal)
                return Optional.of(new LiteralInt(-literal.getValue()));
            if (operand instanceof LiteralReal literal)
                return Optional.of(new LiteralReal(-literal.getValue()));
        }

        if (operandFolded.isPresent())
            return Optional.of(new UnaryExpression(op, operand));
        return Optional.empty();
    }

    /**
     * Folds a conditional expression and its branches
     */
    private static Optional<Expression> foldIte(Ite ite) {
        Optional<Expression> conditionFolded = fold(ite.getCondition());
        Optional<Expression> thenFolded = fold(ite.getThen());
        Optional<Expression> elseFolded = fold(ite.getElse());

        Expression condition = conditionFolded.orElseGet(() -> ite.getCondition().clone());
        Expression thenExpression = thenFolded.orElseGet(() -> ite.getThen().clone());
        Expression elseExpression = elseFolded.orElseGet(() -> ite.getElse().clone());

        if (condition instanceof LiteralBoolean literal)
            return Optional.of(literal.isBooleanTrue() ? thenExpression : elseExpression);

        if (thenExpression.equals(elseExpression))
            return Optional.of(thenExpression);

        if (conditionFolded.isPresent() || thenFolded.isPresent() || elseFolded.isPresent())
            return Optional.of(new Ite(condition, thenExpression, elseExpression));
        return Optional.empty();
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
    private static Optional<Expression> foldAdjacentInts(Expression left, Expression right, String op) {
        if (!"+".equals(op) && !"-".equals(op))
            return Optional.empty();
        if (!(right instanceof LiteralInt rightLiteral))
            return Optional.empty();
        if (!(left instanceof BinaryExpression leftBinary))
            return Optional.empty();
        if (!"+".equals(leftBinary.getOperator()) && !"-".equals(leftBinary.getOperator()))
            return Optional.empty();
        if (!(leftBinary.getSecondOperand()instanceof LiteralInt leftLiteral))
            return Optional.empty();

        // treat subtraction as adding a negative constant and then add the two
        int signedLeft = "+".equals(leftBinary.getOperator()) ? leftLiteral.getValue() : -leftLiteral.getValue();
        int signedRight = "+".equals(op) ? rightLiteral.getValue() : -rightLiteral.getValue();
        int constant = signedLeft + signedRight;
        Expression base = leftBinary.getFirstOperand().clone();
        if (constant == 0)
            return Optional.of(base);
        if (constant > 0)
            return Optional.of(new BinaryExpression(base, "+", new LiteralInt(constant)));
        return Optional.of(new BinaryExpression(base, "-", new LiteralInt(-constant)));
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
