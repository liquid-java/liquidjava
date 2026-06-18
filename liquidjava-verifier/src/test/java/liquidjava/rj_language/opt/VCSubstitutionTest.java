package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.*;
import org.junit.jupiter.api.Test;

class VCSubstitutionTest {

    private final VCSubstitution substitution = new VCSubstitution();

    @Test
    void substitutesBinderEqualityIntoWholeChain() {
        assertSimplificationSteps(substitution, vc("∀x:int. x == 3", "x > 0"), step("3 > 0"));
    }

    @Test
    void substitutesReverseBinderEquality() {
        assertSimplificationSteps(substitution, vc("∀x:int. 3 == x", "x > 0"), step("3 > 0"));
    }

    @Test
    void substitutesCompoundKnownValue() {
        assertSimplificationSteps(substitution, vc("∀x:int. x == y + 1", "x > y"), step("y + 1 > y"));
    }

    @Test
    void substitutesOnlyWholeVariableReferences() {
        assertSimplificationSteps(substitution, vc("∀x:int. x == 3", "xx > x"), step("xx > 3"));
    }

    @Test
    void substitutesEveryOccurrenceInPredicate() {
        assertSimplificationSteps(substitution, vc("∀x:int. x == 2", "x + x > 0"), step("2 + 2 > 0"));
    }

    @Test
    void preservesRemainingBinderAfterSubstitution() {
        assertSimplificationSteps(substitution, vc("∀x:int. x == 3", "∀y:int. y > x", "y > 0"), step("y > 3", "y > 0"));
    }

    @Test
    void keepsSourceNodeWhenItIsLastInChain() {
        assertSimplificationSteps(substitution, vc("x > 0", "∀y:int. y == 1"), step("x > 0", "y == 1"));
    }

    @Test
    void keepsReturnBinderWhenConclusionIsSeparate() {
        assertSimplificationSteps(substitution, vc("∀x:int. true", "∀#ret_8:int. #ret_8 == x + 1"),
                step("true", "#ret⁸ == x + 1"));
    }

    @Test
    void usesFirstSubstitutionFoundInChain() {
        assertSimplificationSteps(substitution, vc("∀x:int. x > 0", "∀y:int. y == 4", "x + y > 0"),
                step("x > 0", "x + 4 > 0"));
    }

    @Test
    void substitutesInnerKnownValueAcrossNestedImplications() {
        assertSimplificationSteps(substitution, vc("∀x:int. true", "∀y:int. y == 1", "∀z:int. z > y", "y + z > 0"),
                step("true", "z > 1", "1 + z > 0"));
    }

    @Test
    void substitutesOuterKnownValueIntoNestedBinderRefinements() {
        assertSimplificationSteps(substitution, vc("∀x:int. x == 3", "∀y:int. y == x + 1", "y > x"),
                step("y == 3 + 1", "y > 3"), step("3 + 1 > 3"));
    }

    @Test
    void ignoresRecursiveBinderEquality() {
        assertSimplificationSteps(substitution, vc("∀x:int. x == x + 1", "x > 0"), step("x == x + 1", "x > 0"));
    }

    @Test
    void ignoresNonEqualityBinderRefinement() {
        assertSimplificationSteps(substitution, vc("∀x:int. x > 3", "x > 0"), step("x > 3", "x > 0"));
    }

    @Test
    void ignoresDerivedBinderEquality() {
        assertSimplificationSteps(substitution, vc("∀x:int. x + 1 == 3", "x > 0"), step("x + 1 == 3", "x > 0"));
    }

    @Test
    void ignoresEqualityWithoutBinder() {
        assertSimplificationSteps(substitution, vc("x == 3", "x > 0"), step("x == 3", "x > 0"));
    }
}
