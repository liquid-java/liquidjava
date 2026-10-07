package testSuite.classes.try_catch_correct;

import java.io.StringReader;

import liquidjava.specification.Refinement;

public class Test {
    static void load() throws Exception {
        throw new Exception("x");
    }

    static void sameValueInBothBranches() {
        int y = 1;
        try {
            load();
            y = 5;
        } catch (Exception e) {
            y = 7;
        }
        @Refinement("_ > 0")
        int z = y;
    }

    static void catchDoesNotComplete() {
        Throwable t = new Throwable("start");
        try {
            load();
        } catch (Exception e) {
            t = new Throwable("catch", e);
            return;
        }
        t.initCause(new RuntimeException());
    }

    static void multipleCatches() {
        int y = 1;
        try {
            load();
            y = 2;
        } catch (RuntimeException e) {
            y = 3;
        } catch (Exception e) {
            y = 4;
        }
        @Refinement("_ > 0")
        int z = y;
    }

    static void finallyOverridesBranches() {
        int y = 1;
        try {
            load();
            y = -2;
        } catch (Exception e) {
            y = -3;
        } finally {
            y = 4;
        }
        @Refinement("_ > 0")
        int z = y;
    }

    static void nestedTry() {
        int y = 1;
        try {
            try {
                load();
                y = 2;
            } catch (RuntimeException e) {
                y = 3;
            }
        } catch (Exception e) {
            y = 4;
        }
        @Refinement("_ > 0")
        int z = y;
    }

    static void tryWithResources() throws Exception {
        int y = 1;
        try (StringReader r = new StringReader("x")) {
            y = 2;
        } catch (RuntimeException e) {
            y = 3;
        }
        @Refinement("_ > 0")
        int z = y;
    }

    static void unchangedInTry() {
        Throwable t = new Throwable("start");
        try {
            load();
        } catch (Exception e) {
            t.initCause(e);
        }
    }

    // every state t has in the try allows initCause
    static void everyStateInTryAllowsCall() {
        Throwable t = new Throwable("start");
        try {
            t = new Throwable("try");
            load();
        } catch (Exception e) {
            t.initCause(e);
        }
    }

    // the declared refinement of y holds for every value it has in the try
    static void everyValueInTryPositive() {
        @Refinement("_ > 0")
        int y = 1;
        try {
            y = 2;
            load();
        } catch (Exception e) {
            @Refinement("_ > 0")
            int z = y;
        }
    }

    // every path into the finally leaves y positive
    static void finallyFromEveryPath() {
        int y = 1;
        try {
            y = 2;
            load();
            return;
        } catch (Exception e) {
            y = 3;
        } finally {
            @Refinement("_ > 0")
            int z = y;
        }
    }

    // after the finally, only the states where the try statement completed normally remain
    static void afterTryFinally() {
        int y = 0;
        try {
            y = 5;
        } finally {
        }
        @Refinement("_ > 0")
        int z = y;
    }

    static void afterTryCatchFinally() {
        int y = 0;
        try {
            load();
            y = 5;
        } catch (Exception e) {
            y = 6;
        } finally {
        }
        @Refinement("_ > 0")
        int z = y;
    }

    // what the finally assigns holds after it
    static void finallyAssignmentKept() {
        int y = 0;
        try {
            load();
        } catch (Exception e) {
            y = -1;
        } finally {
            y = 1;
        }
        @Refinement("_ > 0")
        int z = y;
    }
}
