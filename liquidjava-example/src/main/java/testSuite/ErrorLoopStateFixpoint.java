package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// The states of an object at the start of every iteration of a loop are those before the loop and those at the end of
// any iteration, computed until they stop changing
@StateSet({"first", "second", "third"})
public class ErrorLoopStateFixpoint {
    @StateRefinement(to = "first(this)")
    ErrorLoopStateFixpoint() {}

    @StateRefinement(from = "first(this)", to = "second(this)")
    @StateRefinement(from = "second(this)", to = "third(this)")
    @StateRefinement(from = "third(this)")
    void step() {}

    @StateRefinement(from = "first(this)")
    @StateRefinement(from = "second(this)")
    void early() {}

    @StateRefinement(from = "third(this)")
    void last() {}

    // third is reached in the third iteration only, after the states grow twice
    static void thirdIteration(int n) {
        ErrorLoopStateFixpoint o = new ErrorLoopStateFixpoint();
        for (int i = 0; i < n; i++) {
            o.early(); // Expect: State Refinement Error
            o.step();
        }
    }

    // after two steps in the loop, o may still be first or second
    static void notEnoughIterations(int n) {
        ErrorLoopStateFixpoint o = new ErrorLoopStateFixpoint();
        while (n > 0) {
            o.step();
            o.step();
            n--;
        }
        o.last(); // Expect: State Refinement Error
    }

    // the error in the lambda is reported once, although the loop body is checked from more than one set of states
    static void errorInLambdaInLoop(int n) {
        ErrorLoopStateFixpoint o = new ErrorLoopStateFixpoint();
        for (int i = 0; i < n; i++) {
            o.step();
            Runnable r = () -> o.last(); // Expect: State Refinement Error
        }
    }
}
