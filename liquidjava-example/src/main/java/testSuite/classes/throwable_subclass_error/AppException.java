package testSuite.classes.throwable_subclass_error;

// A project exception: its constructor passes the cause up through RuntimeException to Throwable(String, Throwable),
// so the object already has a cause and initCause throws IllegalStateException("Can't overwrite cause").
public class AppException extends RuntimeException {
    public AppException(String message, Throwable cause) {
        super(message, cause);
    }

    static AppException wrap(Exception underlying, Exception detail) {
        AppException e = new AppException("operation failed", underlying);
        e.initCause(detail); // Expect: State Refinement Error
        return e;
    }
}
