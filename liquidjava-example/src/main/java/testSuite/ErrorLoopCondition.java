package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorLoopCondition {

    @Refinement("_ >= -1")
    static int read() {
        return -1;
    }

    static void write(@Refinement("_ > 0") int n) {}

    static void lessThanTen(@Refinement("_ < 10") int n) {}

    // the condition was checked on the old value of n, not on the re-assigned one
    static void useAfterReassignment() {
        int n = read();
        while (n > 0) {
            n = read();
            write(n); // Expect: Refinement Error
        }
    }

    // the condition does not imply the requirement
    static void weakerCondition() {
        int n = read();
        while (n >= 0) {
            write(n); // Expect: Refinement Error
            n = read();
        }
    }

    // the body must see i, not the incremented value i + 1 (which would be > 0)
    static void updateNotBeforeBody() {
        for (@Refinement("_ >= 0") int i = 0; i < 10; i++) {
            write(i); // Expect: Refinement Error
        }
    }

    // a do-while body runs once before the condition is checked
    static void doWhileFirstIteration() {
        int n = read();
        do {
            write(n); // Expect: Refinement Error
            n = read();
        } while (n > 0);
    }

    // i++ in the body invalidates the condition on i
    static void incrementInBody() {
        int i = 0;
        while (i < 10) {
            i++;
            lessThanTen(i); // Expect: Refinement Error
        }
    }

    // later iterations: k is decremented while the condition only re-checks m
    static void laterIteration() {
        int k = read();
        int m = k;
        while (m > 0) {
            write(k); // Expect: Refinement Error
            k = k - 1;
        }
    }

    // the loop may run zero times or exit through the break: facts from its body do not hold after it
    static void factsAfterLoop(int p, boolean c) {
        int n = p;
        while (c) {
            if (n <= 0) {
                break;
            }
        }
        write(n); // Expect: Refinement Error
    }
}
