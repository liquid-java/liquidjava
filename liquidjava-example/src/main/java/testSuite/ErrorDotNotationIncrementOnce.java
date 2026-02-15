// State Refinement Error
package testSuite;

import liquidjava.specification.Ghost;
import liquidjava.specification.StateRefinement;

@Ghost("int n")
public class ErrorDotNotationIncrementOnce {

    @StateRefinement(to="this.n() == 0")
    public ErrorDotNotationIncrementOnce() {}

    @StateRefinement(from="n() == 0", to="n() == old(this).n() + 1")
    public void incrementOnce() {}

    public static void main(String[] args) {
        ErrorDotNotationIncrementOnce t = new ErrorDotNotationIncrementOnce();
        t.incrementOnce();
        t.incrementOnce(); // error
    }
}
