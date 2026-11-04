package liquidjava.processor.state_machine;

import java.util.Set;

import liquidjava.processor.context.RefinedFunction;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.UnaryExpression;

/**
 * A way to call a method whose state refinements depend on more than the state of this: with any arguments, with
 * arguments that meet a condition its refinements test, or followed by a check of its boolean result
 *
 * @param method
 *            the method called
 * @param kind
 *            what {@code condition} is about
 * @param condition
 *            the condition, in the names of the refinement: its parameter names, and {@code _} for the result
 * @param effects
 *            what each state refinement of the method does when the call meets {@code condition}, or null for a call of
 *            a method whose transitions the {@link StateMachine} has
 */
public record CallVariant(RefinedFunction method, Kind kind, Predicate condition, Effects effects) {

    public enum Kind {
        /** Any arguments and any result */
        ANY,
        /** Arguments that meet the condition */
        ARGUMENTS,
        /** A result that meets the condition, checked right after the call */
        RESULT
    }

    /** Whether the {@code from} of a state refinement holds in a state */
    public enum Entry {
        HOLDS, FAILS,
        /** It may or may not hold, or the solver could not tell */
        UNDECIDED
    }

    /**
     * What the state refinements of a method do in each state, in the order they are declared, for calls that meet a
     * variant's condition
     */
    public interface Effects {

        int refinements();

        /** Whether the state refinement at {@code index} has a {@code to} */
        boolean changesState(int index);

        Entry entry(int index, State state);

        /**
         * The states the object can be in after a call made in {@code state}, whose {@code from} must hold for the
         * refinement at {@code index}, or null if the solver could not tell
         */
        Set<State> after(int index, State state);
    }

    /**
     * Whether the condition is on the result and holds when the result is {@code false}, as in {@code !x.isClosed()}
     */
    public boolean negatedResult() {
        return kind == Kind.RESULT && condition.getExpression() instanceof UnaryExpression;
    }
}
