package testSuite;

import liquidjava.specification.Refinement;

class ErrorVarargsLogger {
    void debug(@Refinement("_ >= 0") int level, String format, Object... args) {}
}

public class ErrorVarargsCall {
    static void log(ErrorVarargsLogger logger, Object a, Object b) {
        logger.debug(-1, "{} and {}", a, b); // Expect: Refinement Error
    }
}
