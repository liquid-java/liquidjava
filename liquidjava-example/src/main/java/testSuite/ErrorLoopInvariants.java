package testSuite;

import liquidjava.specification.Refinement;

// Only facts that hold before a loop and are kept by every iteration are inferred for its int locals (#537)
@SuppressWarnings("unused")
public class ErrorLoopInvariants {
    static boolean next() {
        return true;
    }

    static void use(@Refinement("_ >= 0") int x) {}

    static void atMostTen(@Refinement("_ <= 10") int x) {}

    static void decrementing() {
        for (int i = 10; i > -5; i--) {
            use(i); // Expect: Refinement Error
        }
    }

    static void resetInBody() {
        int i = 0;
        while (i < 10) {
            use(i); // Expect: Refinement Error
            i = i + 1;
            if (next())
                i = -1;
        }
    }

    static void decreasedOnSomePath() {
        int i = 0;
        while (i < 10) {
            use(i); // Expect: Refinement Error
            if (next())
                i = i - 2;
            else
                i++;
        }
    }

    // i < 10 does not give i <= 10 at the next iteration, as i grows by 3
    static void boundNotKept() {
        int i = 0;
        while (i < 10) {
            i = i + 3;
            atMostTen(i); // Expect: Refinement Error
        }
    }

    static void decreasedBeforeContinue() {
        int i = 0;
        while (i < 10) {
            use(i); // Expect: Refinement Error
            if (next()) {
                i = i - 1;
                continue;
            }
            i++;
        }
    }

    // the continue skips the use but not the update, which starts the next iteration from i - 5 + 1
    static void decreasedBeforeContinueInFor() {
        for (int i = 0; i < 10; i++) {
            if (next()) {
                i = i - 5;
                continue;
            }
            use(i); // Expect: Refinement Error
        }
    }

    static void decreasedInInnerLoop() {
        int i = 0;
        while (i < 10) {
            use(i); // Expect: Refinement Error
            for (int j = 0; j < 5; j++) {
                i = i - 1;
            }
            i++;
        }
    }
}
