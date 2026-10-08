package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"fresh", "used"})
class CorrectReceiverToken {
    @StateRefinement(to = "fresh(this)")
    public CorrectReceiverToken() {}

    @StateRefinement(to = "used(this)")
    public CorrectReceiverToken(int spent) {}

    @StateRefinement(from = "fresh(this)", to = "used(this)")
    public void use() {}

    @StateRefinement(from = "used(this)")
    public void report() {}
}

public class CorrectConstructorReceiver {
    static void chained() {
        new CorrectReceiverToken().use();
        new CorrectReceiverToken(1).report();
    }
}
