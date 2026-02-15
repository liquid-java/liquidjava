// Not Found Error
package testSuite;
import liquidjava.specification.Refinement;

public class ErrorResultVariable {
    public void test() {
        @Refinement("#result > 0") 
        int x = 10; 
    }
}