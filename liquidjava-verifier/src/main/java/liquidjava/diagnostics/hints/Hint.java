package liquidjava.diagnostics.hints;

import liquidjava.diagnostics.SourceReference;

/**
 * A suggestion on how to fix a diagnostic
 *
 * @param kind
 *            what the suggestion is about, so clients can show or filter hints separately
 * @param message
 *            the suggestion as shown to the user, with code marked as {@link liquidjava.diagnostics.Code} marks it
 * @param reference
 *            the place in the source the suggestion names, or null
 */
public record Hint(Kind kind, String message, SourceReference reference) {

    public Hint(Kind kind, String message) {
        this(kind, message, null);
    }

    public enum Kind {
        /** A suggestion that does not come from a specific analysis */
        GENERAL,
        /** An earlier call whose arguments decided the state, and values that would allow the failing call */
        DECIDING_ARGUMENT,
        /** Calls to add before the failing call so the object reaches a state that allows it */
        CALL_SEQUENCE,
        /** The calls allowed in the object's current state */
        AVAILABLE_CALLS,
        /** No sequence of calls brings the object to a state that allows the failing call */
        UNREACHABLE_STATE,
        /** The object's state depends on the path taken, and no sequence of calls works for all of them */
        PATH_DEPENDENT_STATE,
        /** The object is in a state where no call with a state refinement is allowed */
        FINAL_STATE,
        /** The object can be in any state: the state the failing call needs, and the constructors that create it */
        NEEDED_STATE,
        /** A call whose result, checked right before the failing call, puts the object in a state that allows it */
        GUARD
    }

    public static Hint general(String message) {
        return new Hint(Kind.GENERAL, message);
    }
}
