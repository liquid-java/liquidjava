package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorExtraToken {
    
    void test() {
        @Refinement("true false") // Syntax Error
        int a = 1;
    }
}
