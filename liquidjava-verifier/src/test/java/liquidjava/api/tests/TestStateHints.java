package liquidjava.api.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import liquidjava.api.CommandLineLauncher;
import liquidjava.diagnostics.Diagnostics;
import liquidjava.diagnostics.SourceReference;
import liquidjava.diagnostics.categories.ErrorCategory;
import liquidjava.diagnostics.errors.LJError;
import liquidjava.diagnostics.errors.StateRefinementError;
import liquidjava.diagnostics.hints.Hint;
import liquidjava.diagnostics.hints.Hint.Kind;

class TestStateHints {

    private static final String TEST_SUITE = "../liquidjava-example/src/main/java/testSuite/";

    @Test
    void missingCallIsSuggestedWithTheStateItLeadsTo() {
        assertHints("ErrorStateHintCallSequence.java",
                new Hint(Kind.CALL_SEQUENCE, "Call `f.open()` before this call; afterwards `f` is `open`"));
    }

    @Test
    void missingCallsAreSuggestedInOrder() {
        assertHints("ErrorStateHintTwoCalls.java", new Hint(Kind.CALL_SEQUENCE,
                "Call `r.prepare()`, then `r.start()` before this call; afterwards `r` is `running`"));
    }

    @Test
    void callSequenceNamesOnlyTheStateSetsItChanges() {
        assertHints("ErrorStateHintMultipleSets.java", new Hint(Kind.CALL_SEQUENCE,
                "Call `d.unlock()` before this call; afterwards `lockState(d) == unlocked`"));
    }

    @Test
    void finalStateSuggestsCreatingANewObject() {
        assertHints("ErrorStateHintFinalState.java", new Hint(Kind.UNREACHABLE_STATE,
                "No sequence of calls moves `f` from `closed` to `open`. Create a new object instead with `new ErrorStateHintFinalState()`"),
                new Hint(Kind.FINAL_STATE,
                        "`f` is in state `closed`, where none of its methods with state refinements can be called"));
    }

    @Test
    void transitionDependingOnArgumentsIsSuggestedWithItsConditionButPreventsClaimingStateIsFinal() {
        assertHints("ErrorStateHintIncomplete.java", new Hint(Kind.CALL_SEQUENCE,
                "Call `f.reopen(int attempts)` with `attempts > 0` before this call; afterwards `f` is `open`"));
    }

    @Test
    void subclassTransitionsPreventClaimingStateIsUnreachable() {
        assertHints("ErrorStateHintSubclass.java");
    }

    @Test
    void subclassReceiverCanUseItsOwnTransitionsOnInheritedStates() {
        assertHints("ErrorStateHintInheritedStates.java",
                new Hint(Kind.CALL_SEQUENCE, "Call `s.bc()` before this call; afterwards `s` is `c`"));
    }

    @Test
    void overrideInTheReceiversClassReplacesTheInheritedTransition() {
        assertHints("ErrorStateHintOverride.java",
                new Hint(Kind.CALL_SEQUENCE, "Call `s.next()` before this call; afterwards `s` is `c`"));
    }

    @Test
    void stateSetAndMethodsFromSpecsOfTheReceiversSuperclasses() {
        assertHints("classes/state_hints_grandchild_spec_error", new Hint(Kind.CALL_SEQUENCE,
                "Call `window.dispose()` before this call; afterwards `window` is `notDisplayable`"));
    }

    @Test
    void superclassRefinementOfAMethodTheReceiversClassRedeclaresIsNotSuggested() {
        assertHints("classes/state_hints_redeclared_method_error", new Hint(Kind.CALL_SEQUENCE,
                "Call `host.dispose()` before this call; afterwards `host` is `notDisplayable`"));
    }

    @Test
    void stateSetAndMethodsFromTheReceiversSuperclassSpec() {
        assertHints("classes/state_hints_superclass_spec_error", new Hint(Kind.CALL_SEQUENCE,
                "Call `host.dispose()` before this call; afterwards `host` is `notDisplayable`"));
    }

    @Test
    void ownGenericRefinementOfAnInheritedMethodIsNotSuggested() {
        assertHints("classes/state_hints_generic_inherited_method_error");
    }

