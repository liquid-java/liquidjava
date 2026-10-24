package liquidjava.processor.state_machine;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import liquidjava.diagnostics.errors.LJError;
import liquidjava.processor.context.Context;
import liquidjava.processor.context.GhostFunction;
import liquidjava.processor.context.GhostState;
import liquidjava.processor.context.RefinedFunction;
import liquidjava.processor.context.StateChange;
import liquidjava.processor.context.Variable;
import liquidjava.processor.context.VariableInstance;
import liquidjava.processor.refinement_checker.TypeChecker;
import liquidjava.rj_language.Predicate;
import liquidjava.smt.Counterexample;
import liquidjava.utils.Pair;
import liquidjava.utils.constants.Keys;
import liquidjava.utils.constants.Types;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtTypeReference;

/**
 * An earlier call whose arguments decided the state that a failing call does not allow, with argument values for which
 * the failing call would have been allowed
 *
 * @param call
 *            the deciding call
 * @param values
 *            the values, by parameter name ({@code return} for the call's result)
 */
public record DecidingArgument(StateChange call, Map<String, String> values) {

    // the object after the deciding call, and its result, under names nothing else uses
    private static final String OBJECT = "#decided";
    private static final String RESULT = "return";

    /**
     * @param receiver
     *            the receiver of the failing call
     * @param failing
     *            the method that cannot be called
     * @param ghostStates
     *            the ghost states of the receiver's class
     * @param invocation
     *            the failing call
     */
    public static Optional<DecidingArgument> find(TypeChecker tc, VariableInstance receiver, RefinedFunction failing,
            List<GhostState> ghostStates, CtElement invocation) throws LJError {
        List<Predicate> required = failing.getFromStates();
        if (!required.stream().allMatch(DecidingArgument::mentionsOnlyThis))
            return Optional.empty();
        List<GhostState> states = ghostStates.stream().filter(g -> g.getParent() != null).toList();
        Set<GhostFunction> checked = required.stream().flatMap(p -> p.getStateInvocations(states).stream())
                .map(GhostState::getParent).collect(Collectors.toSet());

        // the last call that changed a state set the failing call checks; a join of branches breaks the chain
        Optional<StateChange> call = receiver.getStateChange();
        while (call.isPresent() && !changes(call.get(), checked, states))
            call = call.get().previous().flatMap(VariableInstance::getStateChange);
        if (call.isEmpty() || dependsOnlyOnState(call.get()))
            return Optional.empty();
        StateChange deciding = call.get();
        return valuesAllowing(tc, deciding, receiver, required, invocation)
                .map(values -> new DecidingArgument(deciding, values));
    }

    private static boolean changes(StateChange call, Set<GhostFunction> stateSets, List<GhostState> states) {
        return call.change().getTo().getStateInvocations(states).stream().map(GhostState::getParent)
                .anyMatch(stateSets::contains);
    }

    private static boolean dependsOnlyOnState(StateChange call) {
        return mentionsOnlyThis(call.change().getFrom()) && mentionsOnlyThis(call.change().getTo());
    }

    private static boolean mentionsOnlyThis(Predicate predicate) {
        return predicate.getVariableNames().stream().allMatch(Keys.THIS::equals);
    }

    /** Values of the deciding call's parameters and result after which one of {@code required} would hold */
    private static Optional<Map<String, String>> valuesAllowing(TypeChecker tc, StateChange call,
            VariableInstance receiver, List<Predicate> required, CtElement invocation) throws LJError {
        Context context = tc.getContext();
        context.enterContext();
        try {
            context.addVarToContext(OBJECT, receiver.getType(), new Predicate(), invocation);
            Predicate to = call.change().getTo().substituteVariable(Keys.THIS, OBJECT);
            if (call.previous().isPresent())
                to = to.changeOldMentions(call.previous().get().getName(), OBJECT);

            Map<String, String> free = new LinkedHashMap<>(); // fresh name -> name shown
            RefinedFunction function = call.function();
            for (Variable parameter : function.getArguments())
                to = leaveFree(context, to, parameter.getName(), parameter.getType(), parameter.getName(), free,
                        invocation);
            boolean returnsValue = function.getType() != null && !"void".equals(function.getType().toString());
            to = returnsValue ? leaveFree(context, to, Keys.WILDCARD, function.getType(), RESULT, free, invocation)
                    : to.substituteVariable(Keys.WILDCARD, OBJECT);

            Predicate before = call.previous().map(VariableInstance::getRefinement).orElse(new Predicate());
            Predicate allowed = required.stream().map(p -> p.substituteVariable(Keys.THIS, OBJECT))
                    .reduce(Predicate.createLit("false", Types.BOOLEAN), Predicate::createDisjunction);
            Optional<Counterexample> witness = new StateSolver(tc, invocation.getPosition())
                    .witness(Predicate.createConjunction(before, to), allowed);
            return witness.map(model -> model.assignments().stream().filter(a -> free.containsKey(a.first())).collect(
                    Collectors.toMap(a -> free.get(a.first()), Pair::second, (a, b) -> a, LinkedHashMap::new)));
        } finally {
            context.exitContext();
        }
    }

    /** Renames {@code name} in {@code predicate} to a fresh variable with no refinement, if it is mentioned */
    private static Predicate leaveFree(Context context, Predicate predicate, String name, CtTypeReference<?> type,
            String shownAs, Map<String, String> free, CtElement invocation) {
        if (!predicate.getVariableNames().contains(name))
            return predicate;
        String fresh = "#" + shownAs;
        context.addVarToContext(fresh, type, new Predicate(), invocation);
        free.put(fresh, shownAs);
        return predicate.substituteVariable(name, fresh);
    }
}
