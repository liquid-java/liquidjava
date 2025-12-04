package liquidjava.rj_language.visitors;

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

public interface ExpressionVisitor<T> {
    T visitAliasInvocation(AliasInvocation alias) throws LJError;

    T visitBinaryExpression(BinaryExpression exp) throws LJError;

    T visitFunctionInvocation(FunctionInvocation fun) throws LJError;

    T visitGroupExpression(GroupExpression exp) throws LJError;

    T visitIte(Ite ite) throws LJError;

    T visitLiteralInt(LiteralInt lit) throws LJError;

    T visitLiteralBoolean(LiteralBoolean lit) throws LJError;

    T visitLiteralReal(LiteralReal lit) throws LJError;

    T visitLiteralString(LiteralString lit) throws LJError;

    T visitUnaryExpression(UnaryExpression exp) throws LJError;

    T visitVar(Var var) throws LJError;
}