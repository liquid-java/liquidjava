package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorBitwiseConstantArgument {
    static final int READ = 1;
    static final int WRITE = 2;

    static void open(@Refinement("_ == 1 || _ == 5") int mode) {
    }

    public static void main(String[] args) {
        open(READ | WRITE); // Expect: Refinement Error
    }
}
