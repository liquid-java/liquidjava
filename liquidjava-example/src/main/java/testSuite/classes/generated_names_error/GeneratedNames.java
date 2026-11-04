package testSuite.classes.generated_names_error;

import liquidjava.specification.Refinement;

// Each value the verifier introduces is named after what it is, not #fresh (#373)
public class GeneratedNames {

    static void positive(@Refinement("_ > 0") int x) {}

    @Refinement("_ > 0")
    static int negate(@Refinement("_ < 0") int x) {
        return -x;
    }

    static boolean ready(int a) {
        return a > 0;
    }

    static int read(int key) {
        return key;
    }

    static void ifCondition(int a) {
        if (ready(a))
            positive(a); // Expect: Refinement Error
    }

    static void loopCondition(int a) {
        while (ready(a))
            positive(a); // Expect: Refinement Error
    }

    static int ternaryCondition(int a) {
        return ready(a) ? negate(a) : 1; // Expect: Refinement Error
    }

    static void tryAndCatchBranches(int a) {
        try {
            a = read(a);
        } catch (IllegalStateException e) {
            a = 1;
        } catch (RuntimeException e) {
            positive(a); // Expect: Refinement Error
        }
    }

    static void callResult(int key) {
        positive(read(key) - 1); // Expect: Refinement Error
    }

    static void operationResult(String s) {
        boolean isNull = s == null;
        if (isNull)
            positive(0); // Expect: Refinement Error
    }

    static void newObject() {
        new Door().close(); // Expect: State Refinement Error
    }
}
