package testSuite;

import liquidjava.specification.StateRefinement;

public class ErrorSyntaxStateRefinement {
    
    @StateRefinement(from="$", to="#") // Syntax Error
    void test() {}
}
