package testSuite;

import liquidjava.specification.Ghost;
import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;

@Ghost("int size")
public class ErrorDotNotationMultiple {

    @StateRefinement(to = "size() == 0")
    public ErrorDotNotationMultiple() {
    }

    public static void main(String[] args) {
        @Refinement("_ == this.not.size()") // Syntax Error
        int x = 0;
    }
}