    @Test
    void statesOfAnotherClassWithTheSimpleNameOfASupertypeAreLeftOut() {
        assertHints("classes/state_hints_same_simple_name_error",
                new Hint(Kind.CALL_SEQUENCE, "Call `l.add(E)` before this call; afterwards `l` is `nonEmpty`"));
    }

    @Test
    void stateDependingOnThePathAsksForTheSameStateOnEveryPath() {
        assertHints("ErrorStateHintPathDependent.java", new Hint(Kind.PATH_DEPENDENT_STATE,
                "`f` can be in `created` or `open` here, depending on the path taken, and no sequence of calls moves all of them to `open`. Make `f` reach this line in the same state on every path"));
    }

    @Test
    void stateDependingOnThePathInEveryStateIsNotReportedAsUnknown() {
        assertHints("ErrorStateHintJoinOfAllStates.java", new Hint(Kind.PATH_DEPENDENT_STATE,
                "`f` can be in `created` or `open` here, depending on the path taken, and no sequence of calls moves all of them to `open`. Make `f` reach this line in the same state on every path"));
    }

    @Test
    void callAllowedByDifferentRefinementsInEachStateIsNotSuggested() {
        assertHints("ErrorStateHintSeveralRefinements.java", new Hint(Kind.PATH_DEPENDENT_STATE,
                "`f` can be in `created` or `open` here, depending on the path taken, and no sequence of calls moves all of them to `open`. Make `f` reach this line in the same state on every path"));
    }

    @Test
    void receiverThatIsNotAVariableGetsNoHints() {
        assertHints("ErrorStateHintMethodCallReceiver.java");
    }

    @Test
    void unknownStateDoesNotSuggestACallThatNeedsAnotherUnknownState() {
        assertHints("ErrorStateHintUnknownState.java",
                new Hint(Kind.NEEDED_STATE, "The state of `f` is unknown here, and this call needs it to be `open`"));
    }

    @Test
    void unknownStateOfAnExternalClassNamesOnlyTheCheckedStateSet() {
        assertHints("classes/state_hint_undo_manager_error",
                new Hint(Kind.NEEDED_STATE,
                        "The state of `undoManager` is unknown here, and this call needs it to be `redoable`"),
                new Hint(Kind.GUARD,
                        "Guard this call with `if (undoManager.canRedo())`; inside it, `undoManager` is `redoable`"));
    }

    @Test
    void unknownStateSuggestsCallsAllowedInAnyState() {
        assertHints("ErrorStateHintUnknownAnyState.java",
                new Hint(Kind.NEEDED_STATE, "The state of `f` is unknown here, and this call needs it to be `open`"),
                new Hint(Kind.CALL_SEQUENCE,
                        "Call `f.reset()`, then `f.open()` before this call; afterwards `f` is `open`"));
    }

    @Test
    void statesAreNamedOnlyInTheStateSetsThatMatter() {
        List<StateRefinementError> errors = errors("ErrorStateHintNaming.java", 3);
        assertHints(errors.get(0), new Hint(Kind.NEEDED_STATE,
                "The state of `s` is unknown here, and this call needs it to be `open` or `loud`"));
        assertHints(errors.get(1), new Hint(Kind.NEEDED_STATE,
                "The state of `t` is unknown here, and this call needs it to be `a` or `b`. Objects created with `new ErrorStateHintNamingToggle()` start in such a state"));
        assertHints(errors.get(2),
                new Hint(Kind.UNREACHABLE_STATE, "No sequence of calls moves `this` from `notRedoable` to `redoable`"));
    }

    @Test
    void failedCheckOfTheReceiverIsNotAnUnknownState() {
        List<StateRefinementError> errors = errors("classes/state_hint_checked_error", 3);
        // the check is suggested even where it was made with the opposite result
        assertHints(errors.get(0),
                new Hint(Kind.NEEDED_STATE,
                        "What is known here does not decide the state of `it`, and this call needs it to be `hasMore`"),
                new Hint(Kind.GUARD, "Guard this call with `if (it.hasNext())`; inside it, `it` is `hasMore`"));
        assertHints(errors.get(1), new Hint(Kind.NEEDED_STATE,
                "What is known here does not decide the state of `same`, and this call needs it to be `hasMore`"),
                new Hint(Kind.GUARD, "Guard this call with `if (same.hasNext())`; inside it, `same` is `hasMore`"));
        assertHints(errors.get(2),
                new Hint(Kind.NEEDED_STATE,
                        "The state of `input` is unknown here, and this call needs it to be `hasMore`"),
                new Hint(Kind.GUARD, "Guard this call with `if (input.hasNext())`; inside it, `input` is `hasMore`"));
        assertEquals(List.of(Optional.empty(), Optional.empty(), Optional.of(ErrorCategory.UNKNOWN)),
                errors.stream().map(LJError::getCategory).toList());
    }

