package liquidjava.api.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import liquidjava.api.CommandLineLauncher;
import liquidjava.diagnostics.Diagnostics;
import liquidjava.diagnostics.errors.LJError;
import liquidjava.diagnostics.errors.RefinementError;
import liquidjava.diagnostics.errors.StateRefinementError;
import liquidjava.processor.VCImplication;
import liquidjava.rj_language.ast.formatter.VariableFormatter;
import liquidjava.rj_language.opt.VCSimplificationResult;

/**
 * Checks the rows of Found: a binder refined only by true is shown only when the error needs it, and fields are written
 * as in code
 */
class TestFoundBinders {

    private static final String TEST_SUITE = "../liquidjava-example/src/main/java/testSuite/";

    private static Map<Integer, Shown> basket;
    private static Map<Integer, Shown> fieldState;
    private static Map<Integer, Shown> pathDependentState;
    private static Map<Integer, Shown> otherObjectWrite;

    /**
     * What the user is shown for an error, without the instance counters that depend on verification order: the
     * simplified Found, the original Found the panel steps back to, the simplification of each step and the raw binder
     * names of the original
     */
    private record Shown(List<String> rows, String message, String counterexample, List<String> originalRows,
            List<String> steps, List<String> originalBinders) {
    }

    @BeforeAll
    static void verify() {
        // shown as the IDE gets it: right after the verification, before the next one starts
        basket = verify("classes/found_true_binders_error");
        fieldState = verify("classes/found_field_state_error");
        pathDependentState = verify("ErrorStateHintPathDependent.java");
        otherObjectWrite = verify("ErrorOtherObjectFieldWrite.java");
    }

    @Test
    void basketHasOneErrorPerCase() {
        assertEquals(List.of(25, 30, 36, 49, 56, 62, 67, 76, 82, 88, 96, 104, 111, 117),
                new ArrayList<>(basket.keySet()));
    }

    @Test
    void trueFieldRowOfTheIssueIsDropped() {
        Shown shown = basket.get(25);
        assertEquals(List.of("∀bound bound == size(this.items) && bound >= 0"), shown.rows());
        assertEquals("bound == size(this.items) && bound >= 0 is not a subtype of bound > 0", shown.message());
        assertEquals("Counterexample: bound == 0", shown.counterexample());
    }

    @Test
    void steppingBackReachesTheOriginalWithTheDroppedRow() {
        Shown shown = basket.get(25);
        assertEquals(
                List.of("∀this.items true", "∀items items == this.items", "∀bound bound == size(items) && bound >= 0"),
                shown.originalRows());
        assertEquals(List.of("Substitution", "Binder"), shown.steps());
        assertEquals(List.of("this#testSuite.classes.found_true_binders_error.Basket.items", "#items_", "#bound_"),
                shown.originalBinders());
    }

    @Test
    void trueParameterRowIsDropped() {
        assertEquals(List.of("∀bound bound == size(list) && bound >= 0"), basket.get(30).rows());
    }

    @Test
    void trueCallResultRowIsDropped() {
        assertEquals(List.of("∀bound bound == size(copyItems) && bound >= 0"), basket.get(36).rows());
    }

    @Test
    void trueLocalRowIsDropped() {
        assertEquals(List.of("∀bound bound == size(local) && bound >= 0"), basket.get(49).rows());
    }

    @Test
    void severalTrueFieldRowsAreDroppedAndReadWithOrWithoutThisAlike() {
        assertEquals(
                List.of("∀first first == size(this.items) && first >= 0",
                        "∀second second == size(this.extras) && second >= 0", "∀bound bound == first + second"),
                basket.get(56).rows());
    }

    @Test
    void trueRowLinkedToExpectedThroughALaterRowIsKept() {
        Shown shown = basket.get(62);
        assertEquals(List.of("∀x true", "∀y y == x + 1"), shown.rows());
        assertEquals("Counterexample: x == -1 && y == 0", shown.counterexample());
    }

    @Test
    void trueRowOfTheExpectedVariableIsKept() {
        Shown shown = basket.get(67);
        assertEquals(List.of("∀x true"), shown.rows());
        assertEquals("true is not a subtype of x > 0", shown.message());
    }

    @Test
    void staticFieldIsWrittenWithItsClass() {
        Shown shown = basket.get(76);
        assertEquals(List.of("∀Basket.served true", "∀count count == Basket.served + 1"), shown.rows());
        assertEquals("count == Basket.served + 1 is not a subtype of count > 0", shown.message());
        assertEquals("Counterexample: Basket.served == -1 && count == 0", shown.counterexample());
    }

    @Test
    void fieldReadWithThisIsWrittenAsInCode() {
        Shown shown = basket.get(82);
        assertEquals(List.of("∀this.amount true", "∀a a == this.amount"), shown.rows());
        assertEquals("a == this.amount is not a subtype of a > 0", shown.message());
        assertEquals("Counterexample: this.amount == 0 && a == 0", shown.counterexample());
    }

