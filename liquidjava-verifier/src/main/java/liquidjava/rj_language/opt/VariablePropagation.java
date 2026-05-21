package liquidjava.rj_language.opt;

import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Enum;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.FunctionInvocation;
import liquidjava.rj_language.ast.UnaryExpression;
import liquidjava.rj_language.ast.Var;
import liquidjava.rj_language.opt.derivation_node.BinaryDerivationNode;
import liquidjava.rj_language.opt.derivation_node.DerivationNode;
import liquidjava.rj_language.opt.derivation_node.IteDerivationNode;
import liquidjava.rj_language.opt.derivation_node.UnaryDerivationNode;
import liquidjava.rj_language.opt.derivation_node.ValDerivationNode;
import liquidjava.rj_language.opt.derivation_node.VarDerivationNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import liquidjava.utils.Utils;

public class VariablePropagation {

    /**
     * Performs constant and variable propagation on an expression, by substituting variables. Uses the VariableResolver
     * to extract variable equalities from the expression first. Returns a derivation node representing the propagation
     * steps taken.
     */
    public static ValDerivationNode propagate(Expression exp, ValDerivationNode previousOrigin) {
        return propagate(exp, previousOrigin, Set.of());
    }

    /**
     * Variant of {@link #propagate(Expression, ValDerivationNode)} that leaves ghost-state equalities intact.
     *
     * <p>
     * Substitutions of the form {@code stateN(x) -> stateN(y)} (both sides invocations of a ghost-state function named
     * in {@code protectedStateFunctions}) are dropped before propagation. Collapsing them would erase the shared middle
     * term of an equality chain ({@code state1(a)==state1(b) && state1(b)==state1(c)}), which state-equality derivation
     * needs whole. These equalities are an SMT artifact consumed only by error-message derivation.
     */
    public static ValDerivationNode propagate(Expression exp, ValDerivationNode previousOrigin,
            Set<String> protectedStateFunctions) {
        Map<String, Expression> substitutions = VariableResolver.resolve(exp);
        if (!protectedStateFunctions.isEmpty()) {
            substitutions.entrySet()
                    .removeIf(e -> isProtectedStateInvocation(e.getKey(), protectedStateFunctions)
                            && e.getValue()instanceof FunctionInvocation fi
                            && protectedStateFunctions.contains(Utils.getSimpleName(fi.getName())));
        }
        Map<String, Expression> directSubstitutions = new HashMap<>(); // var == literal or var == var
        Map<String, Expression> expressionSubstitutions = new HashMap<>(); // var == expression
        for (Map.Entry<String, Expression> entry : substitutions.entrySet()) {
            Expression value = entry.getValue();
            if (value.isLiteral() || value instanceof Var || value instanceof Enum) {
                directSubstitutions.put(entry.getKey(), value);
            } else {
                expressionSubstitutions.put(entry.getKey(), value);
            }
        }

        // map of variable origins from the previous derivation tree
        Map<String, DerivationNode> varOrigins = new HashMap<>();
        if (previousOrigin != null) {
            extractVarOrigins(previousOrigin, varOrigins);
        }
        Map<String, Expression> activeSubstitutions = directSubstitutions.isEmpty() ? expressionSubstitutions
                : directSubstitutions;
        return propagateRecursive(exp, activeSubstitutions, varOrigins);
    }

    /** A substitution key {@code f(...)} whose function {@code f} is one of the protected ghost-state functions. */
    private static boolean isProtectedStateInvocation(String key, Set<String> stateFunctions) {
        int paren = key.indexOf('(');
        return paren > 0 && stateFunctions.contains(Utils.getSimpleName(key.substring(0, paren)));
    }

