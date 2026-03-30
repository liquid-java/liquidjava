package liquidjava.rj_language.opt;

import java.util.Map;

import liquidjava.processor.facade.AliasDTO;
import liquidjava.rj_language.ast.AliasInvocation;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.GroupExpression;
import liquidjava.rj_language.ast.UnaryExpression;
import liquidjava.rj_language.ast.Var;
import liquidjava.rj_language.opt.derivation_node.BinaryDerivationNode;
import liquidjava.rj_language.opt.derivation_node.DerivationNode;
import liquidjava.rj_language.opt.derivation_node.UnaryDerivationNode;
import liquidjava.rj_language.opt.derivation_node.ValDerivationNode;

public class AliasExpansion {

    /**
     * Expands alias invocations in a derivation node to their definitions, storing the expanded body as the origin of
     * each alias invocation node.
     */
    public static ValDerivationNode expand(ValDerivationNode node, Map<String, AliasDTO> aliases) {
        return expandRecursive(node, aliases);
    }

    private static ValDerivationNode expandRecursive(ValDerivationNode node, Map<String, AliasDTO> aliases) {
        Expression exp = node.getValue();

        // expand alias invocation
        if (exp instanceof AliasInvocation ai) {
            return expandAlias(ai, aliases);
        }

        // recurse into binary expressions
        if (exp instanceof BinaryExpression binary) {
            ValDerivationNode leftNode;
            ValDerivationNode rightNode;
            if (node.getOrigin()instanceof BinaryDerivationNode binOrigin) {
                leftNode = expandRecursive(binOrigin.getLeft(), aliases);
                rightNode = expandRecursive(binOrigin.getRight(), aliases);
            } else {
                leftNode = expandRecursive(new ValDerivationNode(binary.getFirstOperand(), null), aliases);
                rightNode = expandRecursive(new ValDerivationNode(binary.getSecondOperand(), null), aliases);
            }
            boolean hasExpansion = leftNode.getOrigin() != null || rightNode.getOrigin() != null;
            DerivationNode origin = hasExpansion ? new BinaryDerivationNode(leftNode, rightNode, binary.getOperator())
                    : node.getOrigin();
            return new ValDerivationNode(exp, origin);
        }

        // recurse into unary expressions
        if (exp instanceof UnaryExpression unary) {
            ValDerivationNode operandNode;
            if (node.getOrigin()instanceof UnaryDerivationNode unaryOrigin) {
                operandNode = expandRecursive(unaryOrigin.getOperand(), aliases);
            } else {
                operandNode = expandRecursive(new ValDerivationNode(unary.getChildren().get(0), null), aliases);
            }
            DerivationNode origin = operandNode.getOrigin() != null
                    ? new UnaryDerivationNode(operandNode, unary.getOp()) : node.getOrigin();
            return new ValDerivationNode(exp, origin);
        }

        // recurse into group expressions
        if (exp instanceof GroupExpression group && group.getChildren().size() == 1) {
            return expandRecursive(new ValDerivationNode(group.getChildren().get(0), node.getOrigin()), aliases);
        }

        return node;
    }

    private static ValDerivationNode expandAlias(AliasInvocation ai, Map<String, AliasDTO> aliases) {
        AliasDTO dto = aliases.get(ai.getName());

        // no alias found
        if (dto == null || dto.getExpression() == null) {
            return new ValDerivationNode(ai, null);
        }

        // substitute parameters in the body with the invocation arguments
        Expression body = dto.getExpression().clone();
        for (int i = 0; i < ai.getArgs().size() && i < dto.getVarNames().size(); i++) {
            body = body.substitute(new Var(dto.getVarNames().get(i)), ai.getArgs().get(i));
        }

        // recursively expand the body
        ValDerivationNode expandedBody = expandRecursive(new ValDerivationNode(body, null), aliases);
        return new ValDerivationNode(ai, expandedBody);
    }
}
