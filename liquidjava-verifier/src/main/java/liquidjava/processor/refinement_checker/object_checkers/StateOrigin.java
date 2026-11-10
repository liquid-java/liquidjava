package liquidjava.processor.refinement_checker.object_checkers;

import static liquidjava.rj_language.opt.VCSimplificationUtils.factsOf;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import liquidjava.diagnostics.DebugLog;
import liquidjava.diagnostics.errors.LJError;
import liquidjava.processor.context.CallResult;
import liquidjava.processor.context.Context;
import liquidjava.processor.context.GhostFunction;
import liquidjava.processor.context.GhostState;
import liquidjava.processor.context.RefinedFunction;
import liquidjava.processor.context.StateChange;
import liquidjava.processor.context.VariableInstance;
import liquidjava.processor.refinement_checker.TypeChecker;
import liquidjava.processor.state_machine.State;
import liquidjava.processor.state_machine.StateMachine;
import liquidjava.processor.state_machine.StateSolver;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.StateValues;
import liquidjava.rj_language.ast.AliasInvocation;
import liquidjava.rj_language.ast.Expression;
import liquidjava.utils.Utils;
import liquidjava.utils.constants.Formats;
import liquidjava.utils.constants.Keys;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.code.CtVariableWrite;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.visitor.filter.TypeFilter;

/**
 * Where the state of the receiver of a call that its state does not allow comes from, as a note of the error: the last
 * call that set it, or the call whose value the receiver was assigned. Each note is given only when what the verifier
 * knows proves it, and is left out otherwise
 */
final class StateOrigin {

    private final TypeChecker tc;
    private final VariableInstance receiver;
    private final String written;
    private final Set<GhostFunction> shown;
    private final List<GhostState> ghostStates;
    private final CtElement invocation;

    /**
     * @param receiver
     *            the receiver's instance before the call
     * @param written
     *            the receiver as written in the call
     * @param shown
     *            the state sets the note is about
     */
    private StateOrigin(TypeChecker tc, VariableInstance receiver, String written, Set<GhostFunction> shown,
            List<GhostState> ghostStates, CtElement invocation) {
        this.tc = tc;
        this.receiver = receiver;
        this.written = written;
        this.shown = shown;
        this.ghostStates = ghostStates;
        this.invocation = invocation;
    }

    /**
     * The note on where the states of the receiver that the failing call checks come from, or of only those of them
     * whose state no allowed state has, if the machine tells
     *
     * @param found
     *            what is known about the receiver's states before the call
     * @param expected
     *            the states the call needs
     */
    static Optional<String> note(TypeChecker tc, VariableInstance receiver, String written, RefinedFunction failing,
            List<GhostState> ghostStates, Optional<StateMachine> machine, Set<State> current, Predicate found,
            Predicate expected, CtElement invocation) throws LJError {
        Set<GhostFunction> checked = setsOf(failing.getFromStates(), ghostStates);
        Set<GhostFunction> wrong = machine.filter(m -> m.models(failing))
                .map(m -> wrongSets(checked, current, m.statesAllowing(failing), ghostStates)).orElse(Set.of());
        StateOrigin origin = new StateOrigin(tc, receiver, written, wrong.isEmpty() ? checked : wrong, ghostStates,
                invocation);
        return checked.isEmpty() ? Optional.empty() : origin.note(tc.shownFound(found, expected));
    }

    /** The sets where some state the receiver may be in is not the state of any state that allows the call */
    private static Set<GhostFunction> wrongSets(Set<GhostFunction> checked, Set<State> current, Set<State> allowing,
            List<GhostState> ghostStates) {
        return checked.stream()
                .filter(set -> !namesIn(allowing, set, ghostStates).containsAll(namesIn(current, set, ghostStates)))
                .collect(Collectors.toSet());
    }

    private static Set<String> namesIn(Set<State> states, GhostFunction set, List<GhostState> ghostStates) {
        Set<String> names = ghostStates.stream().filter(g -> g.getParent() == set)
                .map(g -> Utils.getSimpleName(g.getName())).collect(Collectors.toSet());
        return states.stream().flatMap(s -> s.names().stream()).filter(names::contains).collect(Collectors.toSet());
    }

    private static Set<GhostFunction> setsOf(List<Predicate> predicates, List<GhostState> ghostStates) {
        List<GhostState> states = ghostStates.stream().filter(g -> g.getParent() != null).toList();
        return predicates.stream().flatMap(p -> p.getStateInvocations(states).stream()).map(GhostState::getParent)
                .collect(Collectors.toSet());
    }

    private Optional<String> note(Predicate shownFound) throws LJError {
        Optional<StateChange> change = receiver.getStateChange();
        if (change.isEmpty())
            return receiver.getReturnedBy().isPresent() ? returnedNote(receiver.getReturnedBy().get(), shownFound)
                    : creationNote(receiver, shownFound);
        VariableInstance after = receiver;
        // the changes crossed leave the shown sets as they were: a state refinement keeps the sets it does not name
        while (!setsOf(List.of(change.get().change().getTo()), ghostStates).stream().anyMatch(shown::contains)) {
            Optional<VariableInstance> before = change.get().previous();
            if (before.isEmpty())
                return Optional.empty();
            after = before.get();
            change = after.getStateChange();
            if (change.isEmpty())
                return creationNote(after, shownFound);
        }
        return lastSetNote(change.get(), after, shownFound);
    }

    /** That the shown facts of found were last set by {@code change}, if what it gave {@code after} proves them */
    private Optional<String> lastSetNote(StateChange change, VariableInstance after, Predicate shownFound)
            throws LJError {
        Optional<Predicate> facts = factsOn(shownFound, receiver.getName());
        if (facts.isEmpty()
                || !implies(after.getRefinement(), facts.get().substituteVariable(receiver.getName(), after.getName())))
            return Optional.empty();
        return code(change.position()).map(code -> String.format("From: %s, last set at line %d: %s",
                display(facts.get(), receiver.getName()), change.position().getLine(), code));
    }

