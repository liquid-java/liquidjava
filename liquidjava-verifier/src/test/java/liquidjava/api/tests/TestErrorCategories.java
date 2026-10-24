package liquidjava.api.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import liquidjava.api.CommandLineLauncher;
import liquidjava.diagnostics.Diagnostics;
import liquidjava.diagnostics.categories.ErrorCategory;
import liquidjava.diagnostics.errors.LJError;

class TestErrorCategories {

    private static final String TEST_SUITE = "../liquidjava-example/src/main/java/testSuite/";

    @Test
    void callInTheWrongSingleStateIsOutOfOrder() {
        assertCategory("ErrorStateHintCallSequence.java", ErrorCategory.OUT_OF_ORDER);
    }

    @Test
    void stateCombiningSeveralStateSetsIsOutOfOrder() {
        assertCategory("ErrorStateHintMultipleSets.java", ErrorCategory.OUT_OF_ORDER);
    }

    @Test
    void stateDependingOnTheBranchIsPathDependent() {
        assertCategory("ErrorStateHintPathDependent.java", ErrorCategory.PATH_DEPENDENT_STATE);
        assertCategory("ErrorStateHintJoinOfAllStates.java", ErrorCategory.PATH_DEPENDENT_STATE);
    }

    @Test
    void statesDifferingInAnotherStateSetGetNoCategory() {
        assertNoCategory("ErrorPathDependentOtherStateSet.java");
    }

    @Test
    void unknownStateIsUnprovable() {
        assertCategory("ErrorStateHintUnknownState.java", ErrorCategory.UNPROVABLE);
    }

    @Test
    void stateErrorWithoutStateSetGetsNoCategory() {
        assertNoCategory("ErrorUnconstrainedStateRefinement.java");
    }

    @Test
    void argumentOutsideItsRefinementIsOutOfDomain() {
        assertCategory("ErrorOutOfDomainArgument.java", ErrorCategory.OUT_OF_DOMAIN);
    }

    @Test
    void returnValueOutsideItsRefinementIsOutOfDomain() {
        assertCategory("ErrorIntegerDivision.java", ErrorCategory.OUT_OF_DOMAIN);
    }

    @Test
    void argumentWithNothingKnownIsUnprovable() {
        assertCategory("ErrorUnprovableArgument.java", ErrorCategory.UNPROVABLE);
    }

    private static void assertCategory(String test, ErrorCategory expected) {
        assertEquals(Optional.of(expected), verify(test).getCategory());
    }

    private static void assertNoCategory(String test) {
        assertEquals(Optional.empty(), verify(test).getCategory());
    }

    private static LJError verify(String test) {
        CommandLineLauncher.launch(TEST_SUITE + test);
        List<LJError> errors = Diagnostics.getInstance().getErrors().stream().toList();
        assertEquals(1, errors.size(), "Expected exactly one error from " + test);
        return errors.get(0);
    }
}
