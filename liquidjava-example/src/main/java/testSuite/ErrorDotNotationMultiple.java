// Syntax Error
package testSuite;

import liquidjava.specification.Ghost;
import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;

@Ghost("int size")
public class ErrorDotNotationMultiple {

    @StateRefinement(to="size() == 0")
    public ErrorDotNotationMultiple() {}

    void test() {
        @Refinement("_ == this.not.size()")
        int x = 0;
    }
}
