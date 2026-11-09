package liquidjava.rj_language;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import liquidjava.diagnostics.errors.CustomError;
import liquidjava.diagnostics.errors.LJError;
import liquidjava.processor.context.GhostFunction;
import liquidjava.processor.context.GhostState;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.FunctionInvocation;
import liquidjava.rj_language.ast.UnaryExpression;
import liquidjava.rj_language.ast.Var;

/**
 * Reads a state written as a value of its state set, {@code phase(e) == open}, as the state shorthand {@code open(e)},
 * so that everything that reads states sees one form
 */
public final class StateValues {

    private StateValues() {
    }

    /**
     * {@code expression} with each {@code set(e) == state} read as {@code state(e)} and each {@code set(e) != state} as
     * {@code !state(e)}. A name {@code isVariable} accepts keeps its meaning as a variable
     *
     * @throws CustomError
     *             when the name is not a variable and is a state of another set of the same class
     */
    public static Expression toShorthand(Expression expression, List<GhostState> states, Predicate<String> isVariable)
            throws LJError {
        return rewrite(expression.clone(), states, isVariable);
    }

    private static Expression rewrite(Expression e, List<GhostState> states, Predicate<String> isVariable)
            throws LJError {
        if (e instanceof BinaryExpression b && (b.getOperator().equals("==") || b.getOperator().equals("!="))) {
            Optional<FunctionInvocation> state = stateOf(b.getFirstOperand(), b.getSecondOperand(), states, isVariable);
            if (state.isEmpty())
                state = stateOf(b.getSecondOperand(), b.getFirstOperand(), states, isVariable);
            if (state.isPresent())
                return b.getOperator().equals("==") ? state.get() : new UnaryExpression("!", state.get());
        }
        for (int i = 0; i < e.getChildren().size(); i++)
            e.setChild(i, rewrite(e.getChildren().get(i), states, isVariable));
        return e;
    }

    /**
     * {@code state(e)} when {@code set} is a state set applied to {@code e} and {@code name} one of its states, named
     * as the shorthand written in the same place, so that it is resolved the same way, in a subclass too
     */
    private static Optional<FunctionInvocation> stateOf(Expression set, Expression name, List<GhostState> states,
            Predicate<String> isVariable) throws LJError {
        if (!(set instanceof FunctionInvocation invocation && invocation.getArgs().size() == 1
                && name instanceof Var var && !isVariable.test(var.getName())))
            return Optional.empty();
        List<GhostFunction> sets = states.stream().map(GhostState::getParent).filter(Objects::nonNull).distinct()
                .filter(s -> s.matches(invocation.getName())).toList();
        if (sets.stream().anyMatch(s -> !statesOf(s, states, var.getName()).isEmpty()))
            return Optional.of(new FunctionInvocation(qualifiedLike(invocation.getName(), var.getName()),
                    List.of(invocation.getArgs().get(0).clone())));
        for (GhostFunction s : sets)
            if (isStateOfAnotherSet(var.getName(), s, states))
                throw new CustomError(String.format("State '%s' is not a state of '%s' (its states are %s)",
                        var.getName(), s.getName(),
                        statesOf(s, states, null).stream().map(GhostState::getName).collect(Collectors.joining(", "))));
        return Optional.empty();
    }

    /** The states of {@code set}, or only those named {@code name} when it is not null */
    private static List<GhostState> statesOf(GhostFunction set, List<GhostState> states, String name) {
        return states.stream().filter(s -> s.getParent() == set && (name == null || s.getName().equals(name))).toList();
    }

    /** {@code state} qualified as {@code setName} is: {@code a.B.phase} gives {@code a.B.open} */
    private static String qualifiedLike(String setName, String state) {
        int dot = setName.lastIndexOf('.');
        return dot < 0 ? state : setName.substring(0, dot + 1) + state;
    }

    private static boolean isStateOfAnotherSet(String name, GhostFunction stateSet, List<GhostState> states) {
        return states.stream().anyMatch(s -> s.getName().equals(name) && s.getParent() != null
                && s.getParent() != stateSet && s.getParent().getPrefix().equals(stateSet.getPrefix()));
    }
}
