package testSuite;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.Ghost;
import liquidjava.specification.RefinementPredicate;
import liquidjava.specification.StateSet;

// A state set name must be a lowercase identifier that no state, ghost or other state set of the class uses, and
// not one of the names given to state sets without a name. A name that is also a method of the class is only a
// warning (#514): undo(x) == undoable reads like a call to undo()
@StateSet(name = "Door", value = {"closed", "opened"}) // Expect: Error
public class ErrorStateSetNames {}

@StateSet(name = "locked", value = {"closed", "opened"}) // Expect: Error
@StateSet({"unlocked", "locked"})
class ErrorStateSetNameOfAState {}

@StateSet(name = "lock", value = {"closed", "opened"}) // Expect: Error
@Ghost("int lock")
class ErrorStateSetNameOfAGhost {}

@StateSet(name = "door", value = {"closed", "opened"}) // Expect: Error
@StateSet(name = "door", value = {"unlocked", "locked"})
class ErrorStateSetNameOfAnotherSet {}

@StateSet(name = "size", value = {"empty", "full"}) // Expect: Error
@RefinementPredicate("int size(ErrorStateSetNameOfARefinementPredicate b)")
class ErrorStateSetNameOfARefinementPredicate {}

@StateSet(name = "state2", value = {"closed", "opened"}) // Expect: Error
class ErrorStateSetNameOfAGeneratedName {}

@StateSet(name = "undo", value = {"undoable", "notUndoable"}) // Expect: Warning
class ErrorStateSetNameOfAMethod {
    void undo() {}
}

@StateSet(name = "undo", value = {"undoable", "notUndoable"}) // Expect: Warning
class ErrorStateSetNameOfAnInheritedMethod extends ErrorStateSetNameOfAMethod {}

@ExternalRefinementsFor("java.util.ArrayList")
@StateSet(name = "clear", value = {"empty", "nonEmpty"}) // Expect: Warning
interface ErrorStateSetNameOfAMethodOfTheTarget {}
