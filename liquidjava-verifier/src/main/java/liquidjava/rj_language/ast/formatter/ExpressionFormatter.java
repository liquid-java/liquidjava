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
import liquidjava.rj_language.ast.Enum;
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
        Expression expression = unwrapGroup(child);
        if (shouldWrap)
            return "(" + formatExpression(expression) + ")";
        return formatExpression(expression);
    }

    private String formatLeftOperand(Expression parent, Expression child) {
        return formatParentheses(child, needsLeftParentheses(parent, child));
    }

    private String formatRightOperand(BinaryExpression parent, Expression child) {
        return formatParentheses(child, needsRightParentheses(parent, child));
    }

    private String formatCondition(Expression child) {
        return formatParentheses(child, unwrapGroup(child) instanceof Ite);
    }

    private String formatArguments(List<Expression> args) {
        return args.stream().map(expression -> formatParentheses(expression, false)).collect(Collectors.joining(", "));
    }

    private Expression unwrapGroup(Expression expression) {
        while (expression instanceof GroupExpression group)
            expression = group.getExpression();
        return expression;
    }

    private boolean needsParentheses(Expression parent, Expression child) {
        return ExpressionPrecedence.of(child).isLowerThan(ExpressionPrecedence.of(parent));
    }

    private boolean needsLeftParentheses(Expression parent, Expression child) {
        if (needsParentheses(parent, child))
            return true;

        Expression unwrappedChild = unwrapGroup(child);
        if (ExpressionPrecedence.of(unwrappedChild) != ExpressionPrecedence.of(parent))
            return false;

        return parent instanceof BinaryExpression binary && isRightAssociative(binary.getOperator())
                && unwrappedChild instanceof BinaryExpression;
    }

    private boolean needsRightParentheses(BinaryExpression parent, Expression child) {
        if (needsParentheses(parent, child))
            return true;

        Expression unwrappedChild = unwrapGroup(child);
        if (ExpressionPrecedence.of(unwrappedChild) != ExpressionPrecedence.of(parent))
            return false;

        if (unwrappedChild instanceof BinaryExpression right)
            return !isRightAssociative(parent.getOperator())
                    && (!isAssociative(parent.getOperator()) || !parent.getOperator().equals(right.getOperator()));

        return false;
    }

    private boolean isAssociative(String operator) {
        return operator.equals("&&") || operator.equals("||") || operator.equals("+") || operator.equals("*");
    }

    private boolean isRightAssociative(String operator) {
        return operator.equals("-->");
    }

    @Override
    public String visitAliasInvocation(AliasInvocation alias) {
        return alias.getName() + "(" + formatArguments(alias.getArgs()) + ")";
    }

    @Override
    public String visitBinaryExpression(BinaryExpression exp) {
        return formatLeftOperand(exp, exp.getFirstOperand()) + " " + exp.getOperator() + " "
                + formatRightOperand(exp, exp.getSecondOperand());
    }

    @Override
    public String visitFunctionInvocation(FunctionInvocation fun) {
        return Utils.getSimpleName(fun.getName()) + "(" + formatArguments(fun.getArgs()) + ")";
    }

    @Override
    public String visitGroupExpression(GroupExpression exp) {
        return formatExpression(exp.getExpression());
    }

    @Override
    public String visitIte(Ite ite) {
        return formatCondition(ite.getCondition()) + " ? " + formatCondition(ite.getThen()) + " : "
                + formatLeftOperand(ite, ite.getElse());
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
        Expression child = unwrapGroup(exp.getExpression());
        boolean nestedMinus = child instanceof UnaryExpression unary && exp.getOp().equals("-")
                && unary.getOp().equals("-");
        return exp.getOp() + formatParentheses(child, needsParentheses(exp, child) || nestedMinus);
    }

    @Override
    public String visitEnum(Enum en) {
        return en.toString();
    }

    @Override
    public String visitVar(Var var) {
        return VariableFormatter.format(var.getName());
    }
}
