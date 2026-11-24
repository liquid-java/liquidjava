package testSuite;

import liquidjava.specification.Ghost;
import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;

@Ghost("float value")
class NegativeFloatCounter {

    NegativeFloatCounter() {}

    @StateRefinement(to = "value(this) == value(old(this)) - 0.5")
    void decrement() {}
}

public class ErrorFloatGhost {

    static void check(@Refinement("value(c) >= 0.0") NegativeFloatCounter c) {}

    public static void main(String[] args) {
        NegativeFloatCounter c = new NegativeFloatCounter();

        c.decrement();
        check(c); // Expect: Refinement Error
    }
}