    /**
     * What the call the receiver was assigned from returns about the shown sets, if it proves what found says about
     * them, or that it says nothing about them when found says nothing either and its return refinement names no state
     * of them and no other value the state could come from
     */
    private Optional<String> returnedNote(CallResult call, Predicate shownFound) throws LJError {
        Optional<String> code = code(call.position());
        if (code.isEmpty())
            return Optional.empty();
        String assigned = String.format("From: %s = %s, line %d, ", written, code.get(), call.position().getLine());
        Optional<Predicate> facts = factsOn(call.returned(), Keys.WILDCARD);
        Optional<Predicate> found = factsOn(shownFound, receiver.getName());
        if (facts.isPresent())
            return found.isPresent()
                    && implies(facts.get().substituteVariable(Keys.WILDCARD, receiver.getName()), found.get())
                            ? Optional.of(assigned + "which returns " + display(facts.get(), Keys.WILDCARD))
                            : Optional.empty();
        Predicate returned = call.returned();
        // a set declared without a name has a generated one, which the message does not otherwise show
        boolean saysNothing = found.isEmpty()
                && shown.stream().noneMatch(set -> Utils.getSimpleName(set.getName()).matches(Formats.STATE_SET_NAMES))
                && setsOf(List.of(returned), ghostStates).stream().noneMatch(shown::contains)
                && returned.getVariableNames().stream().allMatch(Keys.WILDCARD::equals)
                && !hasAlias(returned.getExpression());
        String sets = shown.stream().map(set -> Utils.getSimpleName(set.getName()) + "(" + written + ")").sorted()
                .collect(Collectors.joining(", "));
        return saysNothing ? Optional.of(assigned + "which says nothing about " + sets) : Optional.empty();
    }

    /**
     * When the receiver is a local variable whose only write is its initializer, a constructor call, what that
     * constructor makes it in the shown sets, if found still says it is in that state. It is a fact about the
     * constructor only, given when the walk back to the call that last set the state broke, e.g. at a join of branches
     */
    private Optional<String> creationNote(VariableInstance unknown, Predicate shownFound) throws LJError {
        DebugLog.warn("From note: no known call gives " + unknown.getName() + " its state, e.g. a join of branches");
        if (!(invocation instanceof CtInvocation<?> call && call.getTarget()instanceof CtVariableRead<?> read
                && read.getVariable().getDeclaration()instanceof CtLocalVariable<?> local
                && local.getDefaultExpression()instanceof CtConstructorCall<?> constructor
                && constructor.getMetadata(Keys.STATE_CHANGE)instanceof StateChange created
                && created.function().getAllStates().size() == 1 && !writtenAfterDeclaration(local)))
            return Optional.empty();
        Optional<Predicate> facts = factsOn(created.change().getTo(), Keys.THIS);
        Optional<Predicate> found = factsOn(shownFound, receiver.getName());
        Optional<String> code = code(created.position());
        if (facts.isEmpty() || found.isEmpty() || code.isEmpty()
                || !implies(found.get(), facts.get().substituteVariable(Keys.THIS, receiver.getName())))
            return Optional.empty();
        return Optional.of(String.format("From: %s is created at line %d by %s, which makes it %s", written,
                created.position().getLine(), code.get(), display(facts.get(), Keys.THIS)));
    }

    private static boolean writtenAfterDeclaration(CtLocalVariable<?> local) {
        CtExecutable<?> scope = local.getParent(CtExecutable.class);
        return scope == null || scope.getElements(new TypeFilter<>(CtVariableWrite.class)).stream()
                .anyMatch(write -> write.getVariable().getDeclaration() == local);
    }

    /**
     * The facts of {@code predicate} on the shown sets of {@code object} alone, e.g. {@code unmarked(object)}, or empty
     * if it has none
     */
    private Optional<Predicate> factsOn(Predicate predicate, String object) {
        return factsOf(predicate.getExpression()).stream().map(Predicate::new)
                .filter(fact -> fact.getVariableNames().stream().allMatch(object::equals))
                .filter(fact -> !setsOf(List.of(fact), ghostStates).isEmpty()
                        && shown.containsAll(setsOf(List.of(fact), ghostStates)))
                .reduce(Predicate::createConjunction);
    }

    /** The facts as the error shows them, about the receiver as written */
    private String display(Predicate facts, String object) {
        Context context = tc.getContext();
        Expression shownFacts = facts.substituteVariable(object, written).getExpression();
        return StateValues.toSetForm(shownFacts, context.getGhostStates(), context.getGhosts()).toDisplayString();
    }

    private boolean implies(Predicate premise, Predicate conclusion) throws LJError {
        return new StateSolver(tc, invocation.getPosition()).implies(premise, conclusion);
    }

    private static boolean hasAlias(Expression expression) {
        return expression instanceof AliasInvocation
                || expression.getChildren().stream().anyMatch(StateOrigin::hasAlias);
    }

    /** The code at {@code position} on one line, without the semicolon of a statement */
    private static Optional<String> code(SourcePosition position) {
        if (position == null || !position.isValidPosition() || position.getCompilationUnit() == null)
            return Optional.empty();
        String source = position.getCompilationUnit().getOriginalSourceCode();
        if (source == null || position.getSourceEnd() >= source.length())
            return Optional.empty();
        String code = source.substring(position.getSourceStart(), position.getSourceEnd() + 1);
        return Optional.of(code.strip().replaceAll("\\s+", " ").replaceAll(";$", ""));
    }
}
