package testSuite.classes.throwable_jdk_subclass_correct;

// JDK exceptions whose constructors only pass their arguments up to Throwable(String) or Throwable() are created
// without a cause, so one initCause is allowed.
public class Test {
    static RuntimeException wrap(Throwable t) {
        RuntimeException e = new RuntimeException("failed");
        e.initCause(t);
        return e;
    }

    static IllegalStateException illegalState(Throwable t) {
        IllegalStateException e = new IllegalStateException("failed");
        e.initCause(t);
        return e;
    }

    static Exception noMessage(Throwable t) {
        Exception e = new Exception();
        e.initCause(t);
        return e;
    }

    static MyException mine(Throwable t) {
        MyException e = new MyException("failed");
        e.initCause(t);
        return e;
    }
}
