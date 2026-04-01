package liquidjava.rj_language.ast.formatter;

import java.util.List;
import java.util.stream.Collectors;

import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.AliasInvocation;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.FunctionInvocation;
import liquidjava.rj_language.ast.GroupExpression;
import liquidjava.rj_language.ast.Ite;
import liquidjava.rj_language.ast.LiteralBoolean;
import liquidjava.rj_language.ast.LiteralChar;
import liquidjava.rj_language.ast.LiteralInt;
import liquidjava.rj_language.ast.LiteralLong;
import liquidjava.rj_language.ast.LiteralReal;
import liquidjava.rj_language.ast.LiteralString;
import liquidjava.rj_language.ast.UnaryExpression;
import liquidjava.rj_language.ast.Var;
import liquidjava.rj_language.visitors.ExpressionVisitor;
import liquidjava.utils.Utils;

/**
 * Formatter for expressions that only adds parentheses when required by precedence and associativity rules and formats
 * variable names using {@link VariableFormatter}
 */
public class ExpressionFormatter implements ExpressionVisitor<String> {

    public static String format(Predicate predicate) {
        return format(predicate.getExpression());
    }

    public static String format(Expression expression) {
        return new ExpressionFormatter().formatExpression(expression);
    }

    private String formatExpression(Expression expression) {
        return expression.accept(this);
    }

    private String formatParentheses(Expression child, boolean shouldWrap) {
        if (shouldWrap)
            return "(" + formatExpression(child) + ")";
        if (child instanceof GroupExpression group)
            return "(" + formatExpression(group.getExpression()) + ")";
        return formatExpression(child);
    }

    private String formatOperand(Expression parent, Expression child) {
        return formatParentheses(child, needsParentheses(parent, child));
    }

    private String formatRightOperand(BinaryExpression parent, Expression child) {
        return formatParentheses(child, needsRightParentheses(parent, child));
    }

    private String formatCondition(Expression child) {
        return formatParentheses(child, child instanceof Ite);
    }

    private String formatArguments(List<Expression> args) {
        return args.stream().map(expression -> formatParentheses(expression, false)).collect(Collectors.joining(", "));
    }

    private boolean needsParentheses(Expression parent, Expression child) {
        return ExpressionPrecedence.of(child).isLowerThan(ExpressionPrecedence.of(parent));
    }

    private boolean needsRightParentheses(BinaryExpression parent, Expression child) {
        if (needsParentheses(parent, child))
            return true;

        if (ExpressionPrecedence.of(child) != ExpressionPrecedence.of(parent))
            return false;

        if (child instanceof BinaryExpression right)
            return !isAssociative(parent.getOperator()) || !parent.getOperator().equals(right.getOperator());

        return false;
    }

    private boolean isAssociative(String operator) {
        return operator.equals("&&") || operator.equals("||") || operator.equals("+") || operator.equals("*");
    }

    @Override
    public String visitAliasInvocation(AliasInvocation alias) {
        return alias.getName() + "(" + formatArguments(alias.getArgs()) + ")";
    }

    @Override
    public String visitBinaryExpression(BinaryExpression exp) {
        return formatOperand(exp, exp.getFirstOperand()) + " " + exp.getOperator() + " "
                + formatRightOperand(exp, exp.getSecondOperand());
    }

    @Override
    public String visitFunctionInvocation(FunctionInvocation fun) {
        return Utils.getSimpleName(fun.getName()) + "(" + formatArguments(fun.getArgs()) + ")";
    }

    @Override
    public String visitGroupExpression(GroupExpression exp) {
        return "(" + formatExpression(exp.getExpression()) + ")";
    }

    @Override
    public String visitIte(Ite ite) {
        return formatCondition(ite.getCondition()) + " ? " + formatCondition(ite.getThen()) + " : "
                + formatOperand(ite, ite.getElse());
    }

    @Override
    public String visitLiteralInt(LiteralInt lit) {
        return Integer.toString(lit.getValue());
    }

    @Override
    public String visitLiteralLong(LiteralLong lit) {
        return Long.toString(lit.getValue());
    }

    @Override
    public String visitLiteralBoolean(LiteralBoolean lit) {
        return lit.toString();
    }

    @Override
    public String visitLiteralChar(LiteralChar lit) {
        return lit.toString();
    }

    @Override
    public String visitLiteralReal(LiteralReal lit) {
        return lit.toString();
    }

    @Override
    public String visitLiteralString(LiteralString lit) {
        return lit.toString();
    }

    @Override
    public String visitUnaryExpression(UnaryExpression exp) {
        return exp.getOp() + formatOperand(exp, exp.getExpression());
    }

    @Override
    public String visitVar(Var var) {
        return VariableFormatter.format(var.getName());
    }
}
