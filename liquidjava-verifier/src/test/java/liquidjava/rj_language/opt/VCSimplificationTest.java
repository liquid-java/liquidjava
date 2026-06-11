package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.assertSimplificationSteps;
import static liquidjava.utils.VCTestUtils.vc;
import static org.junit.jupiter.api.Assertions.assertNull;

import liquidjava.processor.VCImplication;
import org.junit.jupiter.api.Test;

class VCSimplificationTest {

    @Test
    void simplifyReturnsNullForNullImplication() {
        assertNull(VCSimplification.simplifyToFixedPoint(null));
    }

    @Test
    void simplifyOnceReturnsNullForNullImplication() {
        assertNull(VCSimplification.simplifyOnce(null));
    }

    @Test
    void simplifyOnceAppliesSubstitutionBeforeFolding() {
        VCImplication implication = vc("∀x:int. x == 1 + 2", "x > 2");

        assertSimplificationSteps(VCSimplification::simplifyOnce, implication, "1 + 2 > 2", "3 > 2", "true");
    }

    @Test
    void simplifyOnceDoesNotFoldAfterSubstitutionInSameStep() {
        VCImplication implication = vc("∀x:int. x == 1 + 2", "x == 3");

        assertSimplificationSteps(VCSimplification::simplifyOnce, implication, "1 + 2 == 3", "3 == 3", "true");
    }

    @Test
    void simplifyOnceAppliesFoldingWhenNoSubstitutionIsAvailable() {
        VCImplication implication = vc("1 + 2 > 2");

        assertSimplificationSteps(VCSimplification::simplifyOnce, implication, "3 > 2", "true");
    }

    @Test
    void simplifyKeepsApplyingStepsUntilFixedPoint() {
        VCImplication implication = vc("∀x:int. x == 1 + 2", "x + 1 > 3");

        assertSimplificationSteps(VCSimplification::simplifyOnce, implication, "1 + 2 + 1 > 3", "3 + 1 > 3", "4 > 3",
                "true");
    }

    @Test
    void simplifyAppliesMultipleSubstitutionsBeforeReachingFixedPoint() {
        VCImplication implication = vc("∀x:int. x == 3", "∀y:int. y == x + 1", "y > x");

        assertSimplificationSteps(VCSimplification::simplifyOnce, implication, "∀y:int. y == 3 + 1 -> y > 3",
                "3 + 1 > 3", "4 > 3", "true");
    }

    @Test
    void simplifyAppliesLongSubstitutionChainBeforeReachingFixedPoint() {
        VCImplication implication = vc("∀x:int. x == 1", "∀y:int. y == x + 1", "∀z:int. z == y + 1", "z == 3");

        assertSimplificationSteps(VCSimplification::simplifyOnce, implication,
                "∀y:int. y == 1 + 1 -> ∀z:int. z == y + 1 -> z == 3", "∀z:int. z == 1 + 1 + 1 -> z == 3",
                "1 + 1 + 1 == 3", "2 + 1 == 3", "3 == 3", "true");
    }

    @Test
    void simplifyCombinesSubstitutionAndNestedFoldingAcrossFixedPoint() {
        VCImplication implication = vc("∀x:int. x == 1", "∀y:int. y == x + 2", "y - 1 == 2");

        assertSimplificationSteps(VCSimplification::simplifyOnce, implication, "∀y:int. y == 1 + 2 -> y - 1 == 2",
                "1 + 2 - 1 == 2", "3 - 1 == 2", "2 == 2", "true");
    }

    @Test
    void simplifyStopsAfterSubstitutionWhenOnlyNegativeLiteralShapeChanges() {
        VCImplication implication = vc("∀x:int. x == a + 0", "x >= -3");

        assertSimplificationSteps(VCSimplification::simplifyOnce, implication, "a + 0 >= -3");
    }

    @Test
    void simplifyLeavesUnchangedVcAsPlainPredicates() {
        VCImplication implication = vc("x > 0", "y > x");

        assertSimplificationSteps(VCSimplification::simplifyOnce, implication, "x > 0 -> y > x");
    }
}
