package testSuite;

import liquidjava.specification.Ghost;
import liquidjava.specification.StateRefinement;

@Ghost("int n")
public class CorrectDotNotationIncrementOnce {

    // explicit this
    @StateRefinement(to="this.n() == 0")
    public CorrectDotNotationIncrementOnce() {}

    // implicit this
    @StateRefinement(from="n() == 0", to="n() == old(this).n() + 1")
    public void incrementOnce() {}

    public static void main(String[] args) {
        CorrectDotNotationIncrementOnce t = new CorrectDotNotationIncrementOnce();
        t.incrementOnce();
    }
}