    @Test
    void joinsAreNotTakenForAnUnknownState() {
        List<StateRefinementError> errors = errors("ErrorStateHintJoins.java", 2);
        assertHints(errors.get(0), new Hint(Kind.UNREACHABLE_STATE,
                "No sequence of calls moves `d` from (`closed` and `unlocked`) or (`closed` and `locked`) or (`opened` and `unlocked`) or (`opened` and `locked`) to `closed` and `unlocked`. Create a new object instead with `new ErrorStateHintJoins()`"));
        assertHints(errors.get(1), new Hint(Kind.UNREACHABLE_STATE,
                "No sequence of calls moves `d` from (`closed` and `unlocked`) or (`opened` and `unlocked`) to `closed` and `unlocked`. Create a new object instead with `new ErrorStateHintJoins()`"));
        assertEquals(Optional.empty(), errors.get(0).getCategory());
    }

    @Test
    void onlyConstructorsThatCanBeCalledAreSuggested() {
        List<StateRefinementError> errors = errors("ErrorStateHintConstructors.java", 4);
        assertHints(errors.get(0), new Hint(Kind.NEEDED_STATE,
                "The state of `value` is unknown here, and this call needs it to be `ready`. Objects created with `new ErrorStateHintConstructors()` start in such a state"));
        assertHints(errors.get(1), new Hint(Kind.NEEDED_STATE,
                "The state of `this` is unknown here, and this call needs it to be `ready`"));
        assertHints(errors.get(2), new Hint(Kind.NEEDED_STATE,
                "The state of `value` is unknown here, and this call needs it to be `ready`"));
        assertHints(errors.get(3), new Hint(Kind.NEEDED_STATE,
                "The state of `value` is unknown here, and this call needs it to be `ready`"));
    }

    @Test
    void onlyPublicInheritedMethodsAreSuggested() {
        List<StateRefinementError> errors = errors("classes/state_hints_inaccessible_inherited_error", 2);
        // the files are verified in no fixed order
        assertEquals(
                Set.of(List.of(new Hint(Kind.CALL_SEQUENCE,
                        "Call `hatch.openPublicly()` before this call; afterwards `hatch` is `open`")),
                        List.of(new Hint(Kind.CALL_SEQUENCE,
                                "Call `door.push()` before this call; afterwards `door` is `ajar`"))),
                errors.stream().map(StateRefinementError::getHints).collect(Collectors.toSet()));
    }

    @Test
    void argumentThatDecidedTheStateIsShownWithAValueThatWouldWork() {
        assertHints("ErrorArgumentPicksTheState.java", new Hint(Kind.DECIDING_ARGUMENT,
                "`buffer.reset(boolean keep)` on line 20 decides the state of `buffer`: with `keep == true`, this call would be allowed"));
        // the line it names, for clients to link to
        SourceReference reference = errors("ErrorArgumentPicksTheState.java", 1).get(0).getHints().get(0).reference();
        assertEquals("line 20", reference.text());
        assertEquals(20, reference.line());
    }

    @Test
    void deciderIsTheLastCallThatChangedTheCheckedStateSet() {
        assertHints("ErrorArgumentPicksTheStateEarlier.java", new Hint(Kind.DECIDING_ARGUMENT,
                "`param.setTilingMode(int mode)` on line 25 decides the state of `param`: with `mode == 2`, this call would be allowed"));
    }

