package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"open", "closed"})
class SeparateResource {
    @StateRefinement(to = "open(this)")
    public SeparateResource() {}

    @StateRefinement(from = "open(this)")
    public void read() {}

    @StateRefinement(from = "open(this)", to = "closed(this)")
    public void close() {}
}

public class CorrectStateChangeThroughAlias {
    static void distinctObjects() {
        SeparateResource a = new SeparateResource();
        SeparateResource b = new SeparateResource();
        b.close();
        a.read();
    }

    static void reassignmentBreaksAlias() {
        SeparateResource a = new SeparateResource();
        SeparateResource b = a;
        b = new SeparateResource();
        b.close();
        a.read();
    }

    static void aliasSeesOpenState() {
        SeparateResource a = new SeparateResource();
        SeparateResource b = a;
        b.read();
        a.read();
        a.close();
    }

    static void aliasClosedInBothBranches(boolean done) {
        SeparateResource a = new SeparateResource();
        SeparateResource b = a;
        if (done) {
            b.close();
        } else {
            a.close();
        }
        SeparateResource c = new SeparateResource();
        c.read();
    }
}
