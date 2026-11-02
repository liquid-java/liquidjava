package testSuite.classes.state_hint_checked_state_error;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"hasMore", "noMore"})
@ExternalRefinementsFor("java.util.Scanner")
public interface ScannerRefinements {

    @Refinement("_ == true --> hasMore(this)")
    boolean hasNext();

    @StateRefinement(from = "hasMore(this)", to = "noMore(this)")
    String next();
}
