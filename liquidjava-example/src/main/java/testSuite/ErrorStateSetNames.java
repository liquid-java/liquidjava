package testSuite;

import liquidjava.specification.Ghost;
import liquidjava.specification.StateSet;

// A state set name must be a lowercase identifier that no state, ghost or other state set of the class uses
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
