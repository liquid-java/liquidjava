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
import liquidjava.processor.VCImplication;
import liquidjava.rj_language.ast.formatter.DisplayNames;
import liquidjava.rj_language.ast.formatter.ExpressionFormatter;
import liquidjava.rj_language.opt.VCSimplificationResult;

/**
 * Checks what Found shows for the errors of {@code classes/found_binders_error}, whose {@code // Expect:} lines also
 * pin that writes to another object's field are accepted and rejected as on main
 */
class TestFoundBinders {

    private static final Map<Integer, Shown> ERRORS = new TreeMap<>();

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
        CommandLineLauncher.launch("../liquidjava-example/src/main/java/testSuite/classes/found_binders_error");
        for (LJError error : Diagnostics.getInstance().getErrors()) {
            VCSimplificationResult found = assertInstanceOf(RefinementError.class, error).getFound();
            List<String> steps = new ArrayList<>();
            VCSimplificationResult original = found;
            for (; original.getOrigin() != null; original = original.getOrigin())
                steps.add(0, original.getSimplification());
            List<String> originalBinders = original.getBinders().stream().map(b -> b.replaceAll("[0-9]+$", ""))
                    .toList();
            DisplayNames names = error.getDisplayNames();
            ERRORS.put(error.getPosition().getLine(), new Shown(rows(found.getImplication(), names), error.getMessage(),
                    error.getCounterexampleStr(), rows(original.getImplication(), names), steps, originalBinders));
        }
    }

    @Test
    void issueReproducerDropsTheTrueFieldRow() {
        Shown shown = ERRORS.get(25);
        assertEquals(List.of("∀items.size() items.size() == size(this.items) && items.size() >= 0"), shown.rows());
    }

    @Test
    void callArgumentIsWrittenAsTheCallerWroteIt() {
        Shown shown = ERRORS.get(79);
        assertEquals("count() >= 0 is not a subtype of count() > 0", shown.message());
        assertEquals("Counterexample: count() == 0", shown.counterexample());
    }

    @Test
    void steppingBackReachesTheOriginalWithTheDroppedRow() {
        Shown shown = ERRORS.get(25);
        assertEquals(List.of("∀this.items true", "∀items items == this.items",
                "∀items.size() items.size() == size(items) && items.size() >= 0"), shown.originalRows());
        assertEquals(List.of("Substitution", "Binder"), shown.steps());
        assertEquals(List.of("this#testSuite.classes.found_binders_error.Basket.items", "#items_", "#bound_"),
                shown.originalBinders());
    }

    @Test
    void staticFieldIsWrittenWithItsClass() {
        Shown shown = ERRORS.get(31);
        assertEquals(List.of("∀count count == Basket.served + 1"), shown.rows());
        assertEquals("Counterexample: count == 0 && Basket.served == -1", shown.counterexample());
    }

    @Test
    void variablesAnotherRowShowsGetNoTrueRow() {
        Shown shown = ERRORS.get(64);
        assertEquals(List.of("∀sum sum == first + second"), shown.rows());
        assertEquals("Counterexample: sum == 0 && first == 0 && second == 0", shown.counterexample());
    }

    @Test
    void variableOnlyExpectedNamesKeepsItsTrueRow() {
        Shown shown = ERRORS.get(69);
        assertEquals(List.of("∀t true"), shown.rows());
        assertEquals("Counterexample: t == 0", shown.counterexample());
    }

    @Test
    void writeToAnotherObjectsFieldIsNamedAfterThatObject() {
        assertEquals("other.level == -1 is not a subtype of other.level >= 0", ERRORS.get(36).message());
        assertEquals(List.of("∀this.level this.level >= 0", "∀other.level other.level == this.level - 1"),
                ERRORS.get(48).rows());
    }

    @Test
    void earlierValuesOfAVariableAreWrittenWithTheirLine() {
        Shown shown = ERRORS.get(86);
        assertEquals("b@86 == 8 && b@86 > 0 && b == -b@86 is not a subtype of b > 0", shown.message());
        assertEquals("Counterexample: b == -8", shown.counterexample());
        assertEquals(List.of("∀b@85 b@85 == 8", "∀b@86 b@86 == b@85 && b@86 > 0", "∀b b == -b@86"),
                shown.originalRows());
    }

    // the rows of Found as the IDE writes them, with the error's names: binder name, then its refinement
    @Test
    void stateFoundFoldsTheBranchesItsPathConditionRulesOut() {
        CommandLineLauncher.launch("../liquidjava-example/src/main/java/testSuite/ErrorStatePathFolding.java");
        List<LJError> errors = Diagnostics.getInstance().getErrors().stream().toList();
        assertEquals(1, errors.size());
        assertEquals("found next == BORDERLESS && (previous != BORDERLESS ? display(frame) == hidden : "
                + "display(frame) == shown) but expected display(frame) == hidden", errors.get(0).getMessage());
    }

    private static List<String> rows(VCImplication found, DisplayNames names) {
        List<String> rows = new ArrayList<>();
        for (VCImplication current = found; current != null; current = current.getNext()) {
            String refinement = ExpressionFormatter.format(current.getRefinement().getExpression(), names);
            rows.add(current.hasBinder() ? "∀" + names.get(current.getName()) + " " + refinement : refinement);
        }
        return rows;
    }
}
