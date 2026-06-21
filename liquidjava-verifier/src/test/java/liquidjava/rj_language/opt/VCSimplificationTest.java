package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertNull;

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
        assertSimplificationSteps(vc("∀x:int. x == 1 + 2", "x > 2"), step("1 + 2 > 2"), step("3 > 2"), step("true"));
    }

    @Test
    void simplifyOnceDoesNotFoldAfterSubstitutionInSameStep() {
        assertSimplificationSteps(vc("∀x:int. x == 1 + 2", "x == 3"), step("1 + 2 == 3"), step("3 == 3"), step("true"));
    }

    @Test
    void simplifyOnceAppliesSubstitutionBeforeBinderSimplification() {
        assertSimplificationSteps(vc("∀x:int. x == 3", "∀y:int. true", "x > 0"), step("true", "3 > 0"), step("3 > 0"),
                step("true"));
    }

    @Test
    void simplifyCombinesSubstitutionWithArithmeticAndLogicalSimplification() {
        assertSimplificationSteps(vc("∀x:int. x == y + 0", "x > 0 && true"), step("y + 0 > 0 && true"),
                step("y > 0 && true"), step("y > 0"));
    }

    @Test
    void simplifyOnceAppliesBinderSimplificationBeforeFolding() {
        assertSimplificationSteps(vc("∀x:int. true", "1 + 2 > 0"), step("1 + 2 > 0"), step("3 > 0"));
    }

    @Test
    void simplifyOnceAppliesBinderSimplificationBeforeLogicalSimplification() {
        assertSimplificationSteps(vc("∀x:int. true", "y && true"), step("y && true"), step("y"));
    }

    @Test
    void simplifyOnceAppliesBinderSimplificationBeforeArithmeticSimplification() {
        assertSimplificationSteps(vc("∀x:int. true", "y + 0 > 0"), step("y + 0 > 0"), step("y > 0"));
    }

    @Test
    void simplifyOnceAppliesFoldingWhenNoSubstitutionIsAvailable() {
        assertSimplificationSteps(vc("1 + 2 > 2"), step("3 > 2"), step("true"));
    }

    @Test
    void simplifyOnceAppliesFoldingBeforeArithmeticSimplification() {
        assertSimplificationSteps(vc("1 + 2 + x + 0 > 0"), step("3 + x + 0 > 0"), step("3 + x > 0"));
    }

    @Test
    void simplifyCombinesFoldingArithmeticAndLogicalSimplification() {
        assertSimplificationSteps(vc("1 + 2 + x + 0 == 3 + x"), step("3 + x + 0 == 3 + x"), step("3 + x == 3 + x"),
                step("true"));
    }

    @Test
    void simplifyOnceAppliesArithmeticWhenNoSubstitutionOrFoldingIsAvailable() {
        assertSimplificationSteps(vc("x + 0 > 0"), step("x > 0"));
    }

    @Test
    void simplifyOnceAppliesArithmeticBeforeLogicalSimplification() {
        assertSimplificationSteps(vc("x + 0 == x"), step("x == x"), step("true"));
    }

    @Test
    void simplifyOnceAppliesArithmeticBeforeFoldingOnNextStep() {
        assertSimplificationSteps(vc("x - x == 0"), step("0 == 0"), step("true"));
    }

    @Test
    void simplifyOnceAppliesLogicalWhenNoEarlierSimplificationIsAvailable() {
        assertSimplificationSteps(vc("x && true"), step("x"));
    }

    @Test
    void simplifyUsesFoldingToEnableSubstitutionOnNextStep() {
        assertSimplificationSteps(vc("∀x:int. (x) == 3", "x > 0"), step("x == 3", "x > 0"), step("3 > 0"),
                step("true"));
    }

    @Test
    void simplifyUsesArithmeticToEnableSubstitutionOnNextStep() {
        assertSimplificationSteps(vc("∀x:int. x + 0 == 3", "x > 0"), step("x == 3", "x > 0"), step("3 > 0"),
                step("true"));
    }

    @Test
    void simplifyUsesLogicalSimplificationToEnableSubstitutionOnNextStep() {
        assertSimplificationSteps(vc("∀x:int. true && x == 3", "x > 0"), step("x == 3", "x > 0"), step("3 > 0"),
                step("true"));
    }

    @Test
    void simplifyUsesFoldingToEnableBinderSimplificationOnNextStep() {
        assertSimplificationSteps(vc("∀x:int. 1 > 2", "y > 0"), step("false", "y > 0"), step("true"));
    }

    @Test
    void simplifyUsesArithmeticAndLogicalSimplificationToEnableBinderRemoval() {
        assertSimplificationSteps(vc("∀x:int. x + 0 == x", "y > 0"), step("x == x", "y > 0"), step("true", "y > 0"),
                step("y > 0"));
    }

    @Test
    void simplifyAppliesLogicalStepsUntilFixedPoint() {
        assertSimplificationSteps(vc("x && true && true"), step("x && true"), step("x"));
    }

    @Test
    void simplifyKeepsApplyingStepsUntilFixedPoint() {
        assertSimplificationSteps(vc("∀x:int. x == 1 + 2", "x + 1 > 3"), step("1 + 2 + 1 > 3"), step("3 + 1 > 3"),
                step("4 > 3"), step("true"));
    }

    @Test
    void simplifyRemovesTrueBindersOverMultipleSteps() {
        assertSimplificationSteps(vc("∀x:int. true", "∀y:int. true", "z > 0"), step("true", "z > 0"), step("z > 0"));
    }

    @Test
    void simplifyAppliesMultipleSubstitutionsBeforeReachingFixedPoint() {
        assertSimplificationSteps(vc("∀x:int. x == 3", "∀y:int. y == x + 1", "y > x"), step("y == 3 + 1", "y > 3"),
                step("3 + 1 > 3"), step("4 > 3"), step("true"));
    }

    @Test
    void simplifyAppliesLongSubstitutionChainBeforeReachingFixedPoint() {
        assertSimplificationSteps(vc("∀x:int. x == 1", "∀y:int. y == x + 1", "∀z:int. z == y + 1", "z == 3"),
                step("y == 1 + 1", "z == y + 1", "z == 3"), step("z == 1 + 1 + 1", "z == 3"), step("1 + 1 + 1 == 3"),
                step("2 + 1 == 3"), step("3 == 3"), step("true"));
    }

    @Test
    void simplifyCombinesSubstitutionAndNestedFoldingAcrossFixedPoint() {
        assertSimplificationSteps(vc("∀x:int. x == 1", "∀y:int. y == x + 2", "y - 1 == 2"),
                step("y == 1 + 2", "y - 1 == 2"), step("1 + 2 - 1 == 2"), step("3 - 1 == 2"), step("2 == 2"),
                step("true"));
    }

    @Test
    void simplifyStopsAfterSubstitutionWhenOnlyNegativeLiteralShapeChanges() {
        assertSimplificationSteps(vc("∀x:int. x == a + 0", "x >= -3"), step("a + 0 >= -3"));
    }

    @Test
    void simplifyLeavesUnchangedVcAsPlainPredicates() {
        assertSimplificationSteps(vc("x > 0", "y > x"), step("x > 0", "y > x"));
    }
}
