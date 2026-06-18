package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

        assertSimplificationSteps(implication, step("1 + 2 > 2"), step("3 > 2"), step("true"));
    }

    @Test
    void simplifyOnceDoesNotFoldAfterSubstitutionInSameStep() {
        VCImplication implication = vc("∀x:int. x == 1 + 2", "x == 3");

        assertSimplificationSteps(implication, step("1 + 2 == 3"), step("3 == 3"), step("true"));
    }

    @Test
    void simplifyOnceAppliesSubstitutionBeforeBinderSimplification() {
        VCImplication implication = vc("∀x:int. x == 3", "∀y:int. true", "x > 0");

        assertSimplificationSteps(implication, step("true", "3 > 0"), step("3 > 0"), step("true"));
    }

    @Test
    void simplifyOnceAppliesBinderSimplificationBeforeFolding() {
        VCImplication implication = vc("∀x:int. true", "1 + 2 > 0");

        assertSimplificationSteps(implication, step("1 + 2 > 0"), step("3 > 0"));
    }

    @Test
    void simplifyOnceAppliesBinderSimplificationBeforeLogicalSimplification() {
        VCImplication implication = vc("∀x:int. true", "y && true");

        assertSimplificationSteps(implication, step("y && true"), step("y"));
    }

    @Test
    void simplifyOnceAppliesFoldingWhenNoSubstitutionIsAvailable() {
        VCImplication implication = vc("1 + 2 > 2");

        assertSimplificationSteps(implication, step("3 > 2"), step("true"));
    }

    @Test
    void simplifyOnceAppliesFoldingBeforeArithmeticSimplification() {
        VCImplication implication = vc("1 + 2 + x + 0 > 0");

        assertSimplificationSteps(implication, step("3 + x + 0 > 0"));
    }

    @Test
    void simplifyOnceAppliesArithmeticWhenNoSubstitutionOrFoldingIsAvailable() {
        VCImplication implication = vc("x + 0 > 0");

        assertSimplificationSteps(implication, step("x > 0"));
    }

    @Test
    void simplifyOnceAppliesArithmeticBeforeLogicalSimplification() {
        VCImplication implication = vc("x + 0 == x");

        assertSimplificationSteps(implication, step("x == x"), step("true"));
    }

    @Test
    void simplifyOnceAppliesLogicalWhenNoEarlierSimplificationIsAvailable() {
        VCImplication implication = vc("x && true");

        assertSimplificationSteps(implication, step("x"));
    }

    @Test
    void simplifyAppliesLogicalStepsUntilFixedPoint() {
        VCImplication implication = vc("x && true && true");

        assertSimplificationSteps(implication, step("x && true"), step("x"));
    }

    @Test
    void simplifyKeepsApplyingStepsUntilFixedPoint() {
        VCImplication implication = vc("∀x:int. x == 1 + 2", "x + 1 > 3");

        assertSimplificationSteps(implication, step("1 + 2 + 1 > 3"), step("3 + 1 > 3"), step("4 > 3"), step("true"));
    }

    @Test
    void simplifyToFixedPointRemovesTrueBindersOverMultipleSteps() {
        VCImplication implication = vc("∀x:int. true", "∀y:int. true", "z > 0");

        VCSimplificationResult result = VCSimplification.simplifyToFixedPoint(implication);

        assertSimplifiedVC(result.getImplication(), "z > 0");
        assertNotNull(result.getOrigin());
        assertNotNull(result.getOrigin().getOrigin());
    }

    @Test
    void simplifyAppliesMultipleSubstitutionsBeforeReachingFixedPoint() {
        VCImplication implication = vc("∀x:int. x == 3", "∀y:int. y == x + 1", "y > x");

        assertSimplificationSteps(implication, step("y == 3 + 1", "y > 3"), step("3 + 1 > 3"), step("4 > 3"),
                step("true"));
    }

    @Test
    void simplifyAppliesLongSubstitutionChainBeforeReachingFixedPoint() {
        VCImplication implication = vc("∀x:int. x == 1", "∀y:int. y == x + 1", "∀z:int. z == y + 1", "z == 3");

        assertSimplificationSteps(implication, step("y == 1 + 1", "z == y + 1", "z == 3"),
                step("z == 1 + 1 + 1", "z == 3"), step("1 + 1 + 1 == 3"), step("2 + 1 == 3"), step("3 == 3"),
                step("true"));
    }

    @Test
    void simplifyCombinesSubstitutionAndNestedFoldingAcrossFixedPoint() {
        VCImplication implication = vc("∀x:int. x == 1", "∀y:int. y == x + 2", "y - 1 == 2");

        assertSimplificationSteps(implication, step("y == 1 + 2", "y - 1 == 2"), step("1 + 2 - 1 == 2"),
                step("3 - 1 == 2"), step("2 == 2"), step("true"));
    }

    @Test
    void simplifyStopsAfterSubstitutionWhenOnlyNegativeLiteralShapeChanges() {
        VCImplication implication = vc("∀x:int. x == a + 0", "x >= -3");

        assertSimplificationSteps(implication, step("a + 0 >= -3"));
    }

    @Test
    void simplifyLeavesUnchangedVcAsPlainPredicates() {
        VCImplication implication = vc("x > 0", "y > x");

        VCSimplificationResult result = VCSimplification.simplifyOnce(new VCSimplificationResult(implication));

        assertSimplifiedVC(result.getImplication(), "x > 0", "y > x");
        assertNull(result.getOrigin());
    }

    @Test
    void simplifyOnceStoresACompleteClonedOriginChain() {
        VCImplication implication = vc("x > 0", "y + 0 > x");

        VCSimplificationResult result = VCSimplification.simplifyOnce(new VCSimplificationResult(implication));
        implication.getNext().setRefinement(vc("changed").getRefinement());

        assertSimplifiedVC(result.getImplication(), "x > 0", "y > x");
        assertSimplifiedVC(result.getOrigin().getImplication(), "x > 0", "y + 0 > x");
    }

    @Test
    void fixedPointHistoryLinksEverySuccessfulStep() {
        VCImplication implication = vc("∀x:int. x == 1 + 2", "x + 0 > 2");

        VCSimplificationResult result = VCSimplification.simplifyToFixedPoint(implication);
        int historyLength = 0;
        for (VCSimplificationResult current = result.getOrigin(); current != null; current = current.getOrigin())
            historyLength++;

        assertEquals(4, historyLength);
        assertSimplifiedVC(result.getImplication(), "true");
    }
}
