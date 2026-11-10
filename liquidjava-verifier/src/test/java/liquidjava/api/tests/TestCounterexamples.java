package liquidjava.api.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import liquidjava.api.CommandLineLauncher;
import liquidjava.diagnostics.Diagnostics;
import liquidjava.diagnostics.errors.LJError;
import liquidjava.diagnostics.errors.RefinementError;
import liquidjava.utils.Pair;

class TestCounterexamples {

    private static final String TEST_SUITE = "../liquidjava-example/src/main/java/testSuite/";

    @Test
    void recursiveDecrementIncludesInputAndGeneratedArgument() {
        RefinementError error = verify("ErrorRecursiveDecrement.java");
        assertAssignments(error, assignment("#x", "0"), assignment("x", "1"));
    }

    @Test
    void integerDivisionIncludesInputAndGeneratedReturn() {
        RefinementError error = verify("ErrorIntegerDivision.java");
        assertAssignments(error, assignment("#ret", "0"), assignment("x", "1"));
    }

    @Test
    void dependentUpperBoundListsExpectedValuesBeforeLinkedOnes() {
        RefinementError error = verify("ErrorDependentUpperBound.java");
        assertAssignments(error, assignment("len", "1"), assignment("#ret", "1"), assignment("i", "0"));
    }

    @Test
    void literalZeroHasNoCounterexampleBecauseValueIsAlreadyKnown() {
        RefinementError error = verify("ErrorLiteralZero.java");
        assertTrue(error.getCounterexample().isEmpty());
    }

    @Test
    void identityRetainsInputAndReturnSelectedByTheModel() {
        RefinementError error = verify("ErrorIdentity.java");
        assertAssignments(error, assignment("#ret", "0"), assignment("x", "0"));
    }

    @Test
    void staticFinalConstantHasNoCounterexampleBecauseValueIsAlreadyKnown() {
        RefinementError error = verify("ErrorStaticFinalConstant.java");
        assertTrue(error.getCounterexample().isEmpty());
    }

    @Test
    void knownReturnAssignmentIsRemovedWhileDependentAssignmentsRemain() {
        RefinementError error = verify("ErrorDependentRefinement.java");
        assertAssignments(error, assignment("smaller", "0"), assignment("bigger", "21"));
    }

    @Test
    void multipleParametersAndGeneratedReturnFollowBinderOrder() {
        RefinementError error = verify("ErrorFunctionDeclarations.java");
        assertAssignments(error, assignment("d", "0"), assignment("i", "1"), assignment("#ret", "2"));
    }

    @Test
    void variableUpdateIncludesNegativeInputAndGeneratedReturn() {
        RefinementError error = verify("ErrorAssignmentBeforeReturn.java");
        assertAssignments(error, assignment("#ret", "0"), assignment("x", "-1"));
    }

    @Test
    void pathConditionIsOmittedWhileRecursiveArgumentRemains() {
        RefinementError error = verify("ErrorRecursion.java");
        assertAssignments(error, assignment("#k", "-1"), assignment("k", "0"));
    }

    @Test
    void booleanCounterexampleIncludesInputAndGeneratedReturn() {
        RefinementError error = verify("ErrorBoolean.java");
        assertAssignments(error, assignment("#ret", "false"), assignment("value", "false"));
    }

    @Test
    void argumentFoundKnowsNothingAboutIsKeptWhileUnrelatedGuardIsOmitted() {
        RefinementError error = verify("ErrorCounterexampleUnconstrainedArgument.java");
        assertAssignments(error, assignment("timeout", "-1"));
    }

    @Test
    void argumentIsKeptWhenFoundIsTrue() {
        RefinementError error = verify("ErrorCounterexampleNothingKnown.java");
        assertAssignments(error, assignment("millis", "-1"));
    }

    @Test
    void argumentConstrainedByFoundIsKeptWhileUnrelatedGuardIsOmitted() {
        RefinementError error = verify("ErrorCounterexampleGuardedArgument.java");
        assertAssignments(error, assignment("timeout", "-1"));
    }

    @Test
    void aliasedArgumentKeepsTheVariableItCopiesAfterIt() {
        RefinementError error = verify("ErrorCounterexampleAliasedArgument.java");
        assertAssignments(error, assignment("#timeout", "-1"), assignment("configured", "-1"));
    }

    @Test
    void fieldArgumentKeepsTheField() {
        RefinementError error = verify("ErrorCounterexampleFieldArgument.java");
        assertAssignments(error, assignment("#timeout", "-1"),
                assignment("this#testSuite.ErrorCounterexampleFieldArgument.timeout", "-1"));
    }

    @Test
    void callInsideArgumentKeepsItsResultAfterTheArgument() {
        RefinementError error = verify("ErrorCounterexampleCallArgument.java");
        assertAssignments(error, assignment("#timeout", "-1"), assignment("##read_result", "0"));
    }

    @Test
    void branchConditionsThatDoNotAffectTheArgumentAreOmitted() {
        RefinementError error = verify("ErrorCounterexampleBranchArgument.java");
        assertAssignments(error, assignment("timeout", "-1"));
    }

    @Test
    void severalExpectedVariablesListTheUnconstrainedOneFirst() {
        RefinementError error = verify("ErrorCounterexampleSeveralArguments.java");
        assertAssignments(error, assignment("hi", "0"), assignment("lo", "1"));
    }

    @Test
    void knownArgumentIsOmittedWhileTheOtherArgumentIsKept() {
        RefinementError error = verify("ErrorCounterexampleKnownArgument.java");
        assertAssignments(error, assignment("capacity", "0"));
    }

