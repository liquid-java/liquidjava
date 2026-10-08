package testSuite;

import liquidjava.specification.Refinement;

public interface CorrectInterfaceMethodBodies {
    static int size(String s) {
        int n = s.length();
        return n;
    }

    @Refinement("_ > 0")
    static int positive() {
        return 1;
    }

    static void checkStaticContract() {
        @Refinement("_ > 0")
        int n = positive();
    }

    @Refinement("_ > 0")
    default int defaultPositive() {
        return 1;
    }

    default CorrectInterfaceMethodBodies self() {
        return this;
    }

    @Refinement("_ > 0")
    default int lambdaReturnIsNotMethodReturn() {
        java.util.function.IntSupplier supplier = () -> { return -1; };
        return 1;
    }

    java.util.function.IntSupplier FIELD_SUPPLIER = () -> { return -1; };

    @Refinement("_ > 0")
    static int overloaded(int value) { return 1; }

    @Refinement("_ < 0")
    static int overloaded(boolean value) { return -1; }
}
