package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class CorrectBoxedTypes {
    public static void main(String[] args) {
        @Refinement("_ == true")
        Boolean b = true;

        @Refinement("_ > 0")
        Integer i = 1;

        @Refinement("_ > 0")
        Double d = 1.0;
    }
}
