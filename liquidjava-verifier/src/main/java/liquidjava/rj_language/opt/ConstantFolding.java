package liquidjava.rj_language.opt;

import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.GroupExpression;
import liquidjava.rj_language.ast.LiteralBoolean;
import liquidjava.rj_language.ast.LiteralInt;
import liquidjava.rj_language.ast.LiteralReal;
import liquidjava.rj_language.ast.UnaryExpression;
import liquidjava.rj_language.opt.derivation_node.BinaryDerivationNode;
import liquidjava.rj_language.opt.derivation_node.DerivationNode;
import liquidjava.rj_language.opt.derivation_node.UnaryDerivationNode;
import liquidjava.rj_language.opt.derivation_node.ValDerivationNode;

public class ConstantFolding {

    /**
     * Performs constant folding on a derivation node by evaluating nodes with constant values. Returns a new derivation
     * node representing the folding steps taken
     */
    public static ValDerivationNode fold(ValDerivationNode node) {
        Expression exp = node.getValue();
        if (exp instanceof BinaryExpression)
            return foldBinary(node);

        if (exp instanceof UnaryExpression)
            return foldUnary(node);

        if (exp instanceof GroupExpression group) {
            if (group.getChildren().size() == 1) {
                return fold(new ValDerivationNode(group.getChildren().get(0), node.getOrigin()));
            }
        }
        return node;
    }

    /**
     * Folds a binary expression node if both children are constant values (e.g. 1 + 2 => 3)
     */
    private static ValDerivationNode foldBinary(ValDerivationNode node) {
        BinaryExpression binExp = (BinaryExpression) node.getValue();
        DerivationNode parent = node.getOrigin();

        // fold child nodes
        ValDerivationNode leftNode;
        ValDerivationNode rightNode;
        if (parent instanceof BinaryDerivationNode binaryOrigin) {
            // has origin (from constant propagation)
            leftNode = fold(binaryOrigin.getLeft());
            rightNode = fold(binaryOrigin.getRight());
        } else {
            // no origin
            leftNode = fold(new ValDerivationNode(binExp.getFirstOperand(), null));
            rightNode = fold(new ValDerivationNode(binExp.getSecondOperand(), null));
        }

        Expression left = leftNode.getValue();
        Expression right = rightNode.getValue();
        String op = binExp.getOperator();
        binExp.setChild(0, left);
        binExp.setChild(1, right);

        // int and int
        if (left instanceof LiteralInt && right instanceof LiteralInt) {
            int l = ((LiteralInt) left).getValue();
            int r = ((LiteralInt) right).getValue();
            Expression res = switch (op) {
            case "+" -> new LiteralInt(l + r);
            case "-" -> new LiteralInt(l - r);
            case "*" -> new LiteralInt(l * r);
            case "/" -> r != 0 ? new LiteralInt(l / r) : null;
            case "%" -> r != 0 ? new LiteralInt(l % r) : null;
            case "<" -> new LiteralBoolean(l < r);
            case "<=" -> new LiteralBoolean(l <= r);
            case ">" -> new LiteralBoolean(l > r);
            case ">=" -> new LiteralBoolean(l >= r);
            case "==" -> new LiteralBoolean(l == r);
            case "!=" -> new LiteralBoolean(l != r);
            default -> null;
            };
            if (res != null)
                return new ValDerivationNode(res, new BinaryDerivationNode(leftNode, rightNode, op));
        }
        // real and real
        else if (left instanceof LiteralReal && right instanceof LiteralReal) {
            double l = ((LiteralReal) left).getValue();
            double r = ((LiteralReal) right).getValue();
            Expression res = switch (op) {
            case "+" -> new LiteralReal(l + r);
            case "-" -> new LiteralReal(l - r);
            case "*" -> new LiteralReal(l * r);
            case "/" -> r != 0.0 ? new LiteralReal(l / r) : null;
            case "%" -> r != 0.0 ? new LiteralReal(l % r) : null;
            case "<" -> new LiteralBoolean(l < r);
            case "<=" -> new LiteralBoolean(l <= r);
            case ">" -> new LiteralBoolean(l > r);
            case ">=" -> new LiteralBoolean(l >= r);
            case "==" -> new LiteralBoolean(l == r);
            case "!=" -> new LiteralBoolean(l != r);
            default -> null;
            };
            if (res != null)
                return new ValDerivationNode(res, new BinaryDerivationNode(leftNode, rightNode, op));
        }

        // mixed int and real
        else if ((left instanceof LiteralInt && right instanceof LiteralReal)
                || (left instanceof LiteralReal && right instanceof LiteralInt)) {
            double l = left instanceof LiteralInt ? ((LiteralInt) left).getValue() : ((LiteralReal) left).getValue();
            double r = right instanceof LiteralInt ? ((LiteralInt) right).getValue() : ((LiteralReal) right).getValue();
            Expression res = switch (op) {
            case "+" -> new LiteralReal(l + r);
            case "-" -> new LiteralReal(l - r);
            case "*" -> new LiteralReal(l * r);
            case "/" -> r != 0.0 ? new LiteralReal(l / r) : null;
            case "%" -> r != 0.0 ? new LiteralReal(l % r) : null;
            case "<" -> new LiteralBoolean(l < r);
            case "<=" -> new LiteralBoolean(l <= r);
            case ">" -> new LiteralBoolean(l > r);
            case ">=" -> new LiteralBoolean(l >= r);
            case "==" -> new LiteralBoolean(l == r);
            case "!=" -> new LiteralBoolean(l != r);
            default -> null;
            };
            if (res != null)
                return new ValDerivationNode(res, new BinaryDerivationNode(leftNode, rightNode, op));
        }
        // bool and bool
        else if (left instanceof LiteralBoolean && right instanceof LiteralBoolean) {
            boolean l = left.isBooleanTrue();
            boolean r = right.isBooleanTrue();
            Expression res = switch (op) {
            case "&&" -> new LiteralBoolean(l && r);
            case "||" -> new LiteralBoolean(l || r);
            case "-->" -> new LiteralBoolean(!l || r);
            case "==" -> new LiteralBoolean(l == r);
            case "!=" -> new LiteralBoolean(l != r);
            default -> null;
            };
            if (res != null)
                return new ValDerivationNode(res, new BinaryDerivationNode(leftNode, rightNode, op));
        }

        // no folding
        DerivationNode origin = (leftNode.getOrigin() != null || rightNode.getOrigin() != null)
                ? new BinaryDerivationNode(leftNode, rightNode, op) : null;
        return new ValDerivationNode(binExp, origin);
    }

