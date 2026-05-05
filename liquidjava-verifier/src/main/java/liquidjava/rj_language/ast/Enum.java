package liquidjava.rj_language.ast;

import java.util.List;

import liquidjava.diagnostics.errors.LJError;
import liquidjava.rj_language.visitors.ExpressionVisitor;

public class Enum extends Expression {

    private final String typeName;
    private final String constName;
    /**
     * If this {@code Type.CONST} reference resolved to a Java {@code static final} primitive/String constant, the
     * corresponding RJ literal expression is stashed here so the SMT translator can emit a binding axiom
     * ({@code Type.CONST == literalValue}) while preserving the symbolic name in the AST. {@code null} for user-defined
     * enum constants and for unresolvable references.
     */
    private Expression resolvedLiteral;

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

    public Expression getResolvedLiteral() {
        return resolvedLiteral;
    }

    public void setResolvedLiteral(Expression resolvedLiteral) {
        this.resolvedLiteral = resolvedLiteral;
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
        Enum c = new Enum(typeName, constName);
        c.resolvedLiteral = resolvedLiteral == null ? null : resolvedLiteral.clone();
        return c;
    }
}
