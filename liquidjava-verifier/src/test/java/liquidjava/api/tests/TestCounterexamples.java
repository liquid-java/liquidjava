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
        assertAssignments(error, assignment("x", "1"), assignment("#x", "0"));
    }

    @Test
    void integerDivisionIncludesInputAndGeneratedReturn() {
        RefinementError error = verify("ErrorIntegerDivision.java");
        assertAssignments(error, assignment("x", "1"), assignment("#ret", "0"));
    }

    @Test
    void dependentUpperBoundIncludesBoundaryValuesInBinderOrder() {
        RefinementError error = verify("ErrorDependentUpperBound.java");
        assertAssignments(error, assignment("len", "1"), assignment("i", "0"), assignment("#ret", "1"));
    }

    @Test
    void literalZeroHasNoCounterexampleBecauseValueIsAlreadyKnown() {
        RefinementError error = verify("ErrorLiteralZero.java");
        assertTrue(error.getCounterexample().isEmpty());
    }

    @Test
    void identityRetainsInputAndReturnSelectedByTheModel() {
        RefinementError error = verify("ErrorIdentity.java");
        assertAssignments(error, assignment("x", "0"), assignment("#ret", "0"));
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
        assertAssignments(error, assignment("x", "-1"), assignment("#ret", "0"));
    }

    @Test
    void pathConditionIsOmittedWhileRecursiveArgumentRemains() {
        RefinementError error = verify("ErrorRecursion.java");
        assertAssignments(error, assignment("k", "0"), assignment("#k", "-1"));
    }

    @Test
    void booleanCounterexampleIncludesInputAndGeneratedReturn() {
        RefinementError error = verify("ErrorBoolean.java");
        assertAssignments(error, assignment("value", "false"), assignment("#ret", "false"));
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
