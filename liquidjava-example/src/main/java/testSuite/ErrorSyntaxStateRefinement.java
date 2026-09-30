package testSuite;

import liquidjava.specification.StateRefinement;

public class ErrorSyntaxStateRefinement {
    
    @StateRefinement(from="$", to="#") // Expect: Syntax Error
    void test() {}
}
