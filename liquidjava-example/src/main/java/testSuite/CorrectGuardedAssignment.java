package testSuite;

import liquidjava.specification.Refinement;

// A guard on a variable still describes its old value after the variable is assigned (#538)
@SuppressWarnings("unused")
public class CorrectGuardedAssignment {
    static int read() {
        return 1;
    }

    static void use(@Refinement("_ >= 5") int x) {}

    static void guarded() {
        @Refinement("_ <= 10")
        int t = 0;
        int n = read();
        if (n <= 10 - t) {
            t = t + n;
        }
    }

    static void earlyExit() {
        @Refinement("_ <= 10")
        int t = 0;
        int n = read();
        if (n > 10 - t)
            return;
        t = t + n;
    }

    // the else branch starts from the value the condition was checked on
    static void elseAfterAssignment() {
        int t = read();
        if (t < 5) {
            t = 100;
        } else {
            use(t);
        }
    }
}
