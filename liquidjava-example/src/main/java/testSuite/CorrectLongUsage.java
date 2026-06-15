package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class CorrectLongUsage {

    // `a * 2` would be 32-bit int arithmetic (overflowing at a >= 2^30) BEFORE the widening to long. Widening
    // the operand to long first makes the multiply 64-bit and overflow-free. For int a > 10 the result is in
    // (20, 2*Integer.MAX_VALUE], hence > 10 and bounded by 4294967294.
    @Refinement("_ > 10 && _ <= 4294967294")
    public static long doubleBiggerThanTen(@Refinement("a > 10") int a) {
        long widened = a; // widen to 64-bit before multiplying, so a * 2 cannot overflow as 32-bit int
        return widened * 2;
    }

    // a * 2 overflows in 64-bit once a exceeds Long.MAX_VALUE / 2, so bound a to keep 2*a in range and > 40.
    @Refinement("_ > 40")
    public static long doubleBiggerThanTwenty(@Refinement("a > 20 && a <= 4611686018427387903") long a) {
        return a * 2;
    }

    public static void main(String[] args) {
        @Refinement("a > 5")
        long a = 9L;

        if (a > 5) {
            @Refinement("b > 50")
            long b = a * 10;

            @Refinement("c < 0")
            long c = -a;
        }

        // Carry doubleBiggerThanTen's upper bound on d so that d * 2 below provably stays > 20 and in range
        // for doubleBiggerThanTwenty's a <= 4611686018427387903 precondition (and cannot itself overflow).
        @Refinement("d > 10 && d <= 4294967294")
        long d = doubleBiggerThanTen(100);

        @Refinement("e > 10")
        long e = doubleBiggerThanTwenty(d * 2);

        @Refinement("_ > 10")
        long f = doubleBiggerThanTwenty(2 * 80);
    }


    void testSmallLong() {                                                                                                                                
        @Refinement("v > 0")                                                                                                                              
        long v = 42L;                                                                                                                                     
    }    

    void testDoublePrecisionBoundary() {                                                                                                                  
      @Refinement("v == 9007199254740993")                                                                                                              
      long v = 9007199254740993L;                                                                                                                       
    }

    void testLargeSubtraction() {                                                                                                                         
        @Refinement("v - 9007199254740992 == 1")                                                                                                          
        long v = 9007199254740993L;                                                                                                                       
    }  

    void testMaxValueModulo() {                                                                                                                           
        @Refinement("v % 1000 == 807")                                                                                                                    
        long v = 9223372036854775807L;                                                                                                                    
    }   

    void testUUID(){
        @Refinement("((v/4096) % 16) == 1")
        long v = 0x01000000122341666L;
    }
    
}
