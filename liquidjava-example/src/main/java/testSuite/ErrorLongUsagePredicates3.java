// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

public class ErrorLongUsagePredicates3 {
    void testWrongSign() {                                                                                                                                
      @Refinement("v < 0")                                                                                                                              
      long v = 42L;                                                                                                                                     
    }
}