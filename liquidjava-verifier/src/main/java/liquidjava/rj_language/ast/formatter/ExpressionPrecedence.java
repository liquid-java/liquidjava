package liquidjava.rj_language.ast.formatter;

import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.GroupExpression;
import liquidjava.rj_language.ast.Ite;
import liquidjava.rj_language.ast.UnaryExpression;

public enum ExpressionPrecedence {
    TERNARY, IMPLICATION, OR, AND, COMPARISON, ADDITIVE, MULTIPLICATIVE, UNARY, ATOMIC;

    public boolean isLowerThan(ExpressionPrecedence other) {
        return ordinal() < other.ordinal();
    }

    public static ExpressionPrecedence of(Expression expression) {
        if (expression instanceof GroupExpression group)
            return of(group.getExpression());
        if (expression instanceof Ite)
            return TERNARY;
        if (expression instanceof UnaryExpression)
            return UNARY;
        if (expression instanceof BinaryExpression binary)
            return of(binary.getOperator());
        return ATOMIC;
    }

    public static ExpressionPrecedence of(String operator) {
        return switch (operator) {
        case "-->" -> IMPLICATION;
        case "||" -> OR;
        case "&&" -> AND;
        case "==", "!=", ">=", ">", "<=", "<" -> COMPARISON;
        case "+", "-" -> ADDITIVE;
        case "*", "/", "%" -> MULTIPLICATIVE;
        default -> ATOMIC;
        };
    }
}
