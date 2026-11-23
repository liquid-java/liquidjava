package liquidjava.processor.refinement_checker;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.UnaryExpression;
import liquidjava.utils.constants.Ops;
import liquidjava.utils.constants.Types;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.code.UnaryOperatorKind;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtVariable;

/**
 * The candidate invariants of an {@code int} local variable that a loop changes: facts that may hold at the loop head
 * on every iteration, to be kept only if they are proved there. As the qualifiers of Liquid Types, they come from a
 * fixed set of shapes:
 * <ul>
 * <li>{@code x >= x0} and {@code x <= x0}, with {@code x0} the instance of {@code x} before the loop: it only grows, or
 * only shrinks;</li>
 * <li>the comparisons of the loop condition that mention {@code x}, made non-strict so that the iteration that ends the
 * loop can keep them ({@code x < n} gives {@code x <= n}).</li>
 * </ul>
 * The comparisons are taken from the conjuncts of the condition, and only over integer literals, {@code int} local
 * variables and parameters, and {@code +}, {@code -} and {@code *}; a field or a call may change out of sight of the
 * loop.
 */
final class InvariantCandidates {

    /** The most candidates of a variable, so that a loop costs a bounded number of checks */
    static final int MAX_PER_VARIABLE = 6;

    private InvariantCandidates() {
    }

    /**
     * @param variable
     *            the name of an {@code int} local variable the loop changes
     * @param before
     *            the name of its instance just before the loop, if it has one
     * @param condition
     *            the loop condition, or null if it has none
     *
     * @return the candidates in a fixed order, without duplicates
     */
    static List<Predicate> of(String variable, Optional<String> before, CtExpression<Boolean> condition) {
        Set<Predicate> candidates = new LinkedHashSet<>();
        before.ifPresent(instance -> {
            candidates.add(
                    Predicate.createOperation(Predicate.createVar(variable), Ops.GE, Predicate.createVar(instance)));
            candidates.add(
                    Predicate.createOperation(Predicate.createVar(variable), Ops.LE, Predicate.createVar(instance)));
        });
        for (CtBinaryOperator<?> comparison : comparisons(condition)) {
            Optional<Predicate> left = term(comparison.getLeftHandOperand());
            Optional<Predicate> right = term(comparison.getRightHandOperand());
            if (left.isPresent() && right.isPresent() && (left.get().getVariableNames().contains(variable)
                    || right.get().getVariableNames().contains(variable)))
                candidates.add(Predicate.createOperation(left.get(), nonStrict(comparison.getKind()), right.get()));
        }
        return candidates.stream().limit(MAX_PER_VARIABLE).toList();
    }

    /** The order comparisons ({@code <}, {@code <=}, {@code >}, {@code >=}) among the conjuncts of {@code condition} */
    private static List<CtBinaryOperator<?>> comparisons(CtExpression<?> condition) {
        List<CtBinaryOperator<?>> comparisons = new ArrayList<>();
        if (condition instanceof CtBinaryOperator<?> operator) {
            switch (operator.getKind()) {
            case AND -> {
                comparisons.addAll(comparisons(operator.getLeftHandOperand()));
                comparisons.addAll(comparisons(operator.getRightHandOperand()));
            }
            case LT, LE, GT, GE -> comparisons.add(operator);
            default -> {
            }
            }
        }
        return comparisons;
    }

    private static String nonStrict(BinaryOperatorKind kind) {
        return switch (kind) {
        case LT, LE -> Ops.LE;
        case GT, GE -> Ops.GE;
        default -> throw new IllegalArgumentException("Not an order comparison: " + kind);
        };
    }

    /** The integer term an expression stands for, if it is built only of what a candidate may mention */
    private static Optional<Predicate> term(CtExpression<?> expression) {
        if (expression instanceof CtLiteral<?> literal && literal.getValue()instanceof Integer value)
            return Optional.of(Predicate.createLit(value.toString(), Types.INT));
        if (expression instanceof CtVariableRead<?> read && isIntLocal(read.getVariable().getDeclaration()))
            return Optional.of(Predicate.createVar(read.getVariable().getSimpleName()));
        if (expression instanceof CtUnaryOperator<?> operator && operator.getKind() == UnaryOperatorKind.NEG)
            return term(operator.getOperand())
                    .map(operand -> new Predicate(new UnaryExpression(Ops.MINUS, operand.getExpression())));
        if (expression instanceof CtBinaryOperator<?> operator) {
            String op = switch (operator.getKind()) {
            case PLUS -> Ops.PLUS;
            case MINUS -> Ops.MINUS;
            case MUL -> Ops.MUL;
            default -> null;
            };
            Optional<Predicate> left = term(operator.getLeftHandOperand());
            Optional<Predicate> right = term(operator.getRightHandOperand());
            if (op != null && left.isPresent() && right.isPresent())
                return Optional.of(Predicate.createOperation(left.get(), op, right.get()));
        }
        return Optional.empty();
    }

    /** Whether a variable is an {@code int} local variable or parameter */
    private static boolean isIntLocal(CtVariable<?> variable) {
        return (variable instanceof CtLocalVariable<?> || variable instanceof CtParameter<?>)
                && variable.getType() != null && variable.getType().getQualifiedName().equals(Types.INT);
    }
}
