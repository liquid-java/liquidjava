package testSuite.classes.state_hints_same_simple_name_error;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// shares its simple name with java.util.Collection, a supertype of ArrayList, but its states are not ArrayList's
@StateSet({"fresh", "used"})
public class Collection {
    @StateRefinement(to = "fresh(this)")
    public Collection() {}

    @StateRefinement(from = "fresh(this)", to = "used(this)")
    public void use() {}
}
