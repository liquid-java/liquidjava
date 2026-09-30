package testSuite.math.errorMultiplyExact;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.Refinement;

@ExternalRefinementsFor("java.lang.Math")
public interface MathRefinements {

    @Refinement("_ == 3.141592653589793")
    public double PI = 0;

    @Refinement("_ == 2.7182818284590452354")
    public double E = 0;

    @Refinement("(arg0 > 0)?( _ == arg0):(_ == -arg0)")
    public int abs(int arg0);

    @Refinement("(arg0 > 0)?( _ == arg0):(_ == -arg0)")
    public int abs(long arg0); // Expect: Warning

    @Refinement("(arg0 > 0)?( _ == arg0):(_ == -arg0)")
    public int abs(float arg0); // Expect: Warning

    @Refinement("(arg0 > 0)?( _ == arg0):(_ == -arg0)")
    public int abs(double arg0); // Expect: Warning

    @Refinement(" _ == a+b")
    public int addExact(int a, int b);

    @Refinement(" _ == a+b")
    public long addExact(long a, long b);

    @Refinement(" _ == a-b")
    public int subtractExact(int a, int b);

    @Refinement(" _ == a*b")
    public int multiplyExact(int a, int b);

    @Refinement("_ == (-a)")
    public int negateExact(int a);

    @Refinement("_ == (a-1)")
    public int decrementExact(int a);

    @Refinement("_ == (a-1)")
    public int decrementExact(long a); // Expect: Warning

    @Refinement("_ == (a+1)")
    public int incrementExact(int a);

    @Refinement("_ == (a+1)")
    public int incrementExact(long a); // Expect: Warning

    @Refinement("(a > b)? (_ == a):(_ == b)")
    public int max(int a, int b);

    @Refinement("(a > b)? (_ == b):(_ == a)")
    public int min(int a, int b);

    @Refinement(" _ > 0.0 && _ < 1.0")
    public long random(long a, long b); // Expect: Warning

    @Refinement("((sig > 0)?(_ > 0):(_ < 0)) && (( _ == arg)||(_ == -arg))")
    public float copySign(float arg, float sig);
}