    /**
     * Recursively performs propagation on an expression (e.g. x + y && x == 1 && y == 2 => 1 + 2)
     */
    private static ValDerivationNode propagateRecursive(Expression exp, Map<String, Expression> subs,
            Map<String, DerivationNode> varOrigins) {

        // substitute variable
        if (exp instanceof Var var) {
            String name = var.getName();
            Expression value = subs.get(name);
            // substitution
            if (value != null) {
                // check if this variable has an origin from a previous pass
                DerivationNode previousOrigin = varOrigins.get(name);

                // preserve origin if value came from previous derivation
                DerivationNode origin = previousOrigin != null ? new VarDerivationNode(name, previousOrigin)
                        : new VarDerivationNode(name);
                return new ValDerivationNode(value.clone(), origin);
            }

            // no substitution
            return new ValDerivationNode(var, null);
        }

        if (exp instanceof FunctionInvocation) {
            Expression value = subs.get(exp.toString());
            if (value != null)
                return new ValDerivationNode(value.clone(), new VarDerivationNode(exp.toString()));
        }

        // lift unary origin
        if (exp instanceof UnaryExpression unary) {
            ValDerivationNode operand = propagateRecursive(unary.getChildren().get(0), subs, varOrigins);
            UnaryExpression cloned = (UnaryExpression) unary.clone();
            cloned.setChild(0, operand.getValue());

            return operand.getOrigin() != null
                    ? new ValDerivationNode(cloned, new UnaryDerivationNode(operand, cloned.getOp()))
                    : new ValDerivationNode(cloned, null);
        }

        // lift binary origin
        if (exp instanceof BinaryExpression binary) {
            ValDerivationNode left = propagateRecursive(binary.getFirstOperand(), subs, varOrigins);
            ValDerivationNode right = propagateRecursive(binary.getSecondOperand(), subs, varOrigins);
            BinaryExpression cloned = (BinaryExpression) binary.clone();
            cloned.setChild(0, left.getValue());
            cloned.setChild(1, right.getValue());

            return (left.getOrigin() != null || right.getOrigin() != null)
                    ? new ValDerivationNode(cloned, new BinaryDerivationNode(left, right, cloned.getOperator()))
                    : new ValDerivationNode(cloned, null);
        }

        // recursively propagate children
        if (exp.hasChildren()) {
            Expression propagated = exp.clone();
            for (int i = 0; i < exp.getChildren().size(); i++) {
                ValDerivationNode child = propagateRecursive(exp.getChildren().get(i), subs, varOrigins);
                propagated.setChild(i, child.getValue());
            }
            return new ValDerivationNode(propagated, null);
        }

        // no propagation
        return new ValDerivationNode(exp, null);
    }

    /**
     * Extracts the derivation nodes for variable values from the derivation tree This is so done so when we find "var
     * == value" in the tree, we store the derivation of the value So it can be preserved when var is substituted in
     * subsequent passes
     */
    private static void extractVarOrigins(ValDerivationNode node, Map<String, DerivationNode> varOrigins) {
        if (node == null)
            return;

        Expression value = node.getValue();
        DerivationNode origin = node.getOrigin();

        // check for equality expressions
        if (value instanceof BinaryExpression binExp && "==".equals(binExp.getOperator())
                && origin instanceof BinaryDerivationNode binOrigin) {
            Expression left = binExp.getFirstOperand();
            Expression right = binExp.getSecondOperand();

            // extract variable name and value derivation from either side
            String varName = null;
            ValDerivationNode valueDerivation = null;

            if (left instanceof Var var && right.isLiteral()) {
                varName = var.getName();
                valueDerivation = binOrigin.getRight();
            } else if (right instanceof Var var && left.isLiteral()) {
                varName = var.getName();
                valueDerivation = binOrigin.getLeft();
            }
            if (varName != null && valueDerivation != null && valueDerivation.getOrigin() != null) {
                varOrigins.put(varName, valueDerivation.getOrigin());
            }
        }

        // recursively process the origin tree
        if (origin instanceof BinaryDerivationNode binOrigin) {
            extractVarOrigins(binOrigin.getLeft(), varOrigins);
            extractVarOrigins(binOrigin.getRight(), varOrigins);
        } else if (origin instanceof UnaryDerivationNode unaryOrigin) {
            extractVarOrigins(unaryOrigin.getOperand(), varOrigins);
        } else if (origin instanceof IteDerivationNode iteOrigin) {
            extractVarOrigins(iteOrigin.getCondition(), varOrigins);
            extractVarOrigins(iteOrigin.getThenBranch(), varOrigins);
            extractVarOrigins(iteOrigin.getElseBranch(), varOrigins);
        } else if (origin instanceof ValDerivationNode valOrigin) {
            extractVarOrigins(valOrigin, varOrigins);
        }
    }
}