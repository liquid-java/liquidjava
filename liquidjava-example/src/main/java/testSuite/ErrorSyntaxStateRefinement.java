// Syntax Error
package testSuite;

import liquidjava.specification.StateRefinement;

public class ErrorSyntaxStateRefinement {
    
    @StateRefinement(from="$", to="#")
    void test() {}
}
