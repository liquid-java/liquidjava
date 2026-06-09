package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import liquidjava.processor.VCImplication;
import org.junit.jupiter.api.Test;

class VCSubstitutionTest {

    @Test
    void applyOnceReturnsNullForNullImplication() {
        assertNull(VCSubstitution.applyOnce(null));
    }

    @Test
    void substitutesBinderEqualityIntoWholeChain() {
        VCImplication implication = vc("∀x:int. x == 3", "x > 0");

        VCImplication result = VCSubstitution.applyOnce(implication);

        assertSimplifiedVC(result, simplified("3 > 0", "x > 0", "x:int"));
    }

    @Test
    void substitutesReverseBinderEquality() {
        VCImplication implication = vc("∀x:int. 3 == x", "x > 0");

        VCImplication result = VCSubstitution.applyOnce(implication);

        assertSimplifiedVC(result, simplified("3 > 0", "x > 0", "x:int"));
    }

    @Test
    void substitutesCompoundKnownValue() {
        VCImplication implication = vc("∀x:int. x == y + 1", "x > y");

        VCImplication result = VCSubstitution.applyOnce(implication);

        assertSimplifiedVC(result, simplified("y + 1 > y", "x > y", "x:int"));
    }

    @Test
    void usesFirstSubstitutionFoundInChain() {
        VCImplication implication = vc("∀x:int. x > 0", "∀y:int. y == 4", "x + y > 0");

        VCImplication result = VCSubstitution.applyOnce(implication);

        assertSimplifiedVC(result, simplified("x > 0", "x > 0", ""), simplified("x + 4 > 0", "x + y > 0", "y:int"));
    }

    @Test
    void substitutesInnerKnownValueAcrossNestedImplications() {
        VCImplication implication = vc("∀x:int. true", "∀y:int. y == 1", "∀z:int. z > y", "y + z > 0");

        VCImplication result = VCSubstitution.applyOnce(implication);

        assertSimplifiedVC(result, simplified("true", "true", ""), simplified("z > 1", "z > y", "y:int"),
                simplified("1 + z > 0", "y + z > 0", "y:int"));
    }

    @Test
    void substitutesOuterKnownValueIntoNestedBinderRefinements() {
        VCImplication implication = vc("∀x:int. x == 3", "∀y:int. y == x + 1", "y > x");

        VCImplication result = VCSubstitution.applyOnce(implication);

        assertSimplifiedVC(result, simplified("y == 3 + 1", "y == x + 1", "x:int"),
                simplified("y > 3", "y > x", "x:int"));
    }

    @Test
    void ignoresRecursiveBinderEquality() {
        VCImplication implication = vc("∀x:int. x == x + 1", "x > 0");

        VCImplication result = VCSubstitution.applyOnce(implication);

        assertNotSame(implication, result);
        assertVC(result, "x == x + 1", "x > 0");
    }

    @Test
    void ignoresNonEqualityBinderRefinement() {
        VCImplication implication = vc("∀x:int. x > 3", "x > 0");

        VCImplication result = VCSubstitution.applyOnce(implication);

        assertNotSame(implication, result);
        assertVC(result, "x > 3", "x > 0");
    }

    @Test
    void ignoresEqualityWithoutBinder() {
        VCImplication implication = vc("x == 3", "x > 0");

        VCImplication result = VCSubstitution.applyOnce(implication);

        assertVC(result, "x == 3", "x > 0");
    }
}
