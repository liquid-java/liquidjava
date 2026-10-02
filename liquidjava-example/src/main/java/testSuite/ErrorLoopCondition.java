package testSuite;

import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@SuppressWarnings("unused")
@StateSet({"open", "closed"})
public class ErrorLoopCondition {

    int field;

    @StateRefinement(to = "open(this)")
    ErrorLoopCondition() {}

    @StateRefinement(from = "open(this)", to = "closed(this)")
    void close() {}

    @StateRefinement(from = "open(this)")
    void use() {}

    @StateRefinement(to = "return ? open(this) : closed(this)")
    boolean isOpen() {
        return true;
    }

    void setFieldNegative() {
        field = -1;
    }

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

    // x starts at 3, but on its third iteration x is 1 and y becomes 0.
    // Checking only the first iteration accepts the false refinement on y.
    static void laterIterationFromKnownStart() {
        int x = 3;
        while (x > 0) {
            @Refinement("_ > 0") int y = x - 1; // Expect: Refinement Error
            x--;
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

    // the loop may run zero times: a value assigned in it is not the value after it
    static void valueAfterLoop(boolean c) {
        int n = -1;
        while (c) {
            n = 1;
        }
        write(n); // Expect: Refinement Error
    }

    // a continue skips the rest of the body, so the update cannot rely on its facts
    static void continueBeforeUpdate(int p) {
        int n = p;
        for (int i = 0; i < 10; write(n)) { // Expect: Refinement Error
            if (n <= 0) {
                continue;
            }
            i++;
        }
    }

    // a call in the body writes the field, which later iterations use
    void fieldWrittenByCall(int p) {
        field = p;
        int k = p;
        while (k > 0) {
            write(field); // Expect: Refinement Error
            setFieldNegative();
        }
    }

    // the loop changes the state of r: the second iteration uses it closed
    static void stateChangedInLoop(ErrorLoopCondition r) {
        boolean open = r.isOpen();
        while (open) {
            r.use(); // Expect: State Refinement Error
            r.close();
        }
    }

    // the inner loop also runs in later iterations of the do-while, after n = 5
    static void innerLoopInDoWhile(boolean c) {
        int n = 0;
        do {
            while (n > 0) {
                write(n - 10); // Expect: Refinement Error
            }
            n = 5;
        } while (c);
    }
}
