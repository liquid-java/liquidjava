package testSuite;

import liquidjava.specification.Refinement;
import liquidjava.specification.RefinementAlias;

@SuppressWarnings("unused")
@RefinementAlias("PtGrade(int x) { x >= 0 && x <= 20}")
public class CorrectAlias {

    public static void main(String[] args) {
        @Refinement("PtGrade(_) && _ >= 10")
        int positiveGrade = 15;

        @Refinement("_ > 5 && _ < 18")
        int nGrade = 10;
        @Refinement("PtGrade(_) && PtGrade(nGrade)")
        int positiveGrade2 = 15;
    }
}
