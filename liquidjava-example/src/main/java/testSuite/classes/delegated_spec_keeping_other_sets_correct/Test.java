package testSuite.classes.delegated_spec_keeping_other_sets_correct;

import javax.print.attribute.standard.PrinterStateReasons;

// HashMap(int)'s spec leaves the b set unchanged, which refers to the old object: a new object has none, so the
// delegated call gets the default state (b1) instead of that spec
public class Test {
    static void get() {
        PrinterStateReasons r = new PrinterStateReasons(5);
        r.get("k");
    }
}
