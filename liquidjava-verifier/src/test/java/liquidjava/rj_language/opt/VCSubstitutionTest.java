package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertNull;

import liquidjava.processor.VCImplication;
import org.junit.jupiter.api.Test;

class VCSubstitutionTest {

    private final VCSubstitution substitution = new VCSubstitution();

    @Test
    void applyReturnsNullForNullImplication() {
        assertNull(substitution.apply(null));
    }

    @Test
    void substitutesBinderEqualityIntoWholeChain() {
        VCImplication implication = vc("∀x:int. x == 3", "x > 0");

        assertSimplificationSteps(substitution::apply, implication, chain(expect("3 > 0", "∀x:int. x > 0")));
    }

    @Test
    void substitutesReverseBinderEquality() {
        VCImplication implication = vc("∀x:int. 3 == x", "x > 0");

        assertSimplificationSteps(substitution::apply, implication, chain(expect("3 > 0", "∀x:int. x > 0")));
    }

    @Test
    void substitutesCompoundKnownValue() {
        VCImplication implication = vc("∀x:int. x == y + 1", "x > y");

        assertSimplificationSteps(substitution::apply, implication, chain(expect("y + 1 > y", "∀x:int. x > y")));
    }

    @Test
    void substitutesOnlyWholeVariableReferences() {
        VCImplication implication = vc("∀x:int. x == 3", "xx > x");

        assertSimplificationSteps(substitution::apply, implication, chain(expect("xx > 3", "∀x:int. xx > x")));
    }

    @Test
    void substitutesEveryOccurrenceInPredicate() {
        VCImplication implication = vc("∀x:int. x == 2", "x + x > 0");

        assertSimplificationSteps(substitution::apply, implication, chain(expect("2 + 2 > 0", "∀x:int. x + x > 0")));
    }

    @Test
    void preservesRemainingBinderAfterSubstitution() {
        VCImplication implication = vc("∀x:int. x == 3", "∀y:int. y > x", "y > 0");

        assertSimplificationSteps(substitution::apply, implication,
                chain(expect("y > 3", "∀x:int. y > x"), expect("y > 0", "y > 0")));
    }

    @Test
    void removesSourceNodeWhenItIsLastInChain() {
        VCImplication implication = vc("x > 0", "∀y:int. y == 1");

        assertSimplificationSteps(substitution::apply, implication, chain(expect("x > 0", "x > 0")));
    }

    @Test
    void usesFirstSubstitutionFoundInChain() {
        VCImplication implication = vc("∀x:int. x > 0", "∀y:int. y == 4", "x + y > 0");

        assertSimplificationSteps(substitution::apply, implication,
                chain(expect("x > 0", "∀x:int. x > 0"), expect("x + 4 > 0", "∀y:int. x + y > 0")));
    }

    @Test
    void substitutesInnerKnownValueAcrossNestedImplications() {
        VCImplication implication = vc("∀x:int. true", "∀y:int. y == 1", "∀z:int. z > y", "y + z > 0");

        assertSimplificationSteps(substitution::apply, implication, chain(expect("true", "∀x:int. true"),
                expect("z > 1", "∀y:int. z > y"), expect("1 + z > 0", "∀y:int. y + z > 0")));
    }

    @Test
    void substitutesOuterKnownValueIntoNestedBinderRefinements() {
        VCImplication implication = vc("∀x:int. x == 3", "∀y:int. y == x + 1", "y > x");

        assertSimplificationSteps(substitution::apply, implication,
                chain(expect("y == 3 + 1", "∀x:int. y == x + 1"), expect("y > 3", "∀x:int. y > x")),
                chain(expect("3 + 1 > 3", "∀y:int. y > x")));
    }

    @Test
    void ignoresRecursiveBinderEquality() {
        VCImplication implication = vc("∀x:int. x == x + 1", "x > 0");

        assertSimplificationSteps(substitution::apply, implication,
                chain(expect("x == x + 1", "∀x:int. x == x + 1"), expect("x > 0", "x > 0")));
    }

    @Test
    void ignoresNonEqualityBinderRefinement() {
        VCImplication implication = vc("∀x:int. x > 3", "x > 0");

        assertSimplificationSteps(substitution::apply, implication,
                chain(expect("x > 3", "∀x:int. x > 3"), expect("x > 0", "x > 0")));
    }

    @Test
    void ignoresDerivedBinderEquality() {
        VCImplication implication = vc("∀x:int. x + 1 == 3", "x > 0");

        assertSimplificationSteps(substitution::apply, implication,
                chain(expect("x + 1 == 3", "∀x:int. x + 1 == 3"), expect("x > 0", "x > 0")));
    }

    @Test
    void ignoresEqualityWithoutBinder() {
        VCImplication implication = vc("x == 3", "x > 0");

        assertSimplificationSteps(substitution::apply, implication,
                chain(expect("x == 3", "x == 3"), expect("x > 0", "x > 0")));
    }
}
