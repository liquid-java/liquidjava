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
import liquidjava.rj_language.ast.formatter.VariableFormatter;
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
            ERRORS.put(error.getPosition().getLine(),
                    new Shown(rows(found.getImplication()), plain(error.getMessage()),
                            error.getCounterexampleStr() == null ? null : plain(error.getCounterexampleStr()),
                            rows(original.getImplication()), steps, originalBinders));
        }
    }

    @Test
    void issueReproducerDropsTheTrueFieldRow() {
        Shown shown = ERRORS.get(25);
        assertEquals(List.of("∀bound bound == size(this.items) && bound >= 0"), shown.rows());
        assertEquals("bound == size(this.items) && bound >= 0 is not a subtype of bound > 0", shown.message());
    }

    @Test
    void steppingBackReachesTheOriginalWithTheDroppedRow() {
        Shown shown = ERRORS.get(25);
        assertEquals(
                List.of("∀this.items true", "∀items items == this.items", "∀bound bound == size(items) && bound >= 0"),
                shown.originalRows());
        assertEquals(List.of("Substitution", "Binder"), shown.steps());
        assertEquals(List.of("this#testSuite.classes.found_binders_error.Basket.items", "#items_", "#bound_"),
                shown.originalBinders());
    }

    @Test
    void staticFieldIsWrittenWithItsClass() {
        Shown shown = ERRORS.get(31);
        assertEquals(List.of("∀Basket.served true", "∀count count == Basket.served + 1"), shown.rows());
        assertEquals("Counterexample: Basket.served == -1 && count == 0", shown.counterexample());
    }

    @Test
    void writeToAnotherObjectsFieldIsNamedAfterThatObject() {
        assertEquals("other.level == -1 is not a subtype of other.level >= 0", ERRORS.get(36).message());
        assertEquals(List.of("∀this.level this.level >= 0", "∀other.level other.level == this.level - 1"),
                ERRORS.get(48).rows());
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
