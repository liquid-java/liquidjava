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
import liquidjava.rj_language.ast.SimplifiedExpression;
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

    private String formatExpression(Expression expression, boolean shouldWrap) {
        expression = unwrapGroup(expression);
        if (shouldWrap)
            return "(" + formatExpression(expression) + ")";
        return formatExpression(expression);
    }

    private String formatOperand(Expression parent, Expression child, boolean rightOperand) {
        child = unwrapGroup(child);
        return formatExpression(child, needsParentheses(parent, child, rightOperand));
    }

    private String formatCondition(Expression child) {
        return formatExpression(child, unwrapGroup(child) instanceof Ite);
    }

    private String formatArguments(List<Expression> args) {
        return args.stream().map(expression -> formatExpression(expression, false)).collect(Collectors.joining(", "));
    }

    private Expression unwrapGroup(Expression expression) {
        while (expression instanceof GroupExpression || expression instanceof SimplifiedExpression) {
            if (expression instanceof GroupExpression group)
                expression = group.getExpression();
            else if (expression instanceof SimplifiedExpression node)
                expression = node.getSimplifiedExpression();
        }
        return expression;
    }

    private boolean needsParentheses(Expression parent, Expression child, boolean rightOperand) {
        ExpressionPrecedence parentPrecedence = ExpressionPrecedence.of(parent);
        ExpressionPrecedence childPrecedence = ExpressionPrecedence.of(child);
        if (childPrecedence.isLowerThan(parentPrecedence))
            return true;
        if (childPrecedence != parentPrecedence)
            return false;

        if (parent instanceof BinaryExpression parentBinary && child instanceof BinaryExpression childBinary)
            return needsBinaryParentheses(parentBinary, childBinary, rightOperand);

        if (parent instanceof Ite && child instanceof Ite)
            return true;

        if (parent instanceof UnaryExpression parentUnary && child instanceof UnaryExpression childUnary)
            return parentUnary.getOp().equals("-") && childUnary.getOp().equals("-");

        return false;
    }

    private boolean needsBinaryParentheses(BinaryExpression parent, BinaryExpression child, boolean rightOperand) {
        if (rightOperand) {
            if (isRightAssociative(parent.getOperator()))
                return true;
            return !isRightAssociative(parent.getOperator())
                    && (!isAssociative(parent.getOperator()) || !parent.getOperator().equals(child.getOperator()));
        }
        return isRightAssociative(parent.getOperator());
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
        return formatOperand(exp, exp.getFirstOperand(), false) + " " + exp.getOperator() + " "
                + formatOperand(exp, exp.getSecondOperand(), true);
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
                + formatOperand(ite, ite.getElse(), true);
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
    public String visitSimplifiedNode(SimplifiedExpression node) {
        return formatExpression(node.getSimplifiedExpression());
    }

    @Override
    public String visitUnaryExpression(UnaryExpression exp) {
        return exp.getOp() + formatOperand(exp, exp.getExpression(), true);
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