    @Test
    void checksAreSuggestedOnlyWhenTheRefinementsProveThem() {
        List<StateRefinementError> errors = errors("ErrorStateHintGuards.java", 12);
        assertHints(errors.get(0), new Hint(Kind.NEEDED_STATE,
                "The state of `c` is unknown here, and this call needs it to be `open`. Objects created with `new GuardChannel()` start in such a state"),
                new Hint(Kind.GUARD, "Guard this call with `if (!c.isClosed())`; inside it, `c` is `open`"));
        assertHints(errors.get(1), new Hint(Kind.GUARD,
                "Guard this call with `if (l.tryLock(...))`, filling in the arguments of `tryLock(long timeout)`; inside it, `l` is `locked`"));
        assertHints(errors.get(2),
                new Hint(Kind.NEEDED_STATE, "The state of `p` is unknown here, and this call needs it to be `ready`"));
        assertHints(errors.get(3),
                new Hint(Kind.NEEDED_STATE, "The state of `w` is unknown here, and this call needs it to be `accept`"));
        assertHints(errors.get(4),
                new Hint(Kind.GUARD, "Guard this call with `if (w.canWrite())`; inside it, `w` is `accept`"));
        assertHints(errors.get(5), new Hint(Kind.NEEDED_STATE,
                "The state of `b` is unknown here, and this call needs it to be `undoable` and `redoable`"));
        assertHints(errors.get(6),
                new Hint(Kind.NEEDED_STATE, "The state of `p` is unknown here, and this call needs it to be `armed`"));
        assertHints(errors.get(7),
                new Hint(Kind.NEEDED_STATE, "The state of `m` is unknown here, and this call needs it to be `s1`"));
        assertHints(errors.get(8), new Hint(Kind.NEEDED_STATE,
                "The state of `t` is unknown here, and this call needs it to be `fresh`. Objects created with `new GuardToken()` start in such a state"));
        assertHints(errors.get(9),
                new Hint(Kind.AVAILABLE_CALLS, "In state `live`, `r` can call: `read()` (and possibly others)"));
        assertHints(errors.get(10), new Hint(Kind.NEEDED_STATE,
                "The state of `g` is unknown here, and this call needs it to be `running`"));
        assertHints(errors.get(11),
                new Hint(Kind.CALL_SEQUENCE, "Call `f.dispose()` before this call; afterwards `f` is `hidden`"));
    }

    @Test
    void argumentConditionsAreSuggestedOnlyWhenEveryArgumentMeetingThemWorks() {
        List<StateRefinementError> errors = errors("ErrorStateHintArgumentConditions.java", 7);
        assertHints(errors.get(0), new Hint(Kind.CALL_SEQUENCE,
                "Call `r.set(int lo, int hi)` with `lo < hi` before this call; afterwards `r` is `high`"));
        assertHints(errors.get(1), new Hint(Kind.CALL_SEQUENCE,
                "Call `chain.pick(1)`, then `chain.next()` before this call; afterwards `chain` is `c`"));
        assertHints(errors.get(2), new Hint(Kind.CALL_SEQUENCE,
                "Call `l.setLimit(int limit)` with `limit <= 0` before this call; afterwards `l` is `unbounded`"));
        // no condition, and no value of the earlier call's argument, is claimed to allow the call
        for (StateRefinementError error : errors.subList(3, 5))
            assertHints(error,
                    new Hint(Kind.AVAILABLE_CALLS, "In state `b`, `l` can call: `set(int)` (and possibly others)"));
        assertHints(errors.get(5),
                new Hint(Kind.AVAILABLE_CALLS, "In state `small`, `s` can call: `grow(int)` (and possibly others)"));
        assertHints(errors.get(6), new Hint(Kind.AVAILABLE_CALLS,
                "In state `empty`, `b` can call: `fill(ConditionBin)` (and possibly others)"));
    }

    @Test
    void callWhoseStateDependsOnItsArgumentIsSuggestedWithTheCondition() {
        assertHints("classes/state_hint_compression_mode_error", new Hint(Kind.CALL_SEQUENCE,
                "Call `wParam.setCompressionMode(2)` before this call; afterwards `wParam` is `compressionExplicit`"));
    }

