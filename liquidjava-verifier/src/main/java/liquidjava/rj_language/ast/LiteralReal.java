package liquidjava.rj_language.ast;

import java.util.List;

import liquidjava.diagnostics.errors.LJError;
import liquidjava.rj_language.visitors.ExpressionVisitor;

public class LiteralReal extends Expression {

    private final double value;

    public LiteralReal(double v) {
        value = v;
    }

    public LiteralReal(String v) {
        value = Double.parseDouble(v);
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) throws LJError {
        return visitor.visitLiteralReal(this);
    }

    public String toString() {
        return Double.toString(value);
    }

    public double getValue() {
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
        return new LiteralReal(value);
    }

    @Override
    public boolean isBooleanTrue() {
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Double.hashCode(value);
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
        LiteralReal other = (LiteralReal) obj;
        return Double.doubleToLongBits(value) == Double.doubleToLongBits(other.value);
    }
}
