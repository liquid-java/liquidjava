// State Refinement Error
package testSuite;

import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"green", "amber", "red"})
public class ErrorTrafficLightRGB {

    @Refinement("r >= 0 && r <= 255")
    int r;

    @Refinement("g >= 0 && g <= 255")
    int g;

    @Refinement("b >= 0 && b <= 255")
    int b;

    @StateRefinement(to = "green(this)")
    public ErrorTrafficLightRGB() {
        r = 255;
        g = 0;
        b = 0;
    }

    @StateRefinement(from = "green(this)", to = "amber(this)")
    public void transitionToAmber() {
        r = 255;
        g = 120;
        b = 0;
    }

    @StateRefinement(from = "red(this)", to = "green(this)")
    public void transitionToGreen() {
        r = 76;
        g = 187;
        b = 23;
    }

    @StateRefinement(from = "amber(this)", to = "red(this)")
    public void transitionToRed() {
        r = 230;
        g = 0;
        b = 1;
    }

    public static void name() {
        ErrorTrafficLightRGB tl = new ErrorTrafficLightRGB();
        tl.transitionToAmber();
        tl.transitionToRed();
        tl.transitionToAmber();
    }
}
