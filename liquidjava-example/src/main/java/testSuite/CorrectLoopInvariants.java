package testSuite;

import liquidjava.specification.Refinement;

// Facts about the int locals a loop changes that hold on every iteration are inferred (#537)
@SuppressWarnings("unused")
public class CorrectLoopInvariants {
    static boolean next() {
        return true;
    }

    static int read(byte[] buffer, @Refinement("_ >= 0") int offset, int length) {
        return 0;
    }

    static void use(@Refinement("_ >= 0") int x) {}

    static void counter() {
        for (int i = 0; i < 10; i++) {
            use(i);
        }
    }

    static void accumulator() {
        int total = 0;
        while (total < 1024) {
            use(total);
            total = total + 1;
        }
    }

    static void fill(byte[] buffer, int limit) {
        int total = 0;
        while (total < limit) {
            int n = read(buffer, total, limit - total);
            if (n < 0)
                break;
            total += n;
        }
    }

    static void symbolicStart(int k) {
        if (k < 0)
            return;
        for (int i = k; i < 100; i++) {
            use(i);
        }
    }

    static void nested() {
        for (int i = 0; i < 10; i++) {
            for (int j = i; j < 10; j++) {
                use(i);
                use(j);
            }
        }
    }

    // a continue goes on to the update
    static void continueInFor() {
        for (int i = 0; i < 10; i++) {
            if (next())
                continue;
            use(i);
        }
    }

    static void continueInWhile() {
        int i = 0;
        while (i < 10) {
            use(i);
            if (next()) {
                i++;
                continue;
            }
            i = i + 2;
        }
    }

    static void continueInDoWhile() {
        int i = 0;
        do {
            use(i);
            if (next()) {
                i++;
                continue;
            }
            i = i + 2;
        } while (i < 10);
    }

    static boolean ten(@Refinement("_ == 10") int x) {
        return true;
    }

    // the continue goes to the inner loop, so the condition still knows the value the body gave i
    static void continueOfInnerLoop(int n) {
        int i = 0;
        do {
            i = 10;
            for (int j = 0; j < n; j++) {
                if (next())
                    continue;
            }
        } while (ten(i) && next());
    }
}
