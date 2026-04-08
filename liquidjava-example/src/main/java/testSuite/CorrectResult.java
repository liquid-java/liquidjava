package testSuite;

import liquidjava.specification.Refinement;

public class CorrectResult {

    @Refinement("#result > 10")
    public int getLargeNumber() {
        return 15;
    }

    @Refinement("#result == (a + b)")
    public int sum(int a, int b) {
        return a + b;
    }
}