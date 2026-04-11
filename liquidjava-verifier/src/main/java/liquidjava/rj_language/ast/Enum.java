package liquidjava.rj_language.ast;

import java.util.List;

import liquidjava.diagnostics.errors.LJError;
import liquidjava.rj_language.visitors.ExpressionVisitor;

public class Enum extends Expression {

    private final String typeName;
    private final String constName;

    public Enum(String typeName, String constName) {
        this.typeName = typeName;
        this.constName = constName;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getConstName() {
        return constName;
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) throws LJError {
        return visitor.visitEnum(this);
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
    public boolean isBooleanTrue() {
        return false;
    }

    @Override
    public String toString() {
        return typeName + "." + constName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((typeName == null) ? 0 : typeName.hashCode());
        result = prime * result + ((constName == null) ? 0 : constName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Enum other = (Enum) obj;
        return typeName.equals(other.typeName) && constName.equals(other.constName);
    }

    @Override
    public Expression clone() {
        return new Enum(typeName, constName);
    }
}
