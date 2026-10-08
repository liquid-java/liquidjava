package testSuite.classes.try_catch_error;

import liquidjava.specification.Refinement;

public class Test {
    static void load() throws Exception {
        throw new Exception("x");
    }

    // the try may complete normally, so t may be withThrowable
    static void stateOfTryPathKept() {
        Throwable t = new Throwable("start");
        try {
            load();
            t = new Throwable("try", new RuntimeException());
        } catch (Exception e) {
            t = new Throwable("catch");
        }
        t.initCause(new RuntimeException()); // Expect: State Refinement Error
    }

    // the catch may not run, so y may be negative
    static void valueOfTryPathKept() {
        int y = 0;
        try {
            load();
            y = -5;
        } catch (Exception e) {
            y = 7;
        }
        @Refinement("_ > 0")
        int z = y; // Expect: Refinement Error
    }

    // the catch may run, so y may be negative
    static void valueOfCatchPathKept() {
        int y = 0;
        try {
            load();
            y = 5;
        } catch (Exception e) {
            y = -5;
        }
        @Refinement("_ > 0")
        int z = y; // Expect: Refinement Error
    }

    // the exception may be thrown after t changed, so its state in the catch is unknown
    static void changedBeforeException() {
        Throwable t = new Throwable("start");
        try {
            t.initCause(new RuntimeException());
            load();
        } catch (Exception e) {
            t.initCause(new RuntimeException()); // Expect: State Refinement Error
        }
    }

    // only the second of several catches assigns a negative value
    static void multipleCatches() {
        int y = 1;
        try {
            load();
        } catch (RuntimeException e) {
            y = 3;
        } catch (Exception e) {
            y = -4;
        }
        @Refinement("_ > 0")
        int z = y; // Expect: Refinement Error
    }

    // a value assigned in a nested if of the try may reach the catch
    static void valueFromIfInTry(boolean b) {
        int y = 1;
        try {
            if (b)
                y = -1;
            load();
        } catch (Exception e) {
            @Refinement("_ > 0")
            int z = y; // Expect: Refinement Error
        }
    }

    // the finally also runs after a catch that returns
    static void finallyAfterReturningCatch() {
        Throwable t = new Throwable("start");
        try {
            load();
            return;
        } catch (Exception e) {
            t.initCause(e);
            return;
        } finally {
            t.initCause(new RuntimeException()); // Expect: State Refinement Error
        }
    }

    // the finally also runs when an exception leaves the try uncaught
    static void finallyAfterUncaughtException() throws Exception {
        Throwable t = new Throwable("start");
        try {
            t.initCause(new RuntimeException());
            load();
            t = new Throwable("again");
        } finally {
            t.initCause(new RuntimeException()); // Expect: State Refinement Error
        }
    }

    // the exception may be thrown before the check, so the catch cannot assume it
    static void pathConditionInCatch(int x) {
        try {
            load();
            if (x <= 0)
                return;
        } catch (Exception e) {
            @Refinement("_ > 0")
            int z = x; // Expect: Refinement Error
        }
    }

    // after the join the catch path may have run, so the check from the try does not hold
    static void pathConditionAfterTry(int x) {
        try {
            load();
            if (x <= 0)
                return;
        } catch (Exception e) {
        }
        @Refinement("_ > 0")
        int z = x; // Expect: Refinement Error
    }

    // the finally also runs on the return, so it cannot assume the check
    static void pathConditionInFinally(int x) {
        try {
            if (x <= 0)
                return;
        } finally {
            @Refinement("_ > 0")
            int z = x; // Expect: Refinement Error
        }
    }

    // the finally changes x, so the check from before the try does not hold after it
    static void finallyChangesCheckedVariable(int x) throws Exception {
        if (x <= 0)
            return;
        try {
            load();
        } finally {
            x = -1;
        }
        @Refinement("_ > 0")
        int z = x; // Expect: Refinement Error
    }

    // the finally changes x, so the check from the try does not hold after it
    static void finallyChangesVariableCheckedInTry(int x) {
        try {
            if (x <= 0)
                return;
        } finally {
            x = -1;
        }
        @Refinement("_ > 0")
        int z = x; // Expect: Refinement Error
    }

    static void drop(int i) throws StoreException {
        throw new StoreException("cannot drop " + i, new RuntimeException());
    }

    // a caught exception may already carry its cause (Apache Derby, DERBY-2472, fixed in 1870e8fa:
    // chaining with initCause threw "Can't overwrite cause")
    static void chainCaughtExceptions() throws StoreException {
        StoreException top = null;
        for (int i = 0; i < 2; i++) {
            try {
                drop(i);
            } catch (StoreException e) {
                e.initCause(top); // Expect: State Refinement Error
                top = e;
            }
        }
    }
}
