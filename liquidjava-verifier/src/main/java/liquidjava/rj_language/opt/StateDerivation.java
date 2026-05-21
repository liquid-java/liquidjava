package liquidjava.rj_language.opt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import liquidjava.processor.context.GhostState;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.FunctionInvocation;
import liquidjava.rj_language.ast.GroupExpression;
import liquidjava.rj_language.opt.derivation_node.BinaryDerivationNode;
import liquidjava.rj_language.opt.derivation_node.StateDerivationNode;
import liquidjava.rj_language.opt.derivation_node.ValDerivationNode;
import liquidjava.utils.Utils;
import liquidjava.utils.constants.Ops;

/**
 * Simplification pass that rewrites internal ghost-state equalities into developer-facing state predicates.
 *
 * <p>
 * Typestate found-state conjunctions mix developer state invocations ({@code startTiling(p)}) with internal ghost-state
 * equalities ({@code state1(a) == state1(b)}). The equalities are an SMT artifact, meaningless to the developer. When a
 * known developer state holds for one side of such an equality, the same state must hold for the other side; this pass
 * derives that state predicate and drops the equality.
 *
 * <p>
 * It is a <b>single pass</b>: it only consults the conjuncts it is given. Chain resolution
 * ({@code state1(a)==state1(b) && state1(b)==state1(c)}) falls out of the surrounding {@code simplifyToFixedPoint} loop
 * re-running the pass until stable.
 *
 * <p>
 * Derivation only matches plain {@link FunctionInvocation} conjuncts and plain {@code stateN(x) == stateN(y)}
 * equalities. It deliberately does <b>not</b> traverse into {@code Ite} / disjunction / negation — a conditional branch
 * is not an asserted fact, so deriving from it would be unsound.
 */
public class StateDerivation {

    /**
     * Rewrites ghost-state equalities in {@code node} into derived developer-state conjuncts. Returns {@code node}
     * unchanged when nothing can be derived, so the enclosing fixed-point loop terminates.
     */
    public static ValDerivationNode derive(ValDerivationNode node, List<GhostState> ghostStates) {
        if (ghostStates == null || ghostStates.isEmpty())
            return node;
        Map<String, String> stateToInternal = buildStateMap(ghostStates);
        if (stateToInternal.isEmpty())
            return node;

        List<ValDerivationNode> conjuncts = flatten(node);
        List<ValDerivationNode> kept = new ArrayList<>();
        List<ValDerivationNode> derived = new ArrayList<>();
        boolean changed = false;

        for (ValDerivationNode conjunct : conjuncts) {
            FunctionInvocation[] equality = stateEquality(conjunct.getValue());
            if (equality != null) {
                List<ValDerivationNode> fromEquality = deriveFromEquality(equality[0], equality[1], conjunct, conjuncts,
                        stateToInternal);
                if (!fromEquality.isEmpty()) {
                    derived.addAll(fromEquality);
                    changed = true;
                    continue; // drop the equality — it has been rewritten as developer states
                }
            }
            kept.add(conjunct);
        }

        if (!changed)
            return node;

        List<ValDerivationNode> result = new ArrayList<>(kept);
        for (ValDerivationNode d : derived) {
            if (result.stream().noneMatch(c -> sameValue(c, d)))
                result.add(d);
        }
        return rebuildConjunction(result, node);
    }

    /**
     * The set of internal {@code stateN} function names backing the given developer states. Used to tell variable
     * propagation which ghost-state equalities it must leave intact for derivation to consume.
     */
    public static Set<String> internalStateFunctions(List<GhostState> ghostStates) {
        Set<String> names = new HashSet<>();
        if (ghostStates == null)
            return names;
        for (String internal : buildStateMap(ghostStates).values())
            names.add(Utils.getSimpleName(internal));
        return names;
    }

    /** Maps each developer state name to the internal {@code stateN} function backing its refinement. */
    private static Map<String, String> buildStateMap(List<GhostState> ghostStates) {
        Map<String, String> map = new HashMap<>();
        for (GhostState gs : ghostStates) {
            if (gs.getRefinement() == null)
                continue;
            Expression ref = unwrap(gs.getRefinement().getExpression());
            if (ref instanceof BinaryExpression be && Ops.EQ.equals(be.getOperator())) {
                FunctionInvocation state = functionInvocation(be.getFirstOperand());
                if (state == null)
                    state = functionInvocation(be.getSecondOperand());
                if (state != null)
                    map.put(gs.getName(), state.getName());
            }
        }
        return map;
    }