    @Test
    void fieldReadWithoutThisIsWrittenTheSameWay() {
        Shown withThis = basket.get(82);
        Shown withoutThis = basket.get(88);
        assertEquals(withThis.rows(), withoutThis.rows());
        assertEquals(withThis.message(), withoutThis.message());
        assertEquals(withThis.counterexample(), withoutThis.counterexample());
    }

    @Test
    void fieldOfAnInnerClassIsWrittenAsInCode() {
        assertEquals(List.of("∀bound bound == size(this.names) && bound >= 0"), basket.get(96).rows());
    }

    @Test
    void trueRowUsedOnlyByAnUnrelatedGuardIsDropped() {
        Shown shown = basket.get(104);
        assertEquals(List.of("∀#fresh !(port < 0)", "∀t true"), shown.rows());
        assertEquals(List.of("∀port true", "∀#fresh !(port < 0)", "∀t true"), shown.originalRows());
    }

    @Test
    void trueRowsOfACombinedGuardAreNotLinkedToExpected() {
        assertEquals(List.of("∀#fresh !(p || q)", "∀t true"), basket.get(111).rows());
    }

    @Test
    void keptTrueRowComesBeforeTheRowUsingIt() {
        assertEquals(List.of("∀Basket.served true", "∀i true", "∀#fresh i < Basket.served"), basket.get(117).rows());
    }

    @Test
    void trueRowDecidingAPathDependentStateIsKept() {
        Shown shown = pathDependentState.get(25);
        assertEquals(List.of("∀ready true", "∀f ready ? open(f) : created(f)"), shown.rows());
    }

    @Test
    void writesToAnotherObjectsFieldAreCheckedAsOnMain() {
        // other.level++ and other.level += 1 verify, other.level-- and the write through an alias of this are reported
        assertEquals(List.of(11, 23, 31, 35), new ArrayList<>(otherObjectWrite.keySet()));
        assertEquals("k == 7", otherObjectWrite.get(31).message().replaceAll(".* is not a subtype of ", ""));
    }

    @Test
    void writeToAnotherObjectsFieldIsWrittenWithThatObject() {
        assertEquals("other.level == -1 is not a subtype of other.level >= 0", otherObjectWrite.get(11).message());
        assertEquals(List.of("∀this.level this.level >= 0", "∀other.level other.level == this.level - 1"),
                otherObjectWrite.get(23).rows());
        assertEquals("this.level == -1 is not a subtype of this.level >= 0", otherObjectWrite.get(35).message());
    }

    @Test
    void fieldRowHoldingAStateIsKept() {
        assertEquals(List.of(14), new ArrayList<>(fieldState.keySet()));
        Shown shown = fieldState.get(14);
        assertEquals(
                List.of("∀#fresh !(intrNumber < 0 || intrNumber >= 67)", "∀this.interrupts empty(this.interrupts)"),
                shown.rows());
        assertEquals("found !(intrNumber < 0 || intrNumber >= 67) && empty(this.interrupts) but expected "
                + "maybeElements(this.interrupts)", shown.message());
    }

    private static Map<Integer, Shown> verify(String test) {
        CommandLineLauncher.launch(TEST_SUITE + test);
        Map<Integer, Shown> shown = new TreeMap<>();
        for (LJError error : Diagnostics.getInstance().getErrors()) {
            VCSimplificationResult found = error instanceof RefinementError refinementError ? refinementError.getFound()
                    : assertInstanceOf(StateRefinementError.class, error).getFoundSimplification();
            List<String> steps = new ArrayList<>();
            VCSimplificationResult original = found;
            for (; original.getOrigin() != null; original = original.getOrigin())
                steps.add(0, original.getSimplification());
            List<String> originalBinders = original.getBinders().stream().map(b -> b.replaceAll("[0-9]+$", ""))
                    .toList();
            shown.put(error.getPosition().getLine(),
                    new Shown(rows(found.getImplication()), plain(error.getMessage()),
                            error.getCounterexampleStr() == null ? null : plain(error.getCounterexampleStr()),
                            rows(original.getImplication()), steps, originalBinders));
        }
        return shown;
    }

    // the rows of Found as the IDE writes them: binder name, then its refinement
    private static List<String> rows(VCImplication found) {
        List<String> rows = new ArrayList<>();
        for (VCImplication current = found; current != null; current = current.getNext()) {
            String refinement = current.getRefinement().getExpression().toDisplayString();
            rows.add(plain(current.hasBinder() ? "∀" + VariableFormatter.format(current.getName()) + " " + refinement
                    : refinement));
        }
        return rows;
    }

    private static String plain(String text) {
        return text.replaceAll("[⁰¹²³⁴⁵⁶⁷⁸⁹]", "");
    }
}
