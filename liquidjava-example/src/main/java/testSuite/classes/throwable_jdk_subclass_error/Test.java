package testSuite.classes.throwable_jdk_subclass_error;

import java.util.MissingResourceException;

public class Test {
    // ClassNotFoundException() runs Throwable(Throwable) with a null cause, so initCause throws
    // IllegalStateException("Can't overwrite cause") (scijava-common CommandInfo, fixed in a6fd9cf)
    static ClassNotFoundException notFound(Throwable exc) throws ClassNotFoundException {
        final ClassNotFoundException cnfe = new ClassNotFoundException();
        cnfe.initCause(exc); // Expect: State Refinement Error
        throw cnfe;
    }

    static RuntimeException causeTwice(Throwable cause, Throwable other) {
        RuntimeException e = new RuntimeException("failed", cause);
        e.initCause(other); // Expect: State Refinement Error
        return e;
    }

    // super(message) runs ClassNotFoundException(String), which passes a null cause on to Throwable(String, Throwable)
    static NotFound notFoundSubclass(Throwable exc) {
        NotFound e = new NotFound("missing");
        e.initCause(exc); // Expect: State Refinement Error
        return e;
    }

    // MissingResourceException(String, String, String) also writes fields after super(message), so it is not known to
    // delegate and gets the default state, in which initCause cannot be shown to be allowed (#367)
    static MissingResourceException notDelegating(Throwable exc) {
        MissingResourceException e = new MissingResourceException("missing", "Bundle", "key");
        e.initCause(exc); // Expect: State Refinement Error
        return e;
    }
}
