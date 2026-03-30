package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorSpecificValuesIf {

    public static void addZ(@Refinement("a > 0") int a) {
        @Refinement("_ > 0")
        int d = a;
        if (d > 5) {
            @Refinement("b > 5")
            int b = d;
        } else {
            @Refinement("_ <= 5")
            int c = d;
            d = 10;
            @Refinement("b > 10")
            int b = d; // Refinement Error
        }
    }

    public static void addZ2() {
        @Refinement("_ > 10")
        int a = 15;
        if (a > 14) {
            a = 12;
            @Refinement("_ < 11")
            int c = a; // Refinement Error
        }
    }
}
