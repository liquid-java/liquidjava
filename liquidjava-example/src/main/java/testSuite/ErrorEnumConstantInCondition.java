package testSuite;

import java.time.DayOfWeek;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
class ErrorEnumConstantInCondition {
    enum Format {
        JPG, PNG, GIF
    }

    static class Limits {
        static final int max = 10; // lowercase: int fields get a generated @Ghost, which must be lowercase
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

    static void staticFinalOfUserClass(int n) {
        if (n == Limits.max) {
            @Refinement("_ == 11")
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
