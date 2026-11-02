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

/**
 * Checks the rows of Found: a binder refined only by true is shown only when the error needs it, and fields are written
 * as in code
 */
class TestFoundBinders {

    private static final String TEST_SUITE = "../liquidjava-example/src/main/java/testSuite/";

    private static Map<Integer, Shown> basket;
    private static Map<Integer, Shown> fieldState;

    /** What the user is shown for an error, without the instance counters that depend on verification order */
    private record Shown(List<String> rows, String message, String counterexample) {
    }

    @BeforeAll
    static void verify() {
        // shown as the IDE gets it: right after the verification, before the next one starts
        basket = verify("classes/found_true_binders_error");
        fieldState = verify("classes/found_field_state_error");
    }

    @Test
    void basketHasOneErrorPerCase() {
        assertEquals(List.of(25, 30, 36, 49, 56, 62, 67, 76, 82, 88, 96), new ArrayList<>(basket.keySet()));
    }

    @Test
    void trueFieldRowOfTheIssueIsDropped() {
        Shown shown = basket.get(25);
        assertEquals(List.of("∀bound bound == size(this.items) && bound >= 0"), shown.rows());
        assertEquals("bound == size(this.items) && bound >= 0 is not a subtype of bound > 0", shown.message());
        assertEquals("Counterexample: bound == 0", shown.counterexample());
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
    void trueRowUsedLaterIsKeptWhenTheCounterexampleHasItsValue() {
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
        assertEquals(basket.get(82), basket.get(88));
    }

    @Test
    void fieldOfAnInnerClassIsWrittenAsInCode() {
        assertEquals(List.of("∀bound bound == size(this.names) && bound >= 0"), basket.get(96).rows());
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
        for (LJError error : Diagnostics.getInstance().getErrors())
            shown.put(error.getPosition().getLine(), new Shown(rows(error), plain(error.getMessage()),
                    error.getCounterexampleStr() == null ? null : plain(error.getCounterexampleStr())));
        return shown;
    }

    // the rows of Found as the IDE writes them: binder name, then its refinement
    private static List<String> rows(LJError error) {
        VCImplication found;
        if (error instanceof RefinementError refinementError)
            found = refinementError.getFound().getImplication();
        else
            found = assertInstanceOf(StateRefinementError.class, error).getFound();

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
