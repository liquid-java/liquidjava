package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

        VCImplication result = VCSubstitution.apply(implication);

        assertSimplifiedVC(result, simplified("3 > 0", "∀x:int. x > 0"));
        assertSimplificationSteps(result, VCSimplification::simplifyOnce, simplified("true", "∀x:int. x > 0"));
    }

    @Test
    void substitutesReverseBinderEquality() {
        VCImplication implication = vc("∀x:int. 3 == x", "x > 0");

        VCImplication result = VCSubstitution.apply(implication);

        assertSimplifiedVC(result, simplified("3 > 0", "∀x:int. x > 0"));
        assertSimplificationSteps(result, VCSimplification::simplifyOnce, simplified("true", "∀x:int. x > 0"));
    }

    @Test
    void substitutesCompoundKnownValue() {
        VCImplication implication = vc("∀x:int. x == y + 1", "x > y");

        VCImplication result = VCSubstitution.apply(implication);

        assertSimplifiedVC(result, simplified("y + 1 > y", "∀x:int. x > y"));
    }

    @Test
    void substitutesOnlyWholeVariableReferences() {
        VCImplication implication = vc("∀x:int. x == 3", "xx > x");

        VCImplication result = VCSubstitution.apply(implication);

        assertSimplifiedVC(result, simplified("xx > 3", "∀x:int. xx > x"));
    }

    @Test
    void substitutesEveryOccurrenceInPredicate() {
        VCImplication implication = vc("∀x:int. x == 2", "x + x > 0");

        VCImplication result = VCSubstitution.apply(implication);

        assertSimplifiedVC(result, simplified("2 + 2 > 0", "∀x:int. x + x > 0"));
        assertSimplificationSteps(result, VCSimplification::simplifyOnce, simplified("4 > 0", "∀x:int. x + x > 0"),
                simplified("true", "∀x:int. x + x > 0"));
    }

    @Test
    void preservesRemainingBinderAfterSubstitution() {
        VCImplication implication = vc("∀x:int. x == 3", "∀y:int. y > x", "y > 0");

        VCImplication result = VCSubstitution.apply(implication);

        assertEquals("y", result.getName());
        assertEquals("y > 3", result.getRefinement().toString());
        assertVC(result.getNext(), "y > 0");
    }

    @Test
    void removesSourceNodeWhenItIsLastInChain() {
        VCImplication implication = vc("x > 0", "∀y:int. y == 1");

        VCImplication result = VCSubstitution.apply(implication);

        assertVC(result, "x > 0");
    }

    @Test
    void usesFirstSubstitutionFoundInChain() {
        VCImplication implication = vc("∀x:int. x > 0", "∀y:int. y == 4", "x + y > 0");

        VCImplication result = VCSubstitution.apply(implication);

        assertVC(result, "x > 0", "x + 4 > 0");
        assertEquals(VCImplication.class, result.getClass());
        assertSimplifiedVC(result.getNext(), simplified("x + 4 > 0", "∀y:int. x + y > 0"));
    }

    @Test
    void substitutesInnerKnownValueAcrossNestedImplications() {
        VCImplication implication = vc("∀x:int. true", "∀y:int. y == 1", "∀z:int. z > y", "y + z > 0");

        VCImplication result = VCSubstitution.apply(implication);

        assertVC(result, "true", "z > 1", "1 + z > 0");
        assertEquals(VCImplication.class, result.getClass());
        assertSimplifiedVC(result.getNext(), simplified("z > 1", "∀y:int. z > y"),
                simplified("1 + z > 0", "∀y:int. y + z > 0"));
    }

    @Test
    void substitutesOuterKnownValueIntoNestedBinderRefinements() {
        VCImplication implication = vc("∀x:int. x == 3", "∀y:int. y == x + 1", "y > x");

        VCImplication result = VCSubstitution.apply(implication);

        assertSimplifiedVC(result, simplified("y == 3 + 1", "∀x:int. y == x + 1"),
                simplified("y > 3", "∀x:int. y > x"));
        assertSimplificationSteps(result, VCSimplification::simplifyOnce, simplified("3 + 1 > 3", "∀y:int. y > x"),
                simplified("4 > 3", "∀y:int. y > x"), simplified("true", "∀y:int. y > x"));
    }

    @Test
    void ignoresRecursiveBinderEquality() {
        VCImplication implication = vc("∀x:int. x == x + 1", "x > 0");

        VCImplication result = VCSubstitution.apply(implication);

        assertVC(result, "x == x + 1", "x > 0");
    }

    @Test
    void ignoresNonEqualityBinderRefinement() {
        VCImplication implication = vc("∀x:int. x > 3", "x > 0");

        VCImplication result = VCSubstitution.apply(implication);

        assertVC(result, "x > 3", "x > 0");
    }

    @Test
    void ignoresDerivedBinderEquality() {
        VCImplication implication = vc("∀x:int. x + 1 == 3", "x > 0");

        VCImplication result = VCSubstitution.apply(implication);

        assertVC(result, "x + 1 == 3", "x > 0");
    }

    @Test
    void ignoresEqualityWithoutBinder() {
        VCImplication implication = vc("x == 3", "x > 0");

        VCImplication result = VCSubstitution.apply(implication);

        assertVC(result, "x == 3", "x > 0");
    }

}
