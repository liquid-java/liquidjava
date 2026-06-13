package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.*;
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

        assertSimplificationSteps(VCSimplification::simplifyOnce, implication,
                chain(expect("1 + 2 > 2", "∀x:int. x > 2")), chain(expect("3 > 2", "∀x:int. x > 2")),
                chain(expect("true", "∀x:int. x > 2")));
    }

    @Test
    void simplifyOnceDoesNotFoldAfterSubstitutionInSameStep() {
        VCImplication implication = vc("∀x:int. x == 1 + 2", "x == 3");

        assertSimplificationSteps(VCSimplification::simplifyOnce, implication,
                chain(expect("1 + 2 == 3", "∀x:int. x == 3")), chain(expect("3 == 3", "∀x:int. x == 3")),
                chain(expect("true", "∀x:int. x == 3")));
    }

    @Test
    void simplifyOnceAppliesFoldingWhenNoSubstitutionIsAvailable() {
        VCImplication implication = vc("1 + 2 > 2");

        assertSimplificationSteps(VCSimplification::simplifyOnce, implication, chain(expect("3 > 2", "1 + 2 > 2")),
                chain(expect("true", "1 + 2 > 2")));
    }

    @Test
    void simplifyOnceAppliesFoldingBeforeArithmeticSimplification() {
        VCImplication implication = vc("1 + 2 + x + 0 > 0");

        assertSimplificationSteps(VCSimplification::simplifyOnce, implication,
                chain(expect("3 + x + 0 > 0", "1 + 2 + x + 0 > 0")));
    }

    @Test
    void simplifyOnceAppliesArithmeticWhenNoSubstitutionOrFoldingIsAvailable() {
        VCImplication implication = vc("x + 0 > 0");

        assertSimplificationSteps(VCSimplification::simplifyOnce, implication, chain(expect("x > 0", "x + 0 > 0")));
    }

    @Test
    void simplifyKeepsApplyingStepsUntilFixedPoint() {
        VCImplication implication = vc("∀x:int. x == 1 + 2", "x + 1 > 3");

        assertSimplificationSteps(VCSimplification::simplifyOnce, implication,
                chain(expect("1 + 2 + 1 > 3", "∀x:int. x + 1 > 3")), chain(expect("3 + 1 > 3", "∀x:int. x + 1 > 3")),
                chain(expect("4 > 3", "∀x:int. x + 1 > 3")), chain(expect("true", "∀x:int. x + 1 > 3")));
    }

    @Test
    void simplifyAppliesMultipleSubstitutionsBeforeReachingFixedPoint() {
        VCImplication implication = vc("∀x:int. x == 3", "∀y:int. y == x + 1", "y > x");

        assertSimplificationSteps(VCSimplification::simplifyOnce, implication,
                chain(expect("y == 3 + 1", "∀x:int. y == x + 1"), expect("y > 3", "∀x:int. y > x")),
                chain(expect("3 + 1 > 3", "∀y:int. y > x")), chain(expect("4 > 3", "∀y:int. y > x")),
                chain(expect("true", "∀y:int. y > x")));
    }

    @Test
    void simplifyAppliesLongSubstitutionChainBeforeReachingFixedPoint() {
        VCImplication implication = vc("∀x:int. x == 1", "∀y:int. y == x + 1", "∀z:int. z == y + 1", "z == 3");

        assertSimplificationSteps(VCSimplification::simplifyOnce, implication,
                chain(expect("y == 1 + 1", "∀x:int. y == x + 1"), expect("z == y + 1", "∀z:int. z == y + 1"),
                        expect("z == 3", "z == 3")),
                chain(expect("z == 1 + 1 + 1", "∀y:int. z == y + 1"), expect("z == 3", "z == 3")),
                chain(expect("1 + 1 + 1 == 3", "∀z:int. z == 3")), chain(expect("2 + 1 == 3", "∀z:int. z == 3")),
                chain(expect("3 == 3", "∀z:int. z == 3")), chain(expect("true", "∀z:int. z == 3")));
    }

    @Test
    void simplifyCombinesSubstitutionAndNestedFoldingAcrossFixedPoint() {
        VCImplication implication = vc("∀x:int. x == 1", "∀y:int. y == x + 2", "y - 1 == 2");

        assertSimplificationSteps(VCSimplification::simplifyOnce, implication,
                chain(expect("y == 1 + 2", "∀x:int. y == x + 2"), expect("y - 1 == 2", "y - 1 == 2")),
                chain(expect("1 + 2 - 1 == 2", "∀y:int. y - 1 == 2")),
                chain(expect("3 - 1 == 2", "∀y:int. y - 1 == 2")), chain(expect("2 == 2", "∀y:int. y - 1 == 2")),
                chain(expect("true", "∀y:int. y - 1 == 2")));
    }

    @Test
    void simplifyStopsAfterSubstitutionWhenOnlyNegativeLiteralShapeChanges() {
        VCImplication implication = vc("∀x:int. x == a + 0", "x >= -3");

        assertSimplificationSteps(VCSimplification::simplifyOnce, implication,
                chain(expect("a + 0 >= -3", "∀x:int. x >= -3")));
    }

    @Test
    void simplifyLeavesUnchangedVcAsPlainPredicates() {
        VCImplication implication = vc("x > 0", "y > x");

        assertSimplificationSteps(VCSimplification::simplifyOnce, implication,
                chain(expect("x > 0", "x > 0"), expect("y > x", "y > x")));
    }
}
