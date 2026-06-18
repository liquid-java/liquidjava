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

        assertSimplificationResults(VCSimplification::simplifyOnce, implication,
                chain(expect("1 + 2 > 2", "∀x:int. x > 2")), chain(expect("3 > 2", "1 + 2 > 2")),
                chain(expect("true", "3 > 2")));
    }

    @Test
    void simplifyOnceDoesNotFoldAfterSubstitutionInSameStep() {
        VCImplication implication = vc("∀x:int. x == 1 + 2", "x == 3");

        assertSimplificationResults(VCSimplification::simplifyOnce, implication,
                chain(expect("1 + 2 == 3", "∀x:int. x == 3")), chain(expect("3 == 3", "1 + 2 == 3")),
                chain(expect("true", "3 == 3")));
    }

    @Test
    void simplifyOnceAppliesSubstitutionBeforeBinderSimplification() {
        VCImplication implication = vc("∀x:int. x == 3", "∀y:int. true", "x > 0");

        assertSimplificationResults(VCSimplification::simplifyOnce, implication,
                chain(expect("true"), expect("3 > 0", "∀x:int. x > 0")), chain(expect("3 > 0", "∀y:int. x > 0")),
                chain(expect("true", "3 > 0")));
    }

    @Test
    void simplifyOnceAppliesBinderSimplificationBeforeFolding() {
        VCImplication implication = vc("∀x:int. true", "1 + 2 > 0");

        assertSimplificationResults(VCSimplification::simplifyOnce, implication,
                chain(expect("1 + 2 > 0", "∀x:int. 1 + 2 > 0")), chain(expect("3 > 0", "1 + 2 > 0")));
    }

    @Test
    void simplifyOnceAppliesBinderSimplificationBeforeLogicalSimplification() {
        VCImplication implication = vc("∀x:int. true", "y && true");

        assertSimplificationResults(VCSimplification::simplifyOnce, implication,
                chain(expect("y && true", "∀x:int. y && true")), chain(expect("y", "y && true")));
    }

    @Test
    void simplifyOnceAppliesFoldingWhenNoSubstitutionIsAvailable() {
        VCImplication implication = vc("1 + 2 > 2");

        assertSimplificationResults(VCSimplification::simplifyOnce, implication, chain(expect("3 > 2", "1 + 2 > 2")),
                chain(expect("true", "3 > 2")));
    }

    @Test
    void simplifyOnceAppliesFoldingBeforeArithmeticSimplification() {
        VCImplication implication = vc("1 + 2 + x + 0 > 0");

        assertSimplificationResults(VCSimplification::simplifyOnce, implication,
                chain(expect("3 + x + 0 > 0", "1 + 2 + x + 0 > 0")));
    }

    @Test
    void simplifyOnceAppliesArithmeticWhenNoSubstitutionOrFoldingIsAvailable() {
        VCImplication implication = vc("x + 0 > 0");

        assertSimplificationResults(VCSimplification::simplifyOnce, implication, chain(expect("x > 0", "x + 0 > 0")));
    }

    @Test
    void simplifyOnceAppliesArithmeticBeforeLogicalSimplification() {
        VCImplication implication = vc("x + 0 == x");

        assertSimplificationResults(VCSimplification::simplifyOnce, implication, chain(expect("x == x", "x + 0 == x")),
                chain(expect("true", "x == x")));
    }

    @Test
    void simplifyOnceAppliesLogicalWhenNoEarlierSimplificationIsAvailable() {
        VCImplication implication = vc("x && true");

        assertSimplificationResults(VCSimplification::simplifyOnce, implication, chain(expect("x", "x && true")));
    }

    @Test
    void simplifyAppliesLogicalStepsUntilFixedPoint() {
        VCImplication implication = vc("x && true && true");

        assertSimplificationResults(VCSimplification::simplifyOnce, implication,
                chain(expect("x && true", "x && true && true")), chain(expect("x", "x && true")));
    }

    @Test
    void simplifyKeepsApplyingStepsUntilFixedPoint() {
        VCImplication implication = vc("∀x:int. x == 1 + 2", "x + 1 > 3");

        assertSimplificationResults(VCSimplification::simplifyOnce, implication,
                chain(expect("1 + 2 + 1 > 3", "∀x:int. x + 1 > 3")), chain(expect("3 + 1 > 3", "1 + 2 + 1 > 3")),
                chain(expect("4 > 3", "3 + 1 > 3")), chain(expect("true", "4 > 3")));
    }

    @Test
    void simplifyToFixedPointRemovesTrueBindersOverMultipleSteps() {
        VCImplication implication = vc("∀x:int. true", "∀y:int. true", "z > 0");

        VCSimplificationResult result = VCSimplification.simplifyToFixedPoint(implication);

        assertSimplifiedVC(result.getImplication(), expect("z > 0", "∀y:int. z > 0"));
        assertNotNull(result.getOrigin());
        assertNotNull(result.getOrigin().getOrigin());
    }

    @Test
    void simplifyAppliesMultipleSubstitutionsBeforeReachingFixedPoint() {
        VCImplication implication = vc("∀x:int. x == 3", "∀y:int. y == x + 1", "y > x");

        assertSimplificationResults(VCSimplification::simplifyOnce, implication,
                chain(expect("y == 3 + 1", "∀x:int. y == x + 1"), expect("y > 3", "∀x:int. y > x")),
                chain(expect("3 + 1 > 3", "∀y:int. y > 3")), chain(expect("4 > 3", "3 + 1 > 3")),
                chain(expect("true", "4 > 3")));
    }

    @Test
    void simplifyAppliesLongSubstitutionChainBeforeReachingFixedPoint() {
        VCImplication implication = vc("∀x:int. x == 1", "∀y:int. y == x + 1", "∀z:int. z == y + 1", "z == 3");

        assertSimplificationResults(VCSimplification::simplifyOnce, implication,
                chain(expect("y == 1 + 1", "∀x:int. y == x + 1"), expect("z == y + 1"), expect("z == 3")),
                chain(expect("z == 1 + 1 + 1", "∀y:int. z == y + 1"), expect("z == 3")),
                chain(expect("1 + 1 + 1 == 3", "∀z:int. z == 3")), chain(expect("2 + 1 == 3", "1 + 1 + 1 == 3")),
                chain(expect("3 == 3", "2 + 1 == 3")), chain(expect("true", "3 == 3")));
    }

    @Test
    void simplifyCombinesSubstitutionAndNestedFoldingAcrossFixedPoint() {
        VCImplication implication = vc("∀x:int. x == 1", "∀y:int. y == x + 2", "y - 1 == 2");

        assertSimplificationResults(VCSimplification::simplifyOnce, implication,
                chain(expect("y == 1 + 2", "∀x:int. y == x + 2"), expect("y - 1 == 2")),
                chain(expect("1 + 2 - 1 == 2", "∀y:int. y - 1 == 2")), chain(expect("3 - 1 == 2", "1 + 2 - 1 == 2")),
                chain(expect("2 == 2", "3 - 1 == 2")), chain(expect("true", "2 == 2")));
    }

    @Test
    void simplifyStopsAfterSubstitutionWhenOnlyNegativeLiteralShapeChanges() {
        VCImplication implication = vc("∀x:int. x == a + 0", "x >= -3");

        assertSimplificationResults(VCSimplification::simplifyOnce, implication,
                chain(expect("a + 0 >= -3", "∀x:int. x >= -3")));
    }

    @Test
    void simplifyLeavesUnchangedVcAsPlainPredicates() {
        VCImplication implication = vc("x > 0", "y > x");

        VCSimplificationResult result = VCSimplification.simplifyOnce(implication);

        assertSimplifiedVC(result.getImplication(), expect("x > 0"), expect("y > x"));
        assertNull(result.getOrigin());
    }

    @Test
    void simplifyOnceStoresACompleteClonedOriginChain() {
        VCImplication implication = vc("x > 0", "y + 0 > x");

        VCSimplificationResult result = VCSimplification.simplifyOnce(implication);
        implication.getNext().setRefinement(vc("changed").getRefinement());

        assertSimplifiedVC(result.getImplication(), expect("x > 0"), expect("y > x"));
        assertSimplifiedVC(result.getOrigin().getImplication(), expect("x > 0"), expect("y + 0 > x"));
    }

    @Test
    void fixedPointHistoryLinksEverySuccessfulStep() {
        VCImplication implication = vc("∀x:int. x == 1 + 2", "x + 0 > 2");

        VCSimplificationResult result = VCSimplification.simplifyToFixedPoint(implication);
        int historyLength = 0;
        for (VCSimplificationResult current = result.getOrigin(); current != null; current = current.getOrigin())
            historyLength++;

        assertEquals(4, historyLength);
        assertSimplifiedVC(result.getImplication(), expect("true"));
    }
}
