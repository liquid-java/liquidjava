package testSuite;

import liquidjava.specification.Refinement;

class CorrectVarargsLogger {
    void debug(@Refinement("_ >= 0") int level, String format, Object... args) {}
}

public class CorrectVarargsCall {
    static void log(CorrectVarargsLogger logger, Object a, Object b) {
        logger.debug(1, "{} and {}", a, b);
        logger.debug(2, "nothing");
        logger.debug(3, "{}", new Object[] {a});
    }
}
