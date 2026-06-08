package liquidjava.rj_language.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import liquidjava.diagnostics.errors.LJError;
import liquidjava.rj_language.visitors.ExpressionVisitor;
import spoon.reflect.reference.CtTypeReference;

public class SimplifiedExpression extends Expression {

    private final Expression origin;
    private final List<Binder> binders;

    public SimplifiedExpression(Expression simplified, Expression origin) {
        this(simplified, origin, List.of());
    }

    public SimplifiedExpression(Expression simplified, Expression origin, List<Binder> binders) {
        addChild(simplified);
        this.origin = origin;
        this.binders = new ArrayList<>(binders);
    }

    public Expression getSimplifiedExpression() {
        return children.get(0);
    }

    public Expression getOrigin() {
        return origin;
    }

    public List<Binder> getBinders() {
        return binders;
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) throws LJError {
        return visitor.visitSimplifiedNode(this);
    }

    @Override
    public void getVariableNames(List<String> toAdd) {
        getSimplifiedExpression().getVariableNames(toAdd);
    }

    @Override
    public void getStateInvocations(List<String> toAdd, List<String> all) {
        getSimplifiedExpression().getStateInvocations(toAdd, all);
    }

    @Override
    public boolean isBooleanTrue() {
        return getSimplifiedExpression().isBooleanTrue();
    }

    @Override
    public Expression clone() {
        return new SimplifiedExpression(getSimplifiedExpression().clone(), origin.clone(), binders);
    }

    @Override
    public String toString() {
        return getSimplifiedExpression().toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSimplifiedExpression(), origin, binders);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SimplifiedExpression other = (SimplifiedExpression) obj;
        return getSimplifiedExpression().equals(other.getSimplifiedExpression()) && origin.equals(other.origin)
                && binders.equals(other.binders);
    }

    public static class Binder {
        private final String name;
        private final String type;

        public Binder(String name, String type) {
            this.name = name;
            this.type = type;
        }

        public Binder(String name, CtTypeReference<?> type) {
            this(name, type.getQualifiedName());
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Binder other = (Binder) obj;
            return name.equals(other.name) && type.equals(other.type);
        }
    }
}
