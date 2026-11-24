package testSuite;

import liquidjava.specification.Ghost;
import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;

@Ghost("float value")
class FloatCounter {

    FloatCounter() {}

    @StateRefinement(to = "value(this) == value(old(this)) + 0.5")
    void increment() {}
}

public class CorrectFloatGhost {

    static void check(@Refinement("value(c) >= 0.0") FloatCounter c) {}

    public static void main(String[] args) {
        FloatCounter c = new FloatCounter();

        c.increment();
        check(c); // Expected: No Refinement Error
    }
}
