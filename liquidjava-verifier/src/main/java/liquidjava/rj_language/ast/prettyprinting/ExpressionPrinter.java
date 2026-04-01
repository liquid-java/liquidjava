package liquidjava.rj_language.ast.prettyprinting;

import java.util.List;
import java.util.stream.Collectors;

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
 * Pretty printer for expressions that preserves only the parentheses required by precedence and associativity rules
 * Also formats variable names using {@link VariableFormatter}
 */
public class ExpressionPrinter implements ExpressionVisitor<String> {

    public static String print(Expression expression) {
        return new ExpressionPrinter().render(expression);
    }

    private String render(Expression expression) {
        return expression.accept(this);
    }

    private String renderWithOptionalParentheses(Expression child, boolean shouldWrap) {
        if (shouldWrap)
            return "(" + render(child) + ")";
        if (child instanceof GroupExpression group)
            return "(" + render(group.getExpression()) + ")";
        return render(child);
    }

    private String renderOperand(Expression parent, Expression child) {
        return renderWithOptionalParentheses(child, needsParentheses(parent, child));
    }

    private String renderRightOperand(BinaryExpression parent, Expression child) {
        return renderWithOptionalParentheses(child, needsRightParentheses(parent, child));
    }

    private String renderConditionOperand(Expression child) {
        return renderWithOptionalParentheses(child, child instanceof Ite);
    }

    private String renderArguments(List<Expression> args) {
        return args.stream().map(expression -> renderWithOptionalParentheses(expression, false))
                .collect(Collectors.joining(", "));
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
        return alias.getName() + "(" + renderArguments(alias.getArgs()) + ")";
    }

    @Override
    public String visitBinaryExpression(BinaryExpression exp) {
        return renderOperand(exp, exp.getFirstOperand()) + " " + exp.getOperator() + " "
                + renderRightOperand(exp, exp.getSecondOperand());
    }

    @Override
    public String visitFunctionInvocation(FunctionInvocation fun) {
        return Utils.getSimpleName(fun.getName()) + "(" + renderArguments(fun.getArgs()) + ")";
    }

    @Override
    public String visitGroupExpression(GroupExpression exp) {
        return "(" + render(exp.getExpression()) + ")";
    }

    @Override
    public String visitIte(Ite ite) {
        return renderConditionOperand(ite.getCondition()) + " ? " + renderConditionOperand(ite.getThen()) + " : "
                + renderOperand(ite, ite.getElse());
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
        return exp.getOp() + renderOperand(exp, exp.getExpression());
    }

    @Override
    public String visitVar(Var var) {
        return VariableFormatter.format(var.getName());
    }
}
