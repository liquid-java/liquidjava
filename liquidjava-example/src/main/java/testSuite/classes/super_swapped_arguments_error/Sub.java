package testSuite.classes.super_swapped_arguments_error;

// The parameters of Sub are named like those of Base, in the other order: each Base parameter is replaced by the Sub
// parameter passed for it, all at once, so a name is not replaced twice
public class Sub extends Base {
    public Sub(boolean y, boolean x) {
        super(y, x);
    }

    static void test() {
        Sub s = new Sub(false, true); // Base(false, true): closed
        s.close(); // Expect: State Refinement Error
    }
}
