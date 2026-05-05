package liquidjava.rj_language.opt;

import liquidjava.processor.context.Context;
import liquidjava.rj_language.Predicate;
import java.util.Map;

import liquidjava.processor.facade.AliasDTO;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.LiteralBoolean;
import liquidjava.rj_language.ast.UnaryExpression;
import liquidjava.rj_language.opt.derivation_node.BinaryDerivationNode;
import liquidjava.rj_language.opt.derivation_node.DerivationNode;
import liquidjava.rj_language.opt.derivation_node.UnaryDerivationNode;
import liquidjava.rj_language.opt.derivation_node.ValDerivationNode;
import liquidjava.smt.SMTEvaluator;
import liquidjava.smt.SMTResult;

public class ExpressionSimplifier {

    /**
     * Simplifies an expression by applying constant propagation, constant folding, removing redundant conjuncts and
     * expanding aliases Returns a derivation node representing the tree of simplifications applied
     */
    public static ValDerivationNode simplify(Expression exp, Map<String, AliasDTO> aliases) {
        ValDerivationNode fixedPoint = simplifyToFixedPoint(null, exp);
        ValDerivationNode simplified = simplifyValDerivationNode(fixedPoint);
        ValDerivationNode unwrapped = unwrapBooleanLiterals(simplified);
        return AliasExpansion.expand(unwrapped, aliases);
    }

    public static ValDerivationNode simplify(Expression exp) {
        return simplify(exp, Map.of());
    }

    /**
     * Recursively applies propagation and folding until the expression stops changing (fixed point) Stops early if the
     * expression simplifies to a boolean literal, which means we've simplified too much
     */
    private static ValDerivationNode simplifyToFixedPoint(ValDerivationNode current, Expression prevExp) {
        // apply propagation and folding
        ValDerivationNode prop = VariablePropagation.propagate(prevExp, current);
        ValDerivationNode fold = ExpressionFolding.fold(prop);
        ValDerivationNode simplified = simplifyValDerivationNode(fold);
        Expression currExp = simplified.getValue();

        // fixed point reached
        if (current != null && currExp.equals(current.getValue())) {
            return current;
        }

        // prevent oversimplification
        if (current != null && currExp instanceof LiteralBoolean && !(current.getValue() instanceof LiteralBoolean)) {
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

            // remove weaker conjuncts (e.g. x > 0 && x > -1 => x > 0)
            if (implies(leftSimplified.getValue(), rightSimplified.getValue())) {
                return new ValDerivationNode(leftSimplified.getValue(),
                        new BinaryDerivationNode(leftSimplified, rightSimplified, "&&"));
            }
            if (implies(rightSimplified.getValue(), leftSimplified.getValue())) {
                return new ValDerivationNode(rightSimplified.getValue(),
                        new BinaryDerivationNode(leftSimplified, rightSimplified, "&&"));
            }

            // return the conjunction with simplified children
            Expression newValue = new BinaryExpression(leftSimplified.getValue(), "&&", rightSimplified.getValue());
            // only create origin if at least one child has a meaningful origin
            if (leftSimplified.getOrigin() != null || rightSimplified.getOrigin() != null) {
                DerivationNode newOrigin = new BinaryDerivationNode(leftSimplified, rightSimplified, "&&");
                return new ValDerivationNode(newValue, newOrigin);
            }
            return new ValDerivationNode(newValue, null);
        }
        // no simplification
        return node;
    }

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

    /**
     * Recursively traverses the derivation tree and replaces boolean literals with the expressions that produced them,
     * but only when at least one operand in the derivation is non-boolean. e.g. "x == true" where true came from "1 >
     * 0" becomes "x == 1 > 0"
     */
    private static ValDerivationNode unwrapBooleanLiterals(ValDerivationNode node) {
        Expression value = node.getValue();
        DerivationNode origin = node.getOrigin();

        if (origin == null)
            return node;

        // unwrap binary expressions
        if (value instanceof BinaryExpression binExp && origin instanceof BinaryDerivationNode binOrigin) {
            ValDerivationNode left = unwrapBooleanLiterals(binOrigin.getLeft());
            ValDerivationNode right = unwrapBooleanLiterals(binOrigin.getRight());
            if (left != binOrigin.getLeft() || right != binOrigin.getRight()) {
                Expression newValue = new BinaryExpression(left.getValue(), binExp.getOperator(), right.getValue());
                return new ValDerivationNode(newValue, new BinaryDerivationNode(left, right, binOrigin.getOp()));
            }
            return node;
        }

        // unwrap unary expressions
        if (value instanceof UnaryExpression unaryExp && origin instanceof UnaryDerivationNode unaryOrigin) {
            ValDerivationNode operand = unwrapBooleanLiterals(unaryOrigin.getOperand());
            if (operand != unaryOrigin.getOperand()) {
                Expression newValue = new UnaryExpression(unaryExp.getOp(), operand.getValue());
                return new ValDerivationNode(newValue, new UnaryDerivationNode(operand, unaryOrigin.getOp()));
            }
            return node;
        }

        // boolean literal with binary origin: unwrap if at least one child is non-boolean
        if (value instanceof LiteralBoolean && origin instanceof BinaryDerivationNode binOrigin) {
            ValDerivationNode left = unwrapBooleanLiterals(binOrigin.getLeft());
            ValDerivationNode right = unwrapBooleanLiterals(binOrigin.getRight());
            if (!(left.getValue() instanceof LiteralBoolean) || !(right.getValue() instanceof LiteralBoolean)) {
                Expression newValue = new BinaryExpression(left.getValue(), binOrigin.getOp(), right.getValue());
                return new ValDerivationNode(newValue, new BinaryDerivationNode(left, right, binOrigin.getOp()));
            }
            return node;
        }

        // boolean literal with unary origin: unwrap if operand is non-boolean
        if (value instanceof LiteralBoolean && origin instanceof UnaryDerivationNode unaryOrigin) {
            ValDerivationNode operand = unwrapBooleanLiterals(unaryOrigin.getOperand());
            if (!(operand.getValue() instanceof LiteralBoolean)) {
                Expression newValue = new UnaryExpression(unaryOrigin.getOp(), operand.getValue());
                return new ValDerivationNode(newValue, new UnaryDerivationNode(operand, unaryOrigin.getOp()));
            }
            return node;
        }

        return node;
    }

    /**
     * Checks whether one expression implies another by asking Z3, used to remove weaker conjuncts in the simplification
     */
    private static boolean implies(Expression stronger, Expression weaker) {
        try {
            SMTResult result = new SMTEvaluator().verifySubtype(new Predicate(stronger), new Predicate(weaker),
                    Context.getInstance(), true);
            return result.isOk();
        } catch (Exception e) {
            return false;
        }
    }
}
