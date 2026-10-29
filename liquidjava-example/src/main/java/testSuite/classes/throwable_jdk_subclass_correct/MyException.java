package testSuite.classes.throwable_jdk_subclass_correct;

import java.io.IOException;

// super(message) runs IOException(String), Exception(String) and then Throwable(String): no cause.
public class MyException extends IOException {
    public MyException(String message) {
        super(message);
    }
}
