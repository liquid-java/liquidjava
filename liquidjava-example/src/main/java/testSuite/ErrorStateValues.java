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
