package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Ite;
import liquidjava.rj_language.ast.Var;
import org.junit.jupiter.api.Test;

class VCKnownEnumValuesTest {

    @Test
    void decidesOnlyTheExcludedConstant() {
        VCImplication implication = vc("∀mode:Mode. mode != Mode.Photo",
                "mode == Mode.Photo ? a : (mode == Mode.Video ? b : c)");
        assertSimplificationSteps(new VCKnownEnumValues(implication), implication,
                step("mode != Mode.Photo", "false ? a : (mode == Mode.Video ? b : c)"));
    }

    @Test
    void decidesConstantsReadInCode() {
        // code reads an enum constant as a variable named after it
        Var mode = new Var("mode"), photo = new Var("Mode.Photo"), video = new Var("Mode.Video");
        VCImplication implication = vc("∀mode:Mode. true", "true");
        implication.setRefinement(new Predicate(new BinaryExpression(mode, "==", photo)));
        implication.getNext().setRefinement(
                new Predicate(new Ite(new BinaryExpression(video, "==", mode), new Var("a"), new Var("b"))));
        assertSimplificationSteps(new VCKnownEnumValues(implication), implication,
                step("mode == Photo", "false ? a : b"));
    }

    @Test
    void foldsTheTernariesAFactDecides() {
        VCImplication simplified = VCSimplification
                .simplifyToFixedPoint(vc("∀mode:Mode. true", "mode == Mode.Video && (mode == Mode.Photo ? a : b)"))
                .getImplication();
        assertEquals("mode == Mode.Video && b", simplified.toPredicate().getExpression().toDisplayString());
    }
}
