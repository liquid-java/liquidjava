package testSuite.classes.delegated_argument_reassigned_error;

import java.util.function.IntFunction;
import javax.print.attribute.standard.PrinterStateReasons;

// PrinterStateReasons(int) runs HashMap(int): the capacity is the argument's value at the call, not a later one
public class Test {
    static void assignedBefore(int n) {
        n = 5;
        PrinterStateReasons r = new PrinterStateReasons(n);
        r.get("k");
    }

    static void assignedAfter(int n) {
        PrinterStateReasons r = new PrinterStateReasons(n);
        n = 5;
        r.get("k"); // Expect: State Refinement Error
    }

    static void assignedBeforeAndAfter(int n) {
        n = 6;
        PrinterStateReasons r = new PrinterStateReasons(n);
        n = 5;
        r.get("k"); // Expect: State Refinement Error
    }

    static IntFunction<PrinterStateReasons> lambdaParameter() {
        return k -> {
            PrinterStateReasons r = new PrinterStateReasons(k);
            r.get("k"); // Expect: State Refinement Error
            return r;
        };
    }
}
