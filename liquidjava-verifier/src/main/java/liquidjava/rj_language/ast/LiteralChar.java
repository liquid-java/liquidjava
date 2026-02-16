package liquidjava.rj_language.ast;

import java.util.List;

import liquidjava.diagnostics.errors.LJError;
import liquidjava.rj_language.visitors.ExpressionVisitor;

public class LiteralChar extends Expression {

    private final char value;

    public LiteralChar(char value) {
        this.value = value;
    }

    public LiteralChar(String value) {
        this.value = value.charAt(0);
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) throws LJError {
        return visitor.visitLiteralChar(this);
    }

    public char getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "'" + value + "'";
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
        return new LiteralChar(value);
    }

    @Override
    public boolean isBooleanTrue() {
        return false;
    }

    @Override
    public int hashCode() {
        return Character.hashCode(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LiteralChar other = (LiteralChar) obj;
        return value == other.value;
    }
}