    @Test
    void pathConditionLinkedToTheArgumentIsKeptWhileUnlinkedOneIsOmitted() {
        RefinementError error = verify("ErrorCounterexampleLinkedPathCondition.java");
        assertAssignments(error, assignment("#timeout", "-1"), assignment("port", "0"));
    }

    @Test
    void returnKeepsItsSourceWhileUnrelatedPathConditionIsOmitted() {
        RefinementError error = verify("ErrorCounterexampleReturnPath.java");
        assertAssignments(error, assignment("#ret", "-1"), assignment("configured", "-1"));
    }

    @Test
    void refinementAliasArgumentIsKept() {
        RefinementError error = verify("ErrorCounterexampleRefinementAlias.java");
        assertAssignments(error, assignment("timeout", "-1"));
    }

    @Test
    void ghostOfArgumentFoundKnowsNothingAboutIsKept() {
        RefinementError error = verify("ErrorCounterexampleGhostArgument.java");
        assertAssignments(error, assignment("size(bag)", "0"));
        assertTrue(error.getCounterexampleStr().contains("Counterexample: size(bag) == 0"));
    }

    @Test
    void ghostValueFoundAlreadyStatesIsOmittedWithTheUnrelatedPathCondition() {
        RefinementError error = verify("ErrorCounterexampleKnownGhost.java");
        assertTrue(error.getCounterexample().isEmpty());
    }

    @Test
    void combinedGuardShowsOnlyTheArgument() {
        RefinementError error = verify("ErrorCounterexampleCombinedGuard.java");
        assertAssignments(error, assignment("timeout", "-1"));
    }

    @Test
    void mixedGuardShowsOnlyTheArgument() {
        RefinementError error = verify("ErrorCounterexampleMixedGuard.java");
        assertAssignments(error, assignment("timeout", "-1"));
    }

    @Test
    void guardRelatingTheArgumentToAnotherVariableKeepsItAfterTheArgument() {
        RefinementError error = verify("ErrorCounterexampleGuardRelatesArguments.java");
        assertAssignments(error, assignment("timeout", "-1"), assignment("retries", "0"));
    }

    @Test
    void returnedConstantFoundAlreadyStatesHasNoCounterexample() {
        RefinementError error = verify("ErrorCounterexampleReturnConstant.java");
        assertTrue(error.getCounterexample().isEmpty());
    }

    @Test
    void returnComputedFromThePathConditionShowsBoth() {
        RefinementError error = verify("ErrorCounterexampleReturnFromPath.java");
        assertAssignments(error, assignment("#ret", "0"), assignment("port", "-1"));
    }

    @Test
    void argumentFoundAlreadyStatesHasNoCounterexampleUnderAGuard() {
        RefinementError error = verify("ErrorCounterexampleKnownGuardedArgument.java");
        assertTrue(error.getCounterexample().isEmpty());
    }

    @Test
    void argumentFoundAlreadyStatesHasNoCounterexampleUnderTwoGuards() {
        RefinementError error = verify("ErrorCounterexampleKnownTwiceGuardedArgument.java");
        assertTrue(error.getCounterexample().isEmpty());
    }

    @Test
    void argumentKnownThroughAnAliasHasNoCounterexampleUnderAGuard() {
        RefinementError error = verify("ErrorCounterexampleKnownAliasedArgument.java");
        assertTrue(error.getCounterexample().isEmpty());
    }

    @Test
    void enumValueFoundAlreadyStatesIsOmitted() {
        RefinementError error = verify("ErrorCounterexampleKnownEnum.java");
        assertTrue(error.getCounterexample().isEmpty());
    }

    @Test
    void arrayArgumentShowsItsLengthAndNotTheArray() {
        RefinementError error = verify("ErrorCounterexampleArrayArgument.java");
        assertAssignments(error, assignment("length(a)", "0"));
    }

    @Test
    void indexIntoLocalArrayShowsTheIndexAndNotTheArray() {
        RefinementError error = verify("ErrorCounterexampleArrayIndex.java");
        assertAssignments(error, assignment("k", "-1"));
    }

    @Test
    void stateErrorShowsTheValueThatPicksTheFailingBranchButNoStateValue() {
        CommandLineLauncher.launch(TEST_SUITE + "ErrorStateHintPathDependent.java");
        List<LJError> errors = Diagnostics.getInstance().getErrors().stream().toList();
        assertEquals(1, errors.size());
        assertEquals("Counterexample: ready == false", errors.get(0).getCounterexampleStr());
    }

    private static RefinementError verify(String test) {
        CommandLineLauncher.launch(TEST_SUITE + test);
        List<LJError> errors = Diagnostics.getInstance().getErrors().stream().toList();
        assertEquals(1, errors.size(), "Expected exactly one error from " + test);
        return assertInstanceOf(RefinementError.class, errors.get(0));
    }

    @SafeVarargs
    private static void assertAssignments(RefinementError error, Pair<String, String>... expectedAssignments) {
        // get counterexample assignments without instance numbers in variable names
        List<Pair<String, String>> actualAssignments = error.getCounterexample().assignments().stream()
                .map(assignment -> assignment(assignment.first().replaceAll("_[0-9]+$", ""), assignment.second()))
                .toList();
        assertEquals(List.of(expectedAssignments), actualAssignments);
    }

    private static Pair<String, String> assignment(String name, String value) {
        return new Pair<>(name, value);
    }
}