    /**
     * Folds a unary expression node if the child (operand) is a constant value (e.g. !true => false)
     */
    private static ValDerivationNode foldUnary(ValDerivationNode node) {
        UnaryExpression unaryExp = (UnaryExpression) node.getValue();
        DerivationNode parent = node.getOrigin();

        // fold child node
        ValDerivationNode operandNode;
        if (parent instanceof UnaryDerivationNode unaryOrigin) {
            // has origin (from constant propagation)
            operandNode = fold(unaryOrigin.getOperand());
        } else {
            // no origin
            operandNode = fold(new ValDerivationNode(unaryExp.getChildren().get(0), null));
        }
        Expression operand = operandNode.getValue();
        String operator = unaryExp.getOp();
        unaryExp.setChild(0, operand);

        // unary not
        if ("!".equals(operator) && operand instanceof LiteralBoolean) {
            // !true => false, !false => true
            boolean value = operand.isBooleanTrue();
            Expression res = new LiteralBoolean(!value);
            DerivationNode origin = operandNode.getOrigin() != null ? new UnaryDerivationNode(operandNode, operator)
                    : null;
            return new ValDerivationNode(res, origin);
        }
        // unary minus
        if ("-".equals(operator)) {
            // -(x) => -x
            if (operand instanceof LiteralInt) {
                Expression res = new LiteralInt(-((LiteralInt) operand).getValue());
                DerivationNode origin = operandNode.getOrigin() != null ? new UnaryDerivationNode(operandNode, operator)
                        : null;
                return new ValDerivationNode(res, origin);
            }
            if (operand instanceof LiteralReal) {
                Expression res = new LiteralReal(-((LiteralReal) operand).getValue());
                DerivationNode origin = operandNode.getOrigin() != null ? new UnaryDerivationNode(operandNode, operator)
                        : null;
                return new ValDerivationNode(res, origin);
            }
        }

        // no folding
        DerivationNode origin = operandNode.getOrigin() != null ? new UnaryDerivationNode(operandNode, operator) : null;
        return new ValDerivationNode(unaryExp, origin);
    }
}