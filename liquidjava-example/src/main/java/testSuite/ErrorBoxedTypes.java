package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorBoxedTypes {
    public static void errorBoxedBoolean() {
        @Refinement("_ == true")
        Boolean b = false; // Refinement Error
    }

    public static void errorBoxedInteger() {
        @Refinement("_ > 0")
        Integer j = -1; // Refinement Error
    }

    public static void errorBoxedDouble() {
        @Refinement("_ > 0")
        Double d = -1.0; // Refinement Error
    }
}
