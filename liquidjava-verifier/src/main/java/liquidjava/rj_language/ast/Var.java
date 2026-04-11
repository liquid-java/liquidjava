package liquidjava.rj_language.ast;

import java.util.List;

import liquidjava.diagnostics.errors.LJError;
import liquidjava.rj_language.visitors.ExpressionVisitor;

public class Var extends Expression {

    private final String name;

    public Var(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) throws LJError {
        return visitor.visitVar(this);
    }

    public String toString() {
        return name;
    }

    @Override
    public void getVariableNames(List<String> toAdd) {
        if (!toAdd.contains(name))
            toAdd.add(name);
    }

    @Override
    public void getStateInvocations(List<String> toAdd, List<String> all) {
        // end leaf
    }

    @Override
    public Expression clone() {
        return new Var(name);
    }

    @Override
    public boolean isBooleanTrue() {
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        Var other = (Var) obj;
        if (name == null) {
            return other.name == null;
        } else {
            return name.equals(other.name);
        }
    }
}
