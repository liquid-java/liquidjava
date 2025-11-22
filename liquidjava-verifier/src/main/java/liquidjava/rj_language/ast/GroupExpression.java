package liquidjava.rj_language.ast;

import java.util.List;

import liquidjava.diagnostics.errors.LJError;
import liquidjava.rj_language.visitors.ExpressionVisitor;

public class GroupExpression extends Expression {

    public GroupExpression(Expression e) {
        addChild(e);
    }

    public Expression getExpression() {
        return children.get(0);
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) throws LJError {
        return visitor.visitGroupExpression(this);
    }

    public String toString() {
        return "(" + getExpression().toString() + ")";
    }

    @Override
    public String toSimplifiedString() {
        return getExpression().toSimplifiedString();
    }

    @Override
    public void getVariableNames(List<String> toAdd) {
        getExpression().getVariableNames(toAdd);
    }

    @Override
    public void getStateInvocations(List<String> toAdd, List<String> all) {
        getExpression().getStateInvocations(toAdd, all);
    }

    @Override
    public Expression clone() {
        return new GroupExpression(getExpression().clone());
    }

    @Override
    public boolean isBooleanTrue() {
        return getExpression().isBooleanTrue();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getExpression() == null) ? 0 : getExpression().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        GroupExpression other = (GroupExpression) obj;
        if (getExpression() == null) {
            return other.getExpression() == null;
        } else {
            return getExpression().equals(other.getExpression());
        }
    }
}
