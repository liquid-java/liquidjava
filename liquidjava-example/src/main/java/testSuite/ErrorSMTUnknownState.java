package testSuite;

import liquidjava.specification.Ghost;
import liquidjava.specification.StateRefinement;

@Ghost("int amount")
public class ErrorSMTUnknownState {
    @StateRefinement(to = "amount(this) > 0")
    public ErrorSMTUnknownState() {}

    @StateRefinement(from = "amount(this) < limit")
    public void consume(int limit) {}

    public static void test(double limit) {
        ErrorSMTUnknownState value = new ErrorSMTUnknownState();
        int boundary = (int) limit;
        value.consume(boundary); // Expect: SMT Unknown Error
    }
}
