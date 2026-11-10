package liquidjava.rj_language;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
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
import liquidjava.utils.Utils;
import liquidjava.utils.constants.Formats;

/**
 * Reads a state written as a value of its state set, {@code phase(e) == open}, as the state shorthand {@code open(e)},
 * so that everything that reads states sees one form
 */
public final class StateValues {

    private final List<GhostState> states;
    private final Predicate<String> isVariable;
    private final Function<Expression, Optional<Predicate<GhostFunction>>> setsFor;

    /**
     * @param isVariable
     *            whether a name is a variable where the spec is written; a variable keeps its meaning
     * @param setsFor
     *            the state sets that can be applied to an argument, by its type, or empty when its type is not known
     */
    public StateValues(List<GhostState> states, Predicate<String> isVariable,
            Function<Expression, Optional<Predicate<GhostFunction>>> setsFor) {
        this.states = states;
        this.isVariable = isVariable;
        this.setsFor = setsFor;
    }

    /**
     * {@code expression} with each {@code set(e) == state} read as {@code state(e)} and each {@code set(e) != state} as
     * {@code !state(e)}
     *
     * @throws CustomError
     *             when the name is not a variable and is a state of another set of the same class
     */
    public Expression toShorthand(Expression expression) throws LJError {
        return rewrite(expression.clone());
    }

    private Expression rewrite(Expression e) throws LJError {
        if (e instanceof BinaryExpression b && (b.getOperator().equals("==") || b.getOperator().equals("!="))) {
            Optional<FunctionInvocation> state = stateOf(b.getFirstOperand(), b.getSecondOperand());
            if (state.isEmpty())
                state = stateOf(b.getSecondOperand(), b.getFirstOperand());
            if (state.isPresent())
                return b.getOperator().equals("==") ? state.get() : new UnaryExpression("!", state.get());
        }
        for (int i = 0; i < e.getChildren().size(); i++)
            e.setChild(i, rewrite(e.getChildren().get(i)));
        return e;
    }

    /**
     * {@code state(e)} when {@code set} is a state set applied to {@code e} and {@code name} a state of a set of that
     * name that applies to the type of {@code e}, or, when that type is not known, of every set of that name. It is
     * named as the shorthand written in the same place, so that it is resolved the same way, in a subclass too.
     * Otherwise nothing is read, and the name stays an unknown variable
     */
    private Optional<FunctionInvocation> stateOf(Expression set, Expression name) throws LJError {
        if (!(set instanceof FunctionInvocation invocation && invocation.getArgs().size() == 1
                && name instanceof Var var && !isVariable.test(var.getName())))
            return Optional.empty();
        Expression argument = invocation.getArgs().get(0);
        Optional<Predicate<GhostFunction>> ofType = setsFor.apply(argument);
        List<GhostFunction> sets = states.stream().map(GhostState::getParent).filter(Objects::nonNull).distinct()
                .filter(s -> s.matches(invocation.getName()) && ofType.map(p -> p.test(s)).orElse(true)).toList();
        long withState = sets.stream().filter(s -> !statesOf(s, var.getName()).isEmpty()).count();
        if (withState > 0 && (ofType.isPresent() || withState == sets.size()))
            return Optional.of(new FunctionInvocation(qualifiedLike(invocation.getName(), var.getName()),
                    List.of(argument.clone())));
        if (withState == 0)
            for (GhostFunction s : sets)
                if (isStateOfAnotherSet(var.getName(), s))
                    throw new CustomError(String.format("State '%s' is not a state of '%s' (its states are %s)",
                            var.getName(), s.getName(),
                            statesOf(s, null).stream().map(GhostState::getName).collect(Collectors.joining(", "))));
        return Optional.empty();
    }

    /**
     * {@code expression} with each state of a named set, {@code open(e)}, written as a value of its set,
     * {@code phase(e) == open}, and {@code !open(e)} as {@code phase(e) != open}, for display. A state keeps the
     * shorthand when its set has a generated name, such as {@code state1}, when sets with different names have a state
     * of that name, or when a ghost that is not a state has that name: the name alone does not tell which it is
     *
     * @param ghosts
     *            the ghosts that are not states, and the state set functions
     */
    public static Expression toSetForm(Expression expression, List<GhostState> states, List<GhostFunction> ghosts) {
        return rewriteToSetForm(expression.clone(), states, ghosts);
    }

    private static Expression rewriteToSetForm(Expression e, List<GhostState> states, List<GhostFunction> ghosts) {
        if (e instanceof UnaryExpression u && u.getOp().equals("!")) {
            Optional<BinaryExpression> value = setValue(u.getExpression(), "!=", states, ghosts);
            if (value.isPresent())
                return value.get();
        }
        Optional<BinaryExpression> value = setValue(e, "==", states, ghosts);
        if (value.isPresent())
            return value.get();
        for (int i = 0; i < e.getChildren().size(); i++)
            e.setChild(i, rewriteToSetForm(e.getChildren().get(i), states, ghosts));
        return e;
    }

    /** {@code set(e) op state} when {@code e} is the state {@code state(e)} of a set with one, given name */
    private static Optional<BinaryExpression> setValue(Expression e, String op, List<GhostState> states,
            List<GhostFunction> ghosts) {
        if (!(e instanceof FunctionInvocation invocation && invocation.getArgs().size() == 1))
            return Optional.empty();
        return setNameOf(invocation.getName(), states, ghosts).map(setName -> new BinaryExpression(
                new FunctionInvocation(qualifiedLike(invocation.getName(), setName),
                        List.of(invocation.getArgs().get(0).clone())),
                op, new Var(Utils.getSimpleName(invocation.getName()))));
    }

    /**
     * The name of the state set that {@link #toSetForm} shows the state {@code state} as a value of, or empty when it
     * keeps the shorthand
     */
    public static Optional<String> setNameOf(String state, List<GhostState> states, List<GhostFunction> ghosts) {
        if (ghosts.stream().anyMatch(g -> g.matches(state))
                || states.stream().anyMatch(s -> s.getParent() == null && s.matches(state)))
            return Optional.empty();
        List<String> setNames = states.stream().filter(s -> s.getParent() != null && s.matches(state))
                .map(s -> s.getParent().getName()).distinct().toList();
        if (setNames.size() != 1 || setNames.get(0).matches(Formats.STATE_SET_NAMES))
            return Optional.empty();
        return Optional.of(setNames.get(0));
    }

    /** The states of {@code set}, or only those named {@code name} when it is not null */
    private List<GhostState> statesOf(GhostFunction set, String name) {
        return states.stream().filter(s -> s.getParent() == set && (name == null || s.getName().equals(name))).toList();
    }

    /** {@code state} qualified as {@code setName} is: {@code a.B.phase} gives {@code a.B.open} */
    private static String qualifiedLike(String setName, String state) {
        int dot = setName.lastIndexOf('.');
        return dot < 0 ? state : setName.substring(0, dot + 1) + state;
    }

    private boolean isStateOfAnotherSet(String name, GhostFunction stateSet) {
        return states.stream().anyMatch(s -> s.getName().equals(name) && s.getParent() != null
                && s.getParent() != stateSet && s.getParent().getPrefix().equals(stateSet.getPrefix()));
    }
}
