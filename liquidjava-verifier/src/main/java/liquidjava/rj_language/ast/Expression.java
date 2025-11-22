package liquidjava.rj_language.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import liquidjava.diagnostics.errors.ArgumentMismatchError;
import liquidjava.diagnostics.errors.CustomError;
import liquidjava.diagnostics.errors.LJError;
import liquidjava.diagnostics.errors.NotFoundError;
import liquidjava.processor.context.Context;
import liquidjava.processor.facade.AliasDTO;
import liquidjava.rj_language.ast.typing.TypeInfer;
import liquidjava.rj_language.visitors.ExpressionVisitor;
import liquidjava.utils.Utils;
import liquidjava.utils.constants.Keys;
import spoon.reflect.factory.Factory;

public abstract class Expression {

    public abstract <T> T accept(ExpressionVisitor<T> visitor) throws Exception;

    public abstract void getVariableNames(List<String> toAdd);

    public abstract void getStateInvocations(List<String> toAdd, List<String> all);

    public abstract boolean isBooleanTrue();

    public abstract int hashCode();

    public abstract boolean equals(Object obj);

    public abstract Expression clone();

    public abstract String toString();

    /**
     * Returns a simplified string representation of this expression with unqualified names (e.g.,
     * com.example.State.open => open Default implementation delegates to toString() Subclasses that contain qualified
     * names should override this method
     * 
     * @return simplified string representation
     */
    public String toSimplifiedString() {
        return toString();
    }

    List<Expression> children = new ArrayList<>();

    public void addChild(Expression e) {
        children.add(e);
    }

