package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorExtraToken {
    
    void test() {
        @Refinement("true false") // Expect: Syntax Error
        int a = 1;
    }
}
