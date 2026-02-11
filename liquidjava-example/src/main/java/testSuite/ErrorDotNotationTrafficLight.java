// State Refinement Error
package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"green", "amber", "red"})
public class ErrorDotNotationTrafficLight {

    @StateRefinement(to="this.green()")
    public ErrorDotNotationTrafficLight() {}

    @StateRefinement(from="this.green()", to="this.amber()")
    public void transitionToAmber() {}

    @StateRefinement(from="red()", to="green()")
    public void transitionToGreen() {}

    @StateRefinement(from="this.amber()", to="red()")
    public void transitionToRed() {}

    public static void main(String[] args) {
        ErrorDotNotationTrafficLight tl = new ErrorDotNotationTrafficLight();
        tl.transitionToAmber();
        tl.transitionToGreen(); // error
        tl.transitionToRed();
    }
}
