// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorLongUsagePredicates2 {
      void testLargeSubtractionWrong() {                                                                                                                    
      @Refinement("v - 9007199254740992 == 2")                                                                                                          
      long v = 9007199254740993L;                                                                                                                       
  } 
}