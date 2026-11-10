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
import liquidjava.processor.context.ObjectState;
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
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtLambda;
import spoon.reflect.code.CtLoop;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtThisAccess;
import spoon.reflect.code.CtTargetedExpression;
import spoon.reflect.code.CtThrow;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.code.CtVariableWrite;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtVariable;
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

    /**
     * Walks back from the receiver to what last gave the shown sets their state. The changes crossed leave the shown
     * sets as they were: a state refinement keeps the sets it does not name
     */
    private Optional<String> note(Predicate shownFound) throws LJError {
        if (mayBeAliased())
            return Optional.empty();
        VariableInstance at = receiver;
        while (true) {
            Optional<StateChange> change = at.getStateChange();
            if (change.isEmpty())
                return at.getReturnedBy().isPresent() ? returnedNote(at.getReturnedBy().get(), at, shownFound)
                        : creationNote(at, shownFound);
            if (setsOf(List.of(change.get().change().getTo()), ghostStates).stream().anyMatch(shown::contains))
                return lastSetNote(change.get(), at, shownFound);
            Optional<VariableInstance> before = change.get().previous();
            if (before.isEmpty())
                return Optional.empty();
            at = before.get();
        }
    }

    /**
     * Whether another name may hold the receiver, through which its state may change unseen, or a method body may
     * change it: before the call, the receiver is used other than as the target of a call or field access or in a
     * comparison (copied, passed to a method, stored in an array, used in a lambda or a local class), or a method of
     * the analyzed code with no state refinement is called on it. A field, which other code may also reach, counts as
     * aliased
     */
    private boolean mayBeAliased() {
        CtExpression<?> target = invocation instanceof CtInvocation<?> call ? call.getTarget() : null;
        CtExecutable<?> scope = invocation.getParent(CtExecutable.class);
        if (scope == null)
            return true;
        List<CtElement> uses;
        if (target == null || target.isImplicit() || target instanceof CtThisAccess<?>) {
            CtType<?> owner = invocation.getParent(CtType.class);
            uses = scope.getElements(new TypeFilter<>(CtThisAccess.class)).stream()
                    .filter(use -> use.getParent(CtType.class) == owner).map(CtElement.class::cast).toList();
        } else if (target instanceof CtVariableRead<?> read && !(read instanceof CtFieldRead<?>)
                && read.getVariable().getDeclaration() != null) {
            CtVariable<?> variable = read.getVariable().getDeclaration();
            uses = scope.getElements(new TypeFilter<>(CtVariableRead.class)).stream()
                    .filter(use -> use.getVariable().getDeclaration() == variable).map(CtElement.class::cast).toList();
        } else
            return true;
        return uses.stream().filter(use -> mayRunBefore(use, scope))
                .anyMatch(use -> escapes(use, scope) || callsUnspecifiedBody(use));
    }

    /**
     * Whether {@code use} is the receiver of a call to a method of the analyzed code with no state refinement: the
     * verifier takes it to keep the state, which its body may still change
     */
    private boolean callsUnspecifiedBody(CtElement use) {
        return use.getParent()instanceof CtInvocation<?> call && call != invocation && call.getTarget() == use
                && call.getExecutable().getDeclaration()instanceof CtMethod<?> method && method.getBody() != null
                && method.getAnnotations().stream().noneMatch(a -> a.getAnnotationType().getQualifiedName()
                        .equals("liquidjava.specification.StateRefinement"));
    }

    /**
     * Whether {@code use} may run before the failing call: it comes earlier, is one of its arguments, or a loop around
     * both may repeat it, unless it is in a return or a throw, which leaves the loop
     */
    private boolean mayRunBefore(CtElement use, CtExecutable<?> scope) {
        if (use.getPosition().getSourceEnd() < invocation.getPosition().getSourceStart())
            return true;
        if (invocation instanceof CtInvocation<?> call
                && call.getArguments().stream().anyMatch(argument -> argument == use || use.hasParent(argument)))
            return true;
        CtElement exit = use.getParent(e -> e instanceof CtReturn<?> || e instanceof CtThrow);
        for (CtLoop loop = invocation.getParent(CtLoop.class); loop != null
                && loop.hasParent(scope); loop = loop.getParent(CtLoop.class))
            if (use.hasParent(loop) && (exit == null || !exit.hasParent(loop)))
                return true;
        return false;
    }

    /** Whether the object {@code use} gives may afterwards be reached through it by another name */
    private static boolean escapes(CtElement use, CtExecutable<?> scope) {
        CtElement parent = use.getParent();
        CtElement nested = use.getParent(e -> e instanceof CtLambda<?> || e instanceof CtClass<?>);
        // a method reference, such as task::start, keeps its target for later
        boolean target = (parent instanceof CtInvocation<?> || parent instanceof CtFieldAccess<?>)
                && ((CtTargetedExpression<?, ?>) parent).getTarget() == use;
        boolean compared = parent instanceof CtBinaryOperator<?> operator
                && (operator.getKind() == BinaryOperatorKind.EQ || operator.getKind() == BinaryOperatorKind.NE);
        return !(target || compared) || nested != null && nested.hasParent(scope);
    }

    /**
     * The facts of found on the shown sets, about {@code at}: those about the receiver, which has the state of
     * {@code at} in those sets, or else those about {@code at}
     */
    private Optional<Predicate> foundAbout(VariableInstance at, Predicate shownFound, Set<GhostFunction> sets) {
        return factsOn(shownFound, receiver.getName(), sets)
                .map(f -> f.substituteVariable(receiver.getName(), at.getName()))
                .or(() -> factsOn(shownFound, at.getName(), sets));
    }

    /**
     * That the facts of found on the shown sets {@code change} names were last set by it, if what it gave {@code after}
     * proves them
     */
    private Optional<String> lastSetNote(StateChange change, VariableInstance after, Predicate shownFound)
            throws LJError {
        Set<GhostFunction> changed = setsOf(List.of(change.change().getTo()), ghostStates).stream()
                .filter(shown::contains).collect(Collectors.toSet());
        Optional<Predicate> facts = foundAbout(after, shownFound, changed);
        if (facts.isEmpty() || !implies(after.getRefinement(), facts.get()))
            return Optional.empty();
        // the call last set these sets, so it is what makes them so
        return code(change.position()).map(code -> String.format("Origin: line %d, %s makes %s",
                change.position().getLine(), code, display(facts.get(), after.getName())));
    }

    /**
     * What the call the receiver was assigned from returns about the shown sets, if it proves what found says about
     * them, or that it says nothing about them when found says nothing either and its return refinement names no state
     * of them and no other value the state could come from
     */
    private Optional<String> returnedNote(CallResult call, VariableInstance assigned, Predicate shownFound)
            throws LJError {
        Optional<String> code = code(call.position());
        if (code.isEmpty())
            return Optional.empty();
        String origin = String.format("Origin: line %d, %s ", call.position().getLine(), code.get());
        Optional<Predicate> facts = factsOn(call.returned(), Keys.WILDCARD, shown);
        Optional<Predicate> found = foundAbout(assigned, shownFound, shown);
        if (facts.isPresent())
            return found.isPresent()
                    && implies(facts.get().substituteVariable(Keys.WILDCARD, assigned.getName()), found.get())
                            ? Optional.of(origin + "returns " + display(facts.get(), Keys.WILDCARD)) : Optional.empty();
        Predicate returned = call.returned();
        // a set declared without a name has a generated one, which the message does not otherwise show
        // a state refinement of the method may give the value its state, as `to = "running(_)"` does
        boolean saysNothing = found.isEmpty() && call.function().getAllStates().stream().noneMatch(ObjectState::hasTo)
                && shown.stream().noneMatch(set -> Utils.getSimpleName(set.getName()).matches(Formats.STATE_SET_NAMES))
                && setsOf(List.of(returned), ghostStates).stream().noneMatch(shown::contains)
                && returned.getVariableNames().stream().allMatch(Keys.WILDCARD::equals)
                && !hasAlias(returned.getExpression());
        String sets = shown.stream().map(set -> Utils.getSimpleName(set.getName()) + "(" + written + ")").sorted()
                .collect(Collectors.joining(", "));
        return saysNothing ? Optional.of(origin + "says nothing about " + sets) : Optional.empty();
    }

    /**
     * When the receiver is a local variable whose only write is its initializer, a constructor call, what that
     * constructor makes it in the shown sets, if found still says it is in that state. It is a fact about the
     * constructor only, given when the walk back to the call that last set the state broke, e.g. at a join of branches
     */
    private Optional<String> creationNote(VariableInstance unknown, Predicate shownFound) throws LJError {
        DebugLog.warn("Origin note: no known call gives " + unknown.getName() + " its state, e.g. a join of branches");
        if (!(invocation instanceof CtInvocation<?> call && call.getTarget()instanceof CtVariableRead<?> read
                && read.getVariable().getDeclaration()instanceof CtLocalVariable<?> local
                && local.getDefaultExpression()instanceof CtConstructorCall<?> constructor
                && constructor.getMetadata(Keys.STATE_CHANGE)instanceof StateChange created
                && created.function().getAllStates().size() == 1 && !writtenAfterDeclaration(local)
                && !changedByAnotherCall(local)))
            return Optional.empty();
        Optional<Predicate> facts = factsOn(created.change().getTo(), Keys.THIS, shown);
        Optional<Predicate> found = foundAbout(unknown, shownFound, shown);
        Optional<String> code = code(created.position());
        if (facts.isEmpty() || found.isEmpty() || code.isEmpty()
                || !implies(found.get(), facts.get().substituteVariable(Keys.THIS, unknown.getName())))
            return Optional.empty();
        // not "makes": a later call may have set the state again, which found no longer tells
        return Optional.of(String.format("Origin: line %d, %s creates %s with %s", created.position().getLine(),
                code.get(), written, display(facts.get(), Keys.THIS)));
    }

    /**
     * Whether a call on the local other than the failing one, or the failing one if a loop may repeat it, is to a
     * method that changes the state of an object, so the state may come from that call rather than the constructor
     */
    private boolean changedByAnotherCall(CtLocalVariable<?> local) {
        CtExecutable<?> scope = local.getParent(CtExecutable.class);
        boolean repeated = invocation.getParent(CtLoop.class) != null
                && invocation.getParent(CtLoop.class).hasParent(scope);
        return scope.getElements(new TypeFilter<>(CtInvocation.class)).stream()
                .filter(call -> call.getTarget()instanceof CtVariableRead<?> read
                        && read.getVariable().getDeclaration() == local && (call != invocation || repeated))
                .anyMatch(this::mayChangeState);
    }

    /** Whether some method with the name and arity of the invoked one changes the state of its receiver */
    private boolean mayChangeState(CtInvocation<?> call) {
        String name = call.getExecutable().getSimpleName();
        int arity = call.getArguments().size();
        return tc.getContext().getCtxFunctions().stream().anyMatch(f -> Utils.getSimpleName(f.getName()).equals(name)
                && f.getArguments().size() == arity && f.getAllStates().stream().anyMatch(ObjectState::hasTo));
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
    private Optional<Predicate> factsOn(Predicate predicate, String object, Set<GhostFunction> sets) {
        return factsOf(predicate.getExpression()).stream().map(Predicate::new)
                .filter(fact -> fact.getVariableNames().stream().allMatch(object::equals))
                .filter(fact -> !setsOf(List.of(fact), ghostStates).isEmpty()
                        && sets.containsAll(setsOf(List.of(fact), ghostStates)))
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
