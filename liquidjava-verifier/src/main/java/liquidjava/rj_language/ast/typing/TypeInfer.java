package liquidjava.rj_language.ast.typing;

import java.util.Optional;
import liquidjava.processor.context.Context;
import liquidjava.processor.context.GhostFunction;
import liquidjava.rj_language.ast.AliasInvocation;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.FunctionInvocation;
import liquidjava.rj_language.ast.GroupExpression;
import liquidjava.rj_language.ast.Ite;
import liquidjava.rj_language.ast.LiteralBoolean;
import liquidjava.rj_language.ast.LiteralInt;
import liquidjava.rj_language.ast.LiteralLong;
import liquidjava.rj_language.ast.LiteralNull;
import liquidjava.rj_language.ast.LiteralReal;
import liquidjava.rj_language.ast.LiteralString;
import liquidjava.rj_language.ast.UnaryExpression;
import liquidjava.rj_language.ast.Var;
import liquidjava.utils.Utils;
import org.apache.commons.lang3.NotImplementedException;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;

public class TypeInfer {

    public static boolean checkCompatibleType(String type, Expression e, Context ctx, Factory factory) {
        if (e instanceof LiteralNull)
            return !Utils.isPrimitiveType(type);
        Optional<CtTypeReference<?>> t1 = getType(ctx, factory, e);
        CtTypeReference<?> t2 = Utils.getType(type, factory);
        return t1.isPresent() && t1.get().equals(t2);
    }

    public static Optional<CtTypeReference<?>> getType(Context ctx, Factory factory, Expression e) {
        if (e instanceof LiteralString)
            return Optional.of(Utils.getType("String", factory));
        else if (e instanceof LiteralInt)
            return Optional.of(Utils.getType("int", factory));
        else if (e instanceof LiteralLong)
            return Optional.of(Utils.getType("long", factory));
        else if (e instanceof LiteralReal)
            return Optional.of(Utils.getType("double", factory));
        else if (e instanceof LiteralBoolean)
            return boolType(factory);
        else if (e instanceof LiteralNull)
            return Optional.of(Utils.getType("java.lang.Object", factory));
        else if (e instanceof Var)
            return varType(ctx, (Var) e);
        else if (e instanceof UnaryExpression)
            return unaryType(ctx, factory, (UnaryExpression) e);
        else if (e instanceof Ite)
            return boolType(factory);
        else if (e instanceof BinaryExpression)
            return binaryType(ctx, factory, (BinaryExpression) e);
        else if (e instanceof GroupExpression)
            return getType(ctx, factory, ((GroupExpression) e).getExpression());
        else if (e instanceof FunctionInvocation)
            return functionType(ctx, (FunctionInvocation) e);
        else if (e instanceof AliasInvocation)
            return boolType(factory);

        return Optional.empty();
    }

    private static Optional<CtTypeReference<?>> varType(Context ctx, Var v) {
        String name = v.getName();
        if (!ctx.hasVariable(name))
            return Optional.empty();
        return Optional.of(ctx.getVariableByName(name).getType());
    }

    private static Optional<CtTypeReference<?>> unaryType(Context ctx, Factory factory, UnaryExpression e) {
        if (e.getOp().equals("!"))
            return boolType(factory);
        return getType(ctx, factory, e.getExpression());
    }

    private static Optional<CtTypeReference<?>> binaryType(Context ctx, Factory factory, BinaryExpression e) {
        if (e.isLogicOperation())
            return boolType(factory); // &&, ||, -->
        else if (e.isBooleanOperation()) { // >, >=, <, <=, ==, !=
            return boolType(factory);

        } else if (e.isArithmeticOperation()) {
            Optional<CtTypeReference<?>> t1 = getType(ctx, factory, e.getFirstOperand());
            Optional<CtTypeReference<?>> t2 = getType(ctx, factory, e.getSecondOperand());
            if (t1.isEmpty() || t2.isEmpty())
                return Optional.empty();
            if (t1.get().equals(t2.get()))
                return t1;
            // TODO if the types are different ex: double, int
            throw new NotImplementedException(
                    "To implement in TypeInfer: Binary type, arithmetic with different arg types");
        }
        return Optional.empty();
    }

    private static Optional<CtTypeReference<?>> functionType(Context ctx, FunctionInvocation e) {
        Optional<GhostFunction> gh = ctx.getGhosts().stream().filter(g -> g.matches(e.getName())).findAny();
        return gh.map(GhostFunction::getReturnType);
    }

    private static Optional<CtTypeReference<?>> boolType(Factory factory) {
        return Optional.of(Utils.getType("boolean", factory));
    }
}
