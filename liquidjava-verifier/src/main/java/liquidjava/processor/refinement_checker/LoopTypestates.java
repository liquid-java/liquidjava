package liquidjava.processor.refinement_checker;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import liquidjava.diagnostics.errors.LJError;
import liquidjava.processor.context.RefinedVariable;
import liquidjava.processor.context.Variable;
import liquidjava.processor.context.VariableInstance;
import liquidjava.processor.state_machine.State;
import liquidjava.processor.state_machine.StateMachineBuilder;
import liquidjava.processor.state_machine.StateSolver;
import liquidjava.rj_language.Predicate;
import liquidjava.utils.constants.Keys;
import liquidjava.utils.constants.Types;
import spoon.reflect.code.CtLoop;

/**
 * The typestates of the objects a loop may change, as sets of the states of their class. The states at the loop head
 * start as the states before the loop and grow with the states at the end of each checked iteration until they stop
 * changing. A class has finitely many states, so they stop changing after at most one growth per state.
 *
 * <p>
 * While the body is checked, the states where control leaves the iteration are recorded: where it may exit the loop
 * (the condition, a break), and where it goes on to the next iteration (the end of the body, a continue).
 */
class LoopTypestates {

    private final RefinementTypeChecker checker;
    private final CtLoop loop;
    private final Map<String, List<State>> classStates = new LinkedHashMap<>();
    private final Map<String, Set<State>> head = new LinkedHashMap<>();
    private Map<String, Set<State>> exit;
    private Map<String, Set<State>> continued;
    private Map<String, Set<State>> nextIteration;
    // whether the point of the iteration being checked can be reached
    private boolean reachable;

    /**
     * @param changed
     *            the variables the loop may change, of which those whose class has states are tracked
     */
    LoopTypestates(RefinementTypeChecker checker, CtLoop loop, Set<String> changed) throws LJError {
        this.checker = checker;
        this.loop = loop;
        for (String name : changed) {
            if (!(checker.getContext().getVariableByName(name)instanceof Variable variable)
                    || variable.getType() == null)
                continue;
            List<State> states = StateMachineBuilder.statesOf(checker.getContext(),
                    variable.getType().getQualifiedName(), true);
            if (states.isEmpty())
                continue;
            classStates.put(name, states);
            head.put(name, currentStates(name));
        }
    }

    /** The variables whose typestate is tracked, which must not be havocked */
    Set<String> tracked() {
        return classStates.keySet();
    }

    CtLoop loop() {
        return loop;
    }

    /** Starts checking an iteration: each tracked object is in one of the states at the loop head */
    void startIteration() {
        head.forEach(this::setStates);
        exit = emptyStates();
        continued = emptyStates();
        nextIteration = emptyStates();
        reachable = true;
    }

    /**
     * The loop may exit here, through its condition, if this point is reachable (the condition of a do-while is not
     * when every path through the body returns)
     */
    void recordExit() throws LJError {
        if (reachable)
            addCurrentStates(exit);
    }

    void recordBreak() throws LJError {
        addCurrentStates(exit);
    }

    void recordContinue() throws LJError {
        addCurrentStates(continued);
    }

    /**
     * Marks the end of the body, where the paths through its continues join the path through its last statement: from
     * here on, each object is in a state it may be in at the end of the body or at any of the continues
     *
     * @param completes
     *            whether the end of the body can be reached without a continue
     */
    void endBody(boolean completes) throws LJError {
        if (continued.values().stream().allMatch(Set::isEmpty)) {
            reachable = completes;
            return;
        }
        if (completes)
            recordContinue();
        continued.forEach(this::setStates);
        reachable = true;
    }

    /** The next iteration starts here, if this point is reachable */
    void recordNextIteration() throws LJError {
        if (reachable)
            addCurrentStates(nextIteration);
    }

    /**
     * Adds the states at the start of the next iteration to the states at the loop head
     *
     * @return whether they were already there, so the iteration just checked is the last one to check
     */
    boolean reachedFixpoint() {
        boolean grown = false;
        for (String name : tracked())
            grown |= head.get(name).addAll(nextIteration.get(name));
        return !grown;
    }

    /**
     * After the loop, each tracked object is in a state it may be in where the loop exits. With no exit state (the
     * solver proves the loop never exits, or its states were not followed), the object is havocked instead: an object
     * in no state would make every check after the loop pass, which must not depend on that proof
     */
    void finishLoop() {
        exit.forEach((name, states) -> {
            if (states.isEmpty())
                checker.forgetChanges(name, new Predicate(), loop);
            else
                setStates(name, states);
        });
    }

    private Map<String, Set<State>> emptyStates() {
        Map<String, Set<State>> states = new LinkedHashMap<>();
        tracked().forEach(name -> states.put(name, new LinkedHashSet<>()));
        return states;
    }

    private void addCurrentStates(Map<String, Set<State>> states) throws LJError {
        for (String name : tracked())
            states.get(name).addAll(currentStates(name));
    }

    private Set<State> currentStates(String name) throws LJError {
        Optional<VariableInstance> instance = checker.getContext().getLastVariableInstance(name);
        String current = instance.map(RefinedVariable::getName).orElse(name);
        return new StateSolver(checker, loop.getPosition()).possibleStates(classStates.get(name), new Predicate(),
                current);
    }

    /** Gives the object a new instance, known only to be in one of {@code states} */
    private void setStates(String name, Set<State> states) {
        Predicate inAnyState = states.stream().map(State::predicate).reduce(Predicate::createDisjunction)
                .orElseGet(() -> Predicate.createLit("false", Types.BOOLEAN));
        checker.forgetChanges(name, inAnyState, loop);
    }
}
