package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class CorrectLongUsage {

    @Refinement("_ > 10")
    public static long doubleBiggerThanTen(@Refinement("a > 10") int a) {
        return a * 2;
    }

    @Refinement("_ > 40")
    public static long doubleBiggerThanTwenty(@Refinement("a > 20") long a) {
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

        @Refinement("d > 10")
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
