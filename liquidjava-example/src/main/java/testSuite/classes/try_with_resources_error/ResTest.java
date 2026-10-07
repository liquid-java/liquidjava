package testSuite.classes.try_with_resources_error;

public class ResTest {
    // the implicit close() at the end of the block is a second close()
    static void closeTwice() {
        try (Res r = new Res()) { // Expect: State Refinement Error
            r.read();
            r.close();
        }
    }

    // the resource is closed after the block
    static void useAfter() {
        Res r = new Res();
        try (r) {
            r.read();
        }
        r.read(); // Expect: State Refinement Error
    }

    // resources are closed before the catch block runs
    static void useInCatch() {
        Res r = new Res();
        try (r) {
            r.read();
        } catch (RuntimeException e) {
            r.read(); // Expect: State Refinement Error
        }
    }

    // the implicit close() of an already closed resource reference
    static void closedBefore() {
        Res r = new Res();
        r.close();
        try (r) { // Expect: State Refinement Error
            r.hashCode();
        }
    }
}
