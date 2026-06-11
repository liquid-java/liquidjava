package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertNull;

import liquidjava.processor.VCImplication;
import org.junit.jupiter.api.Test;

class VCSubstitutionTest {

    @Test
    void applyReturnsNullForNullImplication() {
        assertNull(VCSubstitution.apply(null));
    }

    @Test
    void substitutesBinderEqualityIntoWholeChain() {
        VCImplication implication = vc("∀x:int. x == 3", "x > 0");

        assertSimplificationSteps(VCSubstitution::apply, implication, "3 > 0");
    }

    @Test
    void substitutesReverseBinderEquality() {
        VCImplication implication = vc("∀x:int. 3 == x", "x > 0");

        assertSimplificationSteps(VCSubstitution::apply, implication, "3 > 0");
    }

    @Test
    void substitutesCompoundKnownValue() {
        VCImplication implication = vc("∀x:int. x == y + 1", "x > y");

        assertSimplificationSteps(VCSubstitution::apply, implication, "y + 1 > y");
    }

    @Test
    void substitutesOnlyWholeVariableReferences() {
        VCImplication implication = vc("∀x:int. x == 3", "xx > x");

        assertSimplificationSteps(VCSubstitution::apply, implication, "xx > 3");
    }

    @Test
    void substitutesEveryOccurrenceInPredicate() {
        VCImplication implication = vc("∀x:int. x == 2", "x + x > 0");

        assertSimplificationSteps(VCSubstitution::apply, implication, "2 + 2 > 0");
    }

    @Test
    void preservesRemainingBinderAfterSubstitution() {
        VCImplication implication = vc("∀x:int. x == 3", "∀y:int. y > x", "y > 0");

        assertSimplificationSteps(VCSubstitution::apply, implication, "∀y:int. y > 3 -> y > 0");
    }

    @Test
    void removesSourceNodeWhenItIsLastInChain() {
        VCImplication implication = vc("x > 0", "∀y:int. y == 1");

        assertSimplificationSteps(VCSubstitution::apply, implication, "x > 0");
    }

    @Test
    void usesFirstSubstitutionFoundInChain() {
        VCImplication implication = vc("∀x:int. x > 0", "∀y:int. y == 4", "x + y > 0");

        assertSimplificationSteps(VCSubstitution::apply, implication, "∀x:int. x > 0 -> x + 4 > 0");
    }

    @Test
    void substitutesInnerKnownValueAcrossNestedImplications() {
        VCImplication implication = vc("∀x:int. true", "∀y:int. y == 1", "∀z:int. z > y", "y + z > 0");

        assertSimplificationSteps(VCSubstitution::apply, implication, "∀x:int. true -> ∀z:int. z > 1 -> 1 + z > 0");
    }

    @Test
    void substitutesOuterKnownValueIntoNestedBinderRefinements() {
        VCImplication implication = vc("∀x:int. x == 3", "∀y:int. y == x + 1", "y > x");

        assertSimplificationSteps(VCSubstitution::apply, implication, "∀y:int. y == 3 + 1 -> y > 3", "3 + 1 > 3");
    }

    @Test
    void ignoresRecursiveBinderEquality() {
        VCImplication implication = vc("∀x:int. x == x + 1", "x > 0");

        assertSimplificationSteps(VCSubstitution::apply, implication, "∀x:int. x == x + 1 -> x > 0");
    }

    @Test
    void ignoresNonEqualityBinderRefinement() {
        VCImplication implication = vc("∀x:int. x > 3", "x > 0");

        assertSimplificationSteps(VCSubstitution::apply, implication, "∀x:int. x > 3 -> x > 0");
    }

    @Test
    void ignoresDerivedBinderEquality() {
        VCImplication implication = vc("∀x:int. x + 1 == 3", "x > 0");

        assertSimplificationSteps(VCSubstitution::apply, implication, "∀x:int. x + 1 == 3 -> x > 0");
    }

    @Test
    void ignoresEqualityWithoutBinder() {
        VCImplication implication = vc("x == 3", "x > 0");

        assertSimplificationSteps(VCSubstitution::apply, implication, "x == 3 -> x > 0");
    }
}
