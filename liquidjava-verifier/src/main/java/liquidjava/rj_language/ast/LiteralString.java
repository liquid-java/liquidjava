package liquidjava.rj_language.ast;

import java.util.List;

import liquidjava.diagnostics.errors.LJError;
import liquidjava.rj_language.visitors.ExpressionVisitor;

public class LiteralString extends Expression {

    private final String value;

    public LiteralString(String v) {
        value = v;
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) throws LJError {
        return visitor.visitLiteralString(this);
    }

    public String toString() {
        return value;
    }

    @Override
    public void getVariableNames(List<String> toAdd) {
        // end leaf
    }

    @Override
    public void getStateInvocations(List<String> toAdd, List<String> all) {
        // end leaf
    }

    @Override
    public Expression clone() {
        return new LiteralString(value);
    }

    @Override
    public boolean isBooleanTrue() {
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
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
        LiteralString other = (LiteralString) obj;
        if (value == null) {
            return other.value == null;
        } else {
            return value.equals(other.value);
        }
    }
}
