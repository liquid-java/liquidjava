package testSuite.classes.catch_parameter_error;

public class Test {
    static void load() throws Exception {
        throw new Exception("x");
    }

    // the state of a catch parameter is unknown
    static void unknownState(Throwable t) {
        try {
            load();
        } catch (Throwable x) {
            x.initCause(t); // Expect: State Refinement Error
        }
    }

    // the catch parameter must not take the state of an earlier local with the same name
    static void sameNameAsEarlierLocal() {
        try {
            load();
        } catch (Exception ex) {
            Throwable e = new Throwable("wrapped");
        }
        try {
            load();
        } catch (Throwable e) {
            e.initCause(new RuntimeException("more")); // Expect: State Refinement Error
        }
    }

    // the catch parameter can be used outside the catch through another variable
    static void usedAfterCatch() {
        Throwable failure = new Throwable("none");
        try {
            load();
        } catch (Throwable e) {
            failure = e;
        }
        failure.initCause(new RuntimeException("root")); // Expect: State Refinement Error
    }
}
