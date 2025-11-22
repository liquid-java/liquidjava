package liquidjava.rj_language.ast;

import java.util.List;

import liquidjava.diagnostics.errors.LJError;
import liquidjava.rj_language.visitors.ExpressionVisitor;

public class BinaryExpression extends Expression {

    private final String op;

    public BinaryExpression(Expression e1, String op, Expression e2) {
        this.op = op;
        addChild(e1);
        addChild(e2);
    }

    public Expression getFirstOperand() {
        return children.get(0);
    }

    public String getOperator() {
        return op;
    }

    public Expression getSecondOperand() {
        return children.get(1);
    }

    public boolean isLogicOperation() {
        return op.equals("||") || op.equals("&&") || op.equals("-->");
    }

    public boolean isBooleanOperation() {
        return op.equals("==") || op.equals("!=") || op.equals(">=") || op.equals(">") || op.equals("<=")
                || op.equals("<");
    }

    public boolean isArithmeticOperation() {
        return !isLogicOperation() && !isBooleanOperation();
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) throws LJError {
        return visitor.visitBinaryExpression(this);
    }

    @Override
    public String toString() {
        return getFirstOperand().toString() + " " + op + " " + getSecondOperand().toString();
    }

    @Override
    public String toSimplifiedString() {
        return getFirstOperand().toSimplifiedString() + " " + op + " " + getSecondOperand().toSimplifiedString();
    }

    @Override
    public void getVariableNames(List<String> toAdd) {
        getFirstOperand().getVariableNames(toAdd);
        getSecondOperand().getVariableNames(toAdd);
    }

    @Override
    public void getStateInvocations(List<String> toAdd, List<String> all) {
        getFirstOperand().getStateInvocations(toAdd, all);
        getSecondOperand().getStateInvocations(toAdd, all);
    }

    @Override
    public Expression clone() {
        return new BinaryExpression(getFirstOperand().clone(), op, getSecondOperand().clone());
    }

    @Override
    public boolean isBooleanTrue() {
        return switch (op) {
        case "&&", "||", "-->", "==" -> getFirstOperand().isBooleanTrue() && getSecondOperand().isBooleanTrue();
        default -> false;
        };
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getFirstOperand() == null) ? 0 : getFirstOperand().hashCode());
        result = prime * result + ((getSecondOperand() == null) ? 0 : getSecondOperand().hashCode());
        result = prime * result + ((op == null) ? 0 : op.hashCode());
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
        BinaryExpression other = (BinaryExpression) obj;
        if (getFirstOperand() == null) {
            if (other.getFirstOperand() != null)
                return false;
        } else if (!getFirstOperand().equals(other.getFirstOperand()))
            return false;
        if (getSecondOperand() == null) {
            if (other.getSecondOperand() != null)
                return false;
        } else if (!getSecondOperand().equals(other.getSecondOperand()))
            return false;
        if (op == null) {
            return other.op == null;
        } else {
            return op.equals(other.op);
        }
    }
}
