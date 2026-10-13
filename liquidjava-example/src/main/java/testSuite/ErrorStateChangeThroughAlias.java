package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"open", "closed"})
class AliasedResource {
    @StateRefinement(to = "open(this)")
    public AliasedResource() {}

    @StateRefinement(from = "open(this)")
    public void read() {}

    @StateRefinement(from = "open(this)", to = "closed(this)")
    public void close() {}
}

public class ErrorStateChangeThroughAlias {
    static void localAlias() {
        AliasedResource a = new AliasedResource();
        AliasedResource b = a;
        b.close();
        a.read(); // Expect: State Refinement Error
    }

    static void aliasOfAlias() {
        AliasedResource a = new AliasedResource();
        AliasedResource b = a;
        AliasedResource c = b;
        a.close();
        c.read(); // Expect: State Refinement Error
    }

    static void aliasAssignedLater() {
        AliasedResource a = new AliasedResource();
        AliasedResource b = new AliasedResource();
        b = a;
        b.close();
        a.read(); // Expect: State Refinement Error
    }

    static void aliasClosedInBranch(boolean done) {
        AliasedResource a = new AliasedResource();
        AliasedResource b = a;
        if (done) {
            b.close();
        }
        a.read(); // Expect: State Refinement Error
    }
}
