package liquidjava.rj_language.opt;

import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.LiteralBoolean;
import liquidjava.rj_language.ast.LiteralNull;
import liquidjava.rj_language.opt.derivation_node.BinaryDerivationNode;
import liquidjava.rj_language.opt.derivation_node.DerivationNode;
import liquidjava.rj_language.opt.derivation_node.ValDerivationNode;

public class ExpressionSimplifier {

    /**
     * Simplifies an expression by applying constant propagation, constant folding and removing redundant conjuncts
     * Returns a derivation node representing the tree of simplifications applied
     */
    public static ValDerivationNode simplify(Expression exp) {
        ValDerivationNode fixedPoint = simplifyToFixedPoint(null, exp);
        return simplifyValDerivationNode(fixedPoint);
    }

    /**
     * Recursively applies propagation and folding until the expression stops changing (fixed point) Stops early if the
     * expression simplifies to 'true', which means we've simplified too much
     */
    private static ValDerivationNode simplifyToFixedPoint(ValDerivationNode current, Expression prevExp) {
        // apply propagation and folding
        ValDerivationNode prop = ConstantPropagation.propagate(prevExp, current);
        ValDerivationNode fold = ConstantFolding.fold(prop);
        ValDerivationNode simplified = simplifyValDerivationNode(fold);
        Expression currExp = simplified.getValue();

        // fixed point reached
        if (current != null && currExp.equals(current.getValue())) {
            return current;
        }

        // continue simplifying
        return simplifyToFixedPoint(simplified, simplified.getValue());
    }

    /**
     * Recursively simplifies the derivation tree by removing redundant conjuncts
     */
    private static ValDerivationNode simplifyValDerivationNode(ValDerivationNode node) {
        Expression value = node.getValue();
        DerivationNode origin = node.getOrigin();

        // binary expression with &&
        if (value instanceof BinaryExpression binExp && "&&".equals(binExp.getOperator())) {
            ValDerivationNode leftSimplified;
            ValDerivationNode rightSimplified;

            if (origin instanceof BinaryDerivationNode binOrigin) {
                leftSimplified = simplifyValDerivationNode(binOrigin.getLeft());
                rightSimplified = simplifyValDerivationNode(binOrigin.getRight());
            } else {
                leftSimplified = simplifyValDerivationNode(new ValDerivationNode(binExp.getFirstOperand(), null));
                rightSimplified = simplifyValDerivationNode(new ValDerivationNode(binExp.getSecondOperand(), null));
            }

            // remove null check when equality already implies non-null
            if (isNullCheckImpliedBy(rightSimplified.getValue(), leftSimplified.getValue()))
                return leftSimplified;
            if (isNullCheckImpliedBy(leftSimplified.getValue(), rightSimplified.getValue()))
                return rightSimplified;

            // check if either side is redundant
            if (isRedundant(leftSimplified.getValue()))
                return rightSimplified;
            if (isRedundant(rightSimplified.getValue()))
                return leftSimplified;

            // collapse identical sides (x && x => x)
            if (leftSimplified.getValue().equals(rightSimplified.getValue())) {
                return leftSimplified;
            }

            // collapse symmetric equalities (e.g. x == y && y == x => x == y)
            if (isSymmetricEquality(leftSimplified.getValue(), rightSimplified.getValue())) {
                return leftSimplified;
            }

            // return the conjunction with simplified children
            Expression newValue = new BinaryExpression(leftSimplified.getValue(), "&&", rightSimplified.getValue());
            DerivationNode newOrigin = new BinaryDerivationNode(leftSimplified, rightSimplified, "&&");
            return new ValDerivationNode(newValue, newOrigin);
        }
        // no simplification
        return node;
    }

    /**
     * Checks if a binary expression is of the form x == y && y == x, which can be simplified to x == y
     */
    private static boolean isSymmetricEquality(Expression left, Expression right) {
        if (left instanceof BinaryExpression b1 && "==".equals(b1.getOperator()) && right instanceof BinaryExpression b2
                && "==".equals(b2.getOperator())) {

            Expression l1 = b1.getFirstOperand();
            Expression r1 = b1.getSecondOperand();
            Expression l2 = b2.getFirstOperand();
            Expression r2 = b2.getSecondOperand();
            return l1.equals(r2) && r1.equals(l2);
        }
        return false;
    }

    /**
     * Checks if a null check (x != null) is implied by an equality check (x == y) where y is not null
     */
    private static boolean isNullCheckImpliedBy(Expression nullCheck, Expression context) {

        // check if in form of x != null
        if (!(nullCheck instanceof BinaryExpression nb) || !"!=".equals(nb.getOperator()))
            return false;

        // identify the variable being checked for null
        Expression checkedVar;
        if (nb.getFirstOperand() instanceof LiteralNull && !(nb.getSecondOperand() instanceof LiteralNull)) {
            checkedVar = nb.getSecondOperand();
        } else if (nb.getSecondOperand() instanceof LiteralNull && !(nb.getFirstOperand() instanceof LiteralNull)) {
            checkedVar = nb.getFirstOperand();
        } else {
            return false;
        }

        // check if context contains an equality check of the form x == y where y is not null
        if (!(context instanceof BinaryExpression cb) || !"==".equals(cb.getOperator()))
            return false;

        // check if either side of the equality is the checked variable and the other side is not null
        Expression left = cb.getFirstOperand();
        Expression right = cb.getSecondOperand();
        return (left.equals(checkedVar) && !(right instanceof LiteralNull))
                || (right.equals(checkedVar) && !(left instanceof LiteralNull));
    }

    /**
     * Checks if an expression is redundant (e.g. true or x == x)
     */
    private static boolean isRedundant(Expression exp) {
        // true
        if (exp instanceof LiteralBoolean && exp.isBooleanTrue()) {
            return true;
        }
        // x == x
        if (exp instanceof BinaryExpression binExp) {
            if ("==".equals(binExp.getOperator())) {
                Expression left = binExp.getFirstOperand();
                Expression right = binExp.getSecondOperand();
                return left.equals(right);
            }
        }
        return false;
    }
}
