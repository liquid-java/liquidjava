package testSuite;

import java.time.DayOfWeek;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
class ErrorEnumConstantInCondition {
    enum Format {
        JPG, PNG, GIF
    }

    static class Limits {
        static final int max = 10; // lowercase until #307 is fixed
    }

    static void onlyJpg(@Refinement("f == Format.JPG") Format f) {}

    static void onlyPng(@Refinement("f == Format.PNG") Format f) {}

    static void thenBranch(Format format) {
        if (format == Format.JPG) {
            onlyPng(format); // Expect: Refinement Error
        }
    }

    // the else-branch is reachable: the condition is not the constant true
    static void elseBranch(Format format) {
        if (format == Format.JPG) {
            onlyJpg(format);
        } else {
            onlyJpg(format); // Expect: Refinement Error
        }
    }

    static void notEquals(Format format) {
        if (format != Format.JPG) {
            onlyJpg(format); // Expect: Refinement Error
        }
    }

    static void or(Format format) {
        if (format == Format.JPG || format == Format.PNG) {
            onlyJpg(format); // Expect: Refinement Error
        }
    }

    static void elseIfChain(Format format) {
        if (format == Format.JPG) {
        } else if (format == Format.PNG) {
        } else {
            onlyPng(format); // Expect: Refinement Error
        }
    }

    // null comparisons carry no information: format may still be anything
    static void nullOrConstant(Format format) {
        if (format == null || format == Format.JPG) {
            onlyJpg(format); // Expect: Refinement Error
        }
    }

    // two different constants are never equal, so the else-branch is always taken
    static void twoConstants(int n) {
        if (Format.JPG == Format.PNG) {
            n = 1;
        } else {
            @Refinement("_ > 0")
            int m = n; // Expect: Refinement Error
        }
    }

    static void staticFinalOfUserClass(int n) {
        if (n == Limits.max) {
            @Refinement("_ == 11")
            int m = n; // Expect: Refinement Error
        }
    }

    static void arithmetic(int n) {
        if (n + Limits.max > 15) {
            @Refinement("_ > 6")
            int m = n; // Expect: Refinement Error
        }
    }

    // a constant that cannot be represented is unknown, so the else-branch is still checked
    static void jdkEnumElse(DayOfWeek day, int n) {
        if (day == DayOfWeek.SUNDAY) {
            n = 1;
        } else {
            @Refinement("_ > 0")
            int m = n; // Expect: Refinement Error
        }
    }
}
