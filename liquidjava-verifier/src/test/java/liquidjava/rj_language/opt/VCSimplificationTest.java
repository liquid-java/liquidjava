package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.assertSimplifiedVC;
import static liquidjava.utils.VCTestUtils.assertVC;
import static liquidjava.utils.VCTestUtils.simplified;
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

        VCImplication result = VCSimplification.simplifyOnce(implication);

        assertSimplifiedVC(result, simplified("1 + 2 > 2", "∀x:int. x > 2"));
    }

    @Test
    void simplifyOnceDoesNotFoldAfterSubstitutionInSameStep() {
        VCImplication implication = vc("∀x:int. x == 1 + 2", "x == 3");

        VCImplication result = VCSimplification.simplifyOnce(implication);

        assertSimplifiedVC(result, simplified("1 + 2 == 3", "∀x:int. x == 3"));
    }

    @Test
    void simplifyOnceAppliesFoldingWhenNoSubstitutionIsAvailable() {
        VCImplication implication = vc("1 + 2 > 2");

        VCImplication result = VCSimplification.simplifyOnce(implication);

        assertSimplifiedVC(result, simplified("true", "1 + 2 > 2"));
    }

    @Test
    void simplifyKeepsApplyingStepsUntilFixedPoint() {
        VCImplication implication = vc("∀x:int. x == 1 + 2", "x + 1 > 3");

        VCImplication result = VCSimplification.simplifyToFixedPoint(implication);

        assertSimplifiedVC(result, simplified("true", "∀x:int. x + 1 > 3"));
    }

    @Test
    void simplifyAppliesMultipleSubstitutionsBeforeReachingFixedPoint() {
        VCImplication implication = vc("∀x:int. x == 3", "∀y:int. y == x + 1", "y > x");

        VCImplication result = VCSimplification.simplifyToFixedPoint(implication);

        assertSimplifiedVC(result, simplified("true", "∀y:int. y > x"));
    }

    @Test
    void simplifyAppliesLongSubstitutionChainBeforeReachingFixedPoint() {
        VCImplication implication = vc("∀x:int. x == 1", "∀y:int. y == x + 1", "∀z:int. z == y + 1", "z == 3");

        VCImplication result = VCSimplification.simplifyToFixedPoint(implication);

        assertSimplifiedVC(result, simplified("true", "∀z:int. z == 3"));
    }

    @Test
    void simplifyCombinesSubstitutionAndNestedFoldingAcrossFixedPoint() {
        VCImplication implication = vc("∀x:int. x == 1", "∀y:int. y == x + 2", "y - 1 == 2");

        VCImplication result = VCSimplification.simplifyToFixedPoint(implication);

        assertSimplifiedVC(result, simplified("true", "∀y:int. y - 1 == 2"));
    }

    @Test
    void simplifyLeavesUnchangedVcAsPlainPredicates() {
        VCImplication implication = vc("x > 0", "y > x");

        VCImplication result = VCSimplification.simplifyToFixedPoint(implication);

        assertVC(result, "x > 0", "y > x");
    }
}
