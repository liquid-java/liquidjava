package testSuite.classes.throwable_subclass_more_than_super_error;

import java.io.IOException;

// the implicit super() leaves no cause, but the constructor then sets one
public class CauseAfterImplicitSuper extends IOException {
    public CauseAfterImplicitSuper(Throwable cause) {
        initCause(cause);
    }

    static CauseAfterImplicitSuper wrap(Throwable cause, Throwable other) {
        CauseAfterImplicitSuper e = new CauseAfterImplicitSuper(cause);
        e.initCause(other); // Expect: State Refinement Error
        return e;
    }
}
