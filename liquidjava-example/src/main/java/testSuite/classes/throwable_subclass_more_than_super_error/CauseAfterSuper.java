package testSuite.classes.throwable_subclass_more_than_super_error;

// super(message) leaves no cause, but the constructor then sets one, so it gets the default state, not
// Throwable(String)'s, and a second initCause is reported
public class CauseAfterSuper extends RuntimeException {
    public CauseAfterSuper(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }

    static CauseAfterSuper wrap(Throwable cause, Throwable other) {
        CauseAfterSuper e = new CauseAfterSuper("failed", cause);
        e.initCause(other); // Expect: State Refinement Error
        return e;
    }
}
