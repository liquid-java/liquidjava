package testSuite.classes.throwable_subclass_correct;

// A project exception whose constructor sets no cause (Throwable(String)), so one initCause is allowed.
public class AppException extends RuntimeException {
    public AppException(String message) {
        super(message);
    }

    static AppException wrap(Exception underlying) {
        AppException e = new AppException("operation failed");
        e.initCause(underlying);
        return e;
    }
}