    public List<Expression> getChildren() {
        return children;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public void setChild(int index, Expression element) {
        children.set(index, element);
    }

    public boolean isLiteral() {
        return this instanceof LiteralInt || this instanceof LiteralReal || this instanceof LiteralBoolean;
    }

    /**
     * Checks if this expression produces a boolean type based on its structure
     * 
     * @return true if it is a boolean expression, false otherwise
     */
    public boolean isBooleanExpression() {
        if (this instanceof LiteralBoolean || this instanceof Ite || this instanceof AliasInvocation
                || this instanceof FunctionInvocation) {
            return true;
        }
        if (this instanceof GroupExpression ge) {
            return ge.getExpression().isBooleanExpression();
        }
        if (this instanceof BinaryExpression be) {
            return be.isBooleanOperation() || be.isLogicOperation();
        }
        if (this instanceof UnaryExpression ue) {
            return ue.getOp().equals("!");
        }
        return false;
    }

    /**
     * Substitutes the expression first given expression by the second
     *
     * @param from
     * @param to
     *
     * @return
     */
    public Expression substitute(Expression from, Expression to) {
        Expression e = clone();
        if (this.equals(from))
            e = to;
        e.auxSubstitute(from, to);
        return e;
    }

    private void auxSubstitute(Expression from, Expression to) {
        if (hasChildren()) {
            for (int i = 0; i < children.size(); i++) {
                Expression exp = children.get(i);
                if (exp.equals(from))
                    setChild(i, to);
                exp.auxSubstitute(from, to);
            }
        }
    }

    /**
     * Substitutes the function call with the given parameter to the expression e
     *
     * @param functionName
     * @param parameters
     * @param sub
     */
    public void substituteFunction(String functionName, List<Expression> parameters, Expression sub) {
        if (hasChildren())
            for (int i = 0; i < children.size(); i++) {
                Expression exp = children.get(i);
                if (exp instanceof FunctionInvocation fi) {
                    if (fi.name.equals(functionName) && fi.argumentsEqual(parameters)) {
                        // substitute by sub in parent
                        setChild(i, sub);
                    }
                }
                exp.substituteFunction(functionName, parameters, sub);
            }
    }

    public Expression substituteState(Map<String, Expression> subMap, String[] toChange) {
        Expression e = clone();
        if (this instanceof FunctionInvocation fi) {
            String key = fi.name;
            String simple = Utils.getSimpleName(key);
            boolean has = subMap.containsKey(key) || subMap.containsKey(simple);
            if (has && fi.children.size() == 1 && fi.children.get(0)instanceof Var v) { // object
                // state
                Expression sub = (subMap.containsKey(key) ? subMap.get(key) : subMap.get(simple)).clone();
                for (String s : toChange) {
                    sub = sub.substitute(new Var(s), v);
                }
                // substitute by sub in parent
                e = new GroupExpression(sub);
            }
        }
        e.auxSubstituteState(subMap, toChange);
        return e;
    }

    private void auxSubstituteState(Map<String, Expression> subMap, String[] toChange) {
        if (hasChildren()) {
            for (int i = 0; i < children.size(); i++) {
                Expression exp = children.get(i);
                if (exp instanceof FunctionInvocation fi) {
                    String key = fi.name;
                    String simple = Utils.getSimpleName(key);
                    boolean has = subMap.containsKey(key) || subMap.containsKey(simple);
                    if (has && fi.children.size() == 1 && fi.children.get(0)instanceof Var v) { // object
                        // state
                        Expression sub = (subMap.containsKey(key) ? subMap.get(key) : subMap.get(simple)).clone();
                        for (String s : toChange) {
                            sub = sub.substitute(new Var(s), v);
                        }
                        // substitute by sub in parent
                        setChild(i, (sub instanceof GroupExpression) ? sub : new GroupExpression(sub));
                    }
                }
                exp.auxSubstituteState(subMap, toChange);
            }
        }
    }

    public Expression changeAlias(Map<String, AliasDTO> alias, Context ctx, Factory f) throws LJError {
        Expression e = clone();
        if (this instanceof AliasInvocation ai) {
            if (alias.containsKey(ai.name)) { // object state
                AliasDTO dto = alias.get(ai.name);
                // check argument count
                if (children.size() != dto.getVarNames().size()) {
                    String msg = String.format(
                            "Wrong number of arguments in alias invocation '%s': expected %d, got %d", ai.name,
                            dto.getVarNames().size(), children.size());
                    throw new ArgumentMismatchError(msg);
                }
                Expression sub = dto.getExpression().clone();
                for (int i = 0; i < children.size(); i++) {
                    Expression varExp = new Var(dto.getVarNames().get(i));
                    String varType = dto.getVarTypes().get(i);
                    Expression aliasExp = children.get(i);

                    // check argument types
                    boolean compatible = TypeInfer.checkCompatibleType(varType, aliasExp, ctx, f);
                    if (!compatible) {
                        String msg = String.format(
                                "Argument '%s' and parameter '%s' of alias '%s' types are incompatible: expected %s, got %s",
                                aliasExp, dto.getVarNames().get(i), ai.name, varType,
                                TypeInfer.getType(ctx, f, aliasExp).get().getQualifiedName());
                        throw new ArgumentMismatchError(msg);
                    }
                    sub = sub.substitute(varExp, aliasExp);
                }
                e = new GroupExpression(sub);
            }
        }
        e.auxChangeAlias(alias, ctx, f);
        return e;
    }

    private void auxChangeAlias(Map<String, AliasDTO> alias, Context ctx, Factory f) throws LJError {
        if (hasChildren())
            for (int i = 0; i < children.size(); i++) {
                if (children.get(i)instanceof AliasInvocation ai) {
                    if (!alias.containsKey(ai.name))
                        throw new NotFoundError("Alias '" + ai.getName() + "' not found", Keys.ALIAS, ai.getName());
                    AliasDTO dto = alias.get(ai.name);
                    // check argument count
                    if (ai.children.size() != dto.getVarNames().size()) {
                        String msg = String.format(
                                "Wrong number of arguments in alias invocation '%s': expected %d, got %d", ai.name,
                                dto.getVarNames().size(), ai.children.size());
                        throw new ArgumentMismatchError(msg);
                    }
                    Expression sub = dto.getExpression().clone();
                    if (ai.hasChildren())
                        for (int j = 0; j < ai.children.size(); j++) {
                            Expression varExp = new Var(dto.getVarNames().get(j));
                            String varType = dto.getVarTypes().get(j);
                            Expression aliasExp = ai.children.get(j);

                            // check argument types
                            boolean compatible = TypeInfer.checkCompatibleType(varType, aliasExp, ctx, f);
                            if (!compatible) {
                                String msg = String.format(
                                        "Argument '%s' and parameter '%s' of alias '%s' types are incompatible: expected %s, got %s",
                                        aliasExp, dto.getVarNames().get(i), ai.name, varType,
                                        TypeInfer.getType(ctx, f, aliasExp).get().getQualifiedName());
                                throw new ArgumentMismatchError(msg);
                            }
                            sub = sub.substitute(varExp, aliasExp);
                        }
                    setChild(i, sub);
                }
                children.get(i).auxChangeAlias(alias, ctx, f);
            }
    }
}
