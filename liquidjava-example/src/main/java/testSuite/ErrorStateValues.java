package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet(name = "door", value = {"closed", "opened"})
@StateSet(name = "lock", value = {"unlocked", "locked"})
public class ErrorStateValues {

    @StateRefinement(from = "door(this) == locked") // Expect: Error
    public void wrongSet() {}
}

@StateSet({"closed", "open"})
@StateSet({"open", "locked"}) // Expect: Error
class ErrorStateValuesSharedState {}

@StateSet({"state1", "open"}) // Expect: Error
class ErrorStateValuesGeneratedSetName {}

// an unrelated set named door that has a state locked does not make locked a state of the door above
@StateSet(name = "door", value = {"locked", "free"})
class ErrorStateValuesOtherDoor {}

// the set applied as a method of this, or with this left out, constrains the same state
@StateSet(name = "door", value = {"shut", "ajar"})
class ErrorStateValueForms {

    @StateRefinement(to = "door() == shut")
    public ErrorStateValueForms() {}

    @StateRefinement(from = "this.door() == ajar")
    public void close() {}

    static void test() {
        new ErrorStateValueForms().close(); // Expect: State Refinement Error
    }
}
