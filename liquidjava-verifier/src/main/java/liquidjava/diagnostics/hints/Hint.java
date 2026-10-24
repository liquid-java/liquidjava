package liquidjava.diagnostics.hints;

/**
 * A suggestion on how to fix a diagnostic
 *
 * @param kind
 *            what the suggestion is about, so clients can show or filter hints separately
 * @param message
 *            the suggestion as shown to the user
 */
public record Hint(Kind kind, String message) {

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
        /** The object's state is unknown, so these are the calls that lead to a state that allows the failing call */
        REACHING_CALLS
    }

    public static Hint general(String message) {
        return new Hint(Kind.GENERAL, message);
    }
}
