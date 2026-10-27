package testSuite.classes.throwable_subclass_more_than_super_error;

// an instance initializer runs right after super(message) and sets the cause
public class CauseInInitializer extends RuntimeException {
    {
        initCause(null);
    }

    public CauseInInitializer(String message) {
        super(message);
    }

    static CauseInInitializer wrap(Throwable other) {
        CauseInInitializer e = new CauseInInitializer("failed");
        e.initCause(other); // Expect: State Refinement Error
        return e;
    }
}
