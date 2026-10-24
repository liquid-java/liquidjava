package liquidjava.processor.state_machine;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import liquidjava.diagnostics.errors.LJError;
import liquidjava.processor.refinement_checker.TypeChecker;
import liquidjava.rj_language.Predicate;
import liquidjava.smt.Counterexample;
import liquidjava.smt.SMTResult;
import liquidjava.utils.constants.Keys;
import spoon.reflect.cu.SourcePosition;

/**
 * Asks the SMT solver about states, in the current verification context
 */
public class StateSolver {

    private final TypeChecker tc;
    private final SourcePosition position;

    /**
     * @param position
     *            where the questions come from, reported if the solver fails
     */
    public StateSolver(TypeChecker tc, SourcePosition position) {
        this.tc = tc;
        this.position = position;
    }

    /** Whether {@code conclusion} holds whenever {@code premise} holds */
    public boolean implies(Predicate premise, Predicate conclusion) throws LJError {
        return tc.checkStateSMT(premise, conclusion, position, true);
    }

    /** Values for which {@code first} and {@code second} hold at the same time, if there are any */
    public Optional<Counterexample> witness(Predicate first, Predicate second) throws LJError {
        SMTResult result = tc.verifyStateSMT(first, second.negate(), position, true);
        return result.isOk() ? Optional.empty() : Optional.of(result.getCounterexample());
    }

    /** Whether {@code first} and {@code second} can hold at the same time */
    public boolean compatible(Predicate first, Predicate second) throws LJError {
        return !implies(first, second.negate());
    }

    /** The {@code states} that {@code variable} can be in when {@code known} holds, in the order given */
    public Set<State> possibleStates(List<State> states, Predicate known, String variable) throws LJError {
        Set<State> possible = new LinkedHashSet<>();
        for (State state : states)
            if (compatible(known, state.predicate().substituteVariable(Keys.THIS, variable)))
                possible.add(state);
        return possible;
    }
}
