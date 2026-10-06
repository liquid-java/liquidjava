package testSuite;

import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@SuppressWarnings("unused")
@StateSet({"open", "closed"})
public class CorrectLoopCondition {

    int field;

    @StateRefinement(to = "open(this)")
    CorrectLoopCondition() {}

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

    static void open(@Refinement("_ >= 0 && _ <= 65535") int port) {}

    static void lessThanTen(@Refinement("_ < 10") int n) {}

    // the condition holds in the body, and again after each re-assignment once it is re-checked
    static void readLoop() {
        int n = read();
        while (n > 0) {
            write(n);
            n = read();
        }
    }

    // the loop variable keeps its declared refinement across iterations; the condition gives the upper bound
    static void scanFrom(int start) {
        if (start <= 0) {
            return;
        }
        for (@Refinement("_ > 0") int i = start; i < 65535; i++) {
            open(i);
        }
    }

    // the update runs after the body: the body sees i < 10, not i + 1
    static void updateAfterBody() {
        for (@Refinement("_ >= 0") int i = 0; i < 10; i++) {
            lessThanTen(i);
        }
    }

    // the condition is assumed on a variable the body does not modify
    static void unmodifiedVariable(int limit) {
        int count = 0;
        while (limit > 0 && count < 10) {
            write(limit);
            count = count + 1;
        }
    }

    // conditions of nested loops hold together in the inner body
    static void nestedLoops(int a, int b) {
        while (a > 0) {
            while (b > 0) {
                write(a);
                write(b);
                b = read();
            }
            a = read();
        }
    }

    // an if (...) break inside the body is a path condition for the rest of the body
    static void breakInBody() {
        while (true) {
            int n = read();
            if (n <= 0) {
                break;
            }
            write(n);
        }
    }

    // the declared refinement of a variable assigned in the loop holds after it
    static void valueAfterLoop(boolean c) {
        @Refinement("_ > 0") int n = 1;
        while (c) {
            n = 2;
        }
        write(n);
    }

    // a continue in the body, with an update that needs no fact from the body
    static void continueInFor(int p) {
        int n = p;
        for (int i = 0; i < 10; i++) {
            if (n <= 0) {
                continue;
            }
            write(n);
        }
    }

    // the condition on a field holds in the body
    void fieldCondition() {
        while (field > 0) {
            write(field);
            field = field - 1;
        }
    }

    // methods that keep the state of r can be called in the loop
    static void stateKeptInLoop(int k) {
        CorrectLoopCondition r = new CorrectLoopCondition();
        while (k > 0) {
            r.use();
            k = k - 1;
        }
        r.close();
    }

    // the condition checks the state of r on every iteration
    static void stateCheckedByCondition(CorrectLoopCondition r) {
        while (r.isOpen()) {
            r.use();
            r.close();
        }
    }
}