    @Test
    void checksOfTheStudysUndoManagerAreSuggestedOneAfterTheOther() {
        List<StateRefinementError> errors = errors("classes/state_hint_study_undo_manager_error", 2);
        assertHints(errors.get(0),
                new Hint(Kind.NEEDED_STATE,
                        "The state of `undoManager` is unknown here, and this call needs it to be `redoable`"),
                new Hint(Kind.GUARD,
                        "Guard this call with `if (undoManager.canRedo())`; inside it, `undoManager` is `redoable`"));
        assertHints(errors.get(1),
                new Hint(Kind.NEEDED_STATE,
                        "The state of `undoManager` is unknown here, and this call needs it to be `undoable`"),
                new Hint(Kind.GUARD,
                        "Guard this call with `if (undoManager.canUndo())`; inside it, `undoManager` is `undoable`"));
    }

    @Test
    void foundOfTheStudysUndoManagerOmitsTheResultsOfOtherCalls() {
        StateRefinementError error = errors("classes/state_hint_study_undo_manager_error", 2).get(0);
        assertEquals("expected `redoable(undoManager)` but could not prove it",
                error.getMessage().replaceAll("[⁰¹²³⁴⁵⁶⁷⁸⁹]", ""));
    }

    @Test
    void foundKeepsANamedStateSetByItsName() {
        StateRefinementError error = errors("ErrorStateHintMultipleSets.java", 1).get(0);
        assertEquals(
                "found `doorState(d@26) == closed && lockState(d) == locked && doorState(d) == doorState(d@26)` but expected `doorState(d) == closed && lockState(d) == unlocked`",
                error.getMessage().replaceAll("[⁰¹²³⁴⁵⁶⁷⁸⁹]", ""));
        // the declaration is shown as written, with what its states mean under it
        assertTrue(error.toString().contains("  where `closed(this)` means `doorState(this) == closed`, "
                + "`unlocked(this)` means `lockState(this) == unlocked`\n"));
    }

    @Test
    void setNameOfTheClassAndOfItsSuperclassKeepsTheShorthand() {
        StateRefinementError error = errors("ErrorStateSetNameInSuperclass.java", 1).get(0);
        assertEquals("found `ready(c) && idle(c)` but expected `ready(c) && busy(c)`",
                error.getMessage().replaceAll("[⁰¹²³⁴⁵⁶⁷⁸⁹]", ""));
        assertHints(error, new Hint(Kind.CALL_SEQUENCE, "Call `c.start()` before this call; afterwards `c` is `busy`"));
    }

    @Test
    void queryWrittenWithItsStateSetIsOfferedAsAGuard() {
        assertHints("ErrorStateValueQuery.java",
                new Hint(Kind.NEEDED_STATE,
                        "The state of `d` is unknown here, and this call needs `lock(d) == unlocked`"),
                new Hint(Kind.GUARD, "Guard this call with `if (d.isUnlocked())`; inside it, `lock(d) == unlocked`"));
    }

    @Test
    void ghostNamedLikeAStateOfAnotherClassKeepsItsForm() {
        StateRefinementError error = errors("ErrorStateValueGhostName.java", 1).get(0);
        assertEquals("found `ready(sensor) == false` but expected `ready(sensor)`",
                error.getMessage().replaceAll("[⁰¹²³⁴⁵⁶⁷⁸⁹]", ""));
    }

    @Test
    void classWithoutStateSetKeepsTheGeneralHint() {
        assertHints("ErrorUnconstrainedStateRefinement.java", Hint.general(
                "No state information is known for this object. Make sure its state is initialized and available at this point."));
    }

    private static void assertHints(String test, Hint... expected) {
        assertHints(errors(test, 1).get(0), expected);
    }

    // the place a hint names, if any, is checked on its own
    private static void assertHints(StateRefinementError error, Hint... expected) {
        assertEquals(List.of(expected),
                error.getHints().stream().map(hint -> new Hint(hint.kind(), hint.message())).toList());
    }

    /** The {@code count} errors {@code test} reports, in the order they are reported */
    private static List<StateRefinementError> errors(String test, int count) {
        CommandLineLauncher.launch(TEST_SUITE + test);
        List<LJError> errors = Diagnostics.getInstance().getErrors().stream().toList();
        assertEquals(count, errors.size(), "Unexpected number of errors from " + test);
        return errors.stream().map(e -> assertInstanceOf(StateRefinementError.class, e)).toList();
    }
}
