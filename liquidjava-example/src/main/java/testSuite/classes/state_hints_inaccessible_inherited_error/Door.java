package testSuite.classes.state_hints_inaccessible_inherited_error;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"shut", "ajar"})
public interface Door {

    @StateRefinement(from = "shut(this)", to = "ajar(this)")
    private void pry() {}

    @StateRefinement(from = "shut(this)", to = "ajar(this)")
    default void push() {}

    @StateRefinement(from = "ajar(this)")
    default void walkThrough() {}
}

// only push() can be called on an implementing class: pry() is private
class FrontDoor implements Door {

    @StateRefinement(to = "shut(this)")
    FrontDoor() {}

    static void walkThroughShut() {
        FrontDoor door = new FrontDoor();
        door.walkThrough(); // Expect: State Refinement Error
    }
}
