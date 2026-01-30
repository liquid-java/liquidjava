// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

public class ErrorLongUsagePredicates1 {
    void testUUID(){
        @Refinement("((v/4096) % 16) == 2")
        long v = 0x01000000122341666L;
    }
}