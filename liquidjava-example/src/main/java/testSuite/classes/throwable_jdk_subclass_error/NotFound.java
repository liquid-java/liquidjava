package testSuite.classes.throwable_jdk_subclass_error;

public class NotFound extends ClassNotFoundException {
    public NotFound(String message) {
        super(message);
    }
}
