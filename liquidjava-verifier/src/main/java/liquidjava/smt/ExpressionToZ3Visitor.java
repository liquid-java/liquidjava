package liquidjava.smt;

import com.microsoft.z3.Expr;

import liquidjava.diagnostics.errors.LJError;
import liquidjava.rj_language.ast.AliasInvocation;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.FunctionInvocation;
import liquidjava.rj_language.ast.GroupExpression;
import liquidjava.rj_language.ast.Ite;
import liquidjava.rj_language.ast.LiteralBoolean;
import liquidjava.rj_language.ast.LiteralInt;
import liquidjava.rj_language.ast.LiteralReal;
import liquidjava.rj_language.ast.LiteralString;
import liquidjava.rj_language.ast.UnaryExpression;
import liquidjava.rj_language.ast.Var;
import liquidjava.rj_language.visitors.ExpressionVisitor;

public class ExpressionToZ3Visitor implements ExpressionVisitor<Expr<?>> {

    private final TranslatorToZ3 ctx;

    public ExpressionToZ3Visitor(TranslatorToZ3 ctx) {
        this.ctx = ctx;
    }

    @Override
    public Expr<?> visitAliasInvocation(AliasInvocation alias) throws LJError {
        Expr<?>[] argsExpr = new Expr[alias.getArgs().size()];
        for (int i = 0; i < argsExpr.length; i++) {
            argsExpr[i] = alias.getArgs().get(i).accept(this);
        }
        return ctx.makeFunctionInvocation(alias.getName(), argsExpr);
    }

    @Override
    public Expr<?> visitBinaryExpression(BinaryExpression exp) throws LJError {
        Expr<?> e1 = exp.getFirstOperand().accept(this);
        Expr<?> e2 = exp.getSecondOperand().accept(this);
        return switch (exp.getOperator()) {
        case "&&" -> ctx.makeAnd(e1, e2);
        case "||" -> ctx.makeOr(e1, e2);
        case "-->" -> ctx.makeImplies(e1, e2);
        case "==" -> ctx.makeEquals(e1, e2);
        case "!=" -> ctx.mkNot(ctx.makeEquals(e1, e2));
        case ">=" -> ctx.makeGtEq(e1, e2);
        case ">" -> ctx.makeGt(e1, e2);
        case "<=" -> ctx.makeLtEq(e1, e2);
        case "<" -> ctx.makeLt(e1, e2);
        case "+" -> ctx.makeAdd(e1, e2);
        case "-" -> ctx.makeSub(e1, e2);
        case "*" -> ctx.makeMul(e1, e2);
        case "/" -> ctx.makeDiv(e1, e2);
        case "%" -> ctx.makeMod(e1, e2);
        default -> throw new RuntimeException("Operation " + exp.getOperator() + " not supported by z3");
        };
    }

    @Override
    public Expr<?> visitFunctionInvocation(FunctionInvocation fun) throws LJError {
        Expr<?>[] argsExpr = new Expr[fun.getArgs().size()];
        for (int i = 0; i < argsExpr.length; i++) {
            argsExpr[i] = fun.getArgs().get(i).accept(this);
        }
        return ctx.makeFunctionInvocation(fun.getName(), argsExpr);
    }

    @Override
    public Expr<?> visitGroupExpression(GroupExpression exp) throws LJError {
        return exp.getExpression().accept(this);
    }

    @Override
    public Expr<?> visitIte(Ite ite) throws LJError {
        return ctx.makeIte(ite.getCondition().accept(this), ite.getThen().accept(this), ite.getElse().accept(this));
    }

    @Override
    public Expr<?> visitVar(Var var) throws LJError {
        return ctx.makeVariable(var.getName());
    }

    @Override
    public Expr<?> visitLiteralInt(LiteralInt lit) {
        return ctx.makeIntegerLiteral(lit.getValue());
    }

    @Override
    public Expr<?> visitLiteralBoolean(LiteralBoolean lit) {
        return ctx.makeBooleanLiteral(lit.isBooleanTrue());
    }

    @Override
    public Expr<?> visitLiteralReal(LiteralReal lit) {
        return ctx.makeDoubleLiteral(lit.getValue());
    }

    @Override
    public Expr<?> visitLiteralString(LiteralString lit) {
        return ctx.makeString(lit.toString());
    }

    @Override
    public Expr<?> visitUnaryExpression(UnaryExpression exp) throws LJError {
        return switch (exp.getOp()) {
        case "-" -> ctx.makeMinus(exp.getExpression().accept(this));
        case "!" -> ctx.mkNot(exp.getExpression().accept(this));
        default -> null;
        };
    }
}