    /** Returns {@code {left, right}} when {@code exp} is {@code stateN(x) == stateN(y)}; otherwise {@code null}. */
    private static FunctionInvocation[] stateEquality(Expression exp) {
        Expression e = unwrap(exp);
        if (!(e instanceof BinaryExpression be) || !Ops.EQ.equals(be.getOperator()))
            return null;
        FunctionInvocation left = functionInvocation(be.getFirstOperand());
        FunctionInvocation right = functionInvocation(be.getSecondOperand());
        if (left == null || right == null || left.getArgs().size() != 1 || right.getArgs().size() != 1)
            return null;
        if (!sameName(left.getName(), right.getName()))
            return null;
        return new FunctionInvocation[] { left, right };
    }

    /**
     * For an equality {@code stateN(x) == stateN(y)}, finds every known developer state holding for one operand and
     * derives the same state for the other operand. Each derived node carries a {@link StateDerivationNode} recording
     * the equality and the known state it came from.
     */
    private static List<ValDerivationNode> deriveFromEquality(FunctionInvocation left, FunctionInvocation right,
            ValDerivationNode equalityNode, List<ValDerivationNode> conjuncts, Map<String, String> stateToInternal) {
        List<ValDerivationNode> derived = new ArrayList<>();
        String internal = left.getName();
        for (ValDerivationNode conjunct : conjuncts) {
            FunctionInvocation state = functionInvocation(conjunct.getValue());
            if (state == null || state.getArgs().size() != 1)
                continue;
            for (Map.Entry<String, String> entry : stateToInternal.entrySet()) {
                if (!sameName(entry.getKey(), state.getName()) || !sameName(entry.getValue(), internal))
                    continue;
                Expression known = state.getArgs().get(0);
                Expression target = known.equals(left.getArgs().get(0)) ? right.getArgs().get(0)
                        : known.equals(right.getArgs().get(0)) ? left.getArgs().get(0) : null;
                if (target != null)
                    derived.add(new ValDerivationNode(invocation(state.getName(), target),
                            new StateDerivationNode(equalityNode, conjunct)));
            }
        }
        return derived;
    }

    private static FunctionInvocation invocation(String name, Expression arg) {
        List<Expression> args = new ArrayList<>();
        args.add(arg.clone());
        return new FunctionInvocation(name, args);
    }

    /** Splits a left-associated {@code &&} derivation tree into its conjunct nodes, preserving their origins. */
    private static List<ValDerivationNode> flatten(ValDerivationNode node) {
        List<ValDerivationNode> out = new ArrayList<>();
        flattenInto(node, out);
        return out;
    }

    private static void flattenInto(ValDerivationNode node, List<ValDerivationNode> out) {
        Expression value = node.getValue();
        if (value instanceof BinaryExpression be && Ops.AND.equals(be.getOperator())) {
            if (node.getOrigin()instanceof BinaryDerivationNode bin) {
                flattenInto(bin.getLeft(), out);
                flattenInto(bin.getRight(), out);
            } else {
                flattenInto(new ValDerivationNode(be.getFirstOperand(), null), out);
                flattenInto(new ValDerivationNode(be.getSecondOperand(), null), out);
            }
        } else {
            out.add(node);
        }
    }

    /** Rebuilds a left-associated {@code &&} conjunction from {@code conjuncts}. */
    private static ValDerivationNode rebuildConjunction(List<ValDerivationNode> conjuncts, ValDerivationNode fallback) {
        if (conjuncts.isEmpty())
            return fallback;
        ValDerivationNode acc = conjuncts.get(0);
        for (int i = 1; i < conjuncts.size(); i++) {
            ValDerivationNode next = conjuncts.get(i);
            Expression value = new BinaryExpression(acc.getValue(), Ops.AND, next.getValue());
            acc = new ValDerivationNode(value, new BinaryDerivationNode(acc, next, Ops.AND));
        }
        return acc;
    }

    private static FunctionInvocation functionInvocation(Expression exp) {
        Expression e = unwrap(exp);
        return e instanceof FunctionInvocation fi ? fi : null;
    }

    private static Expression unwrap(Expression exp) {
        return exp instanceof GroupExpression ge ? unwrap(ge.getExpression()) : exp;
    }

    private static boolean sameName(String first, String second) {
        return first.equals(second) || Utils.getSimpleName(first).equals(Utils.getSimpleName(second));
    }

    private static boolean sameValue(ValDerivationNode a, ValDerivationNode b) {
        return a.getValue().toString().equals(b.getValue().toString());
    }
}
