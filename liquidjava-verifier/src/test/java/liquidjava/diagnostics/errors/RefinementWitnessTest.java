package liquidjava.diagnostics.errors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import liquidjava.diagnostics.TranslationTable;
import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.opt.VCSimplificationResult;
import liquidjava.rj_language.parsing.RefinementsParser;
import liquidjava.smt.Counterexample;
import liquidjava.utils.Pair;
import spoon.Launcher;
import spoon.reflect.factory.Factory;

class RefinementWitnessTest {

    private static final Factory FACTORY = new Launcher().getFactory();

    @Test
    void expandsTheFinalExpectedPredicateBeforeShowingTheWitness() {
        RefinementError error = error("Positive(buffered)", "buffered > 0", List.of("buffered"),
                new Pair<>("buffered", "0"));

        assertEquals("Positive(buffered)", error.getExpected().getExpression().toDisplayString());
        assertEquals("buffered > 0", error.getFinalExpected().getExpression().toDisplayString());
        assertEquals("0 > 0", error.getExpectedWithWitness().getExpression().toDisplayString());
        assertTrue(error.getCounterexampleStr().contains("With witness: 0 > 0 ✗"));
    }

    @Test
    void substitutesEveryAvailableValueAndEveryOccurrence() {
        RefinementError error = error("x < y && x != 0", "x < y && x != 0", List.of("x", "y"), new Pair<>("x", "0"),
                new Pair<>("y", "1"));

        assertEquals("0 < 1 && 0 != 0", error.getExpectedWithWitness().getExpression().toDisplayString());
    }

    @Test
    void leavesValuesThatAreNotSafeLiteralsUnchanged() {
        RefinementError error = error("x < y", "x < y", List.of("x", "y"), new Pair<>("x", "-1"),
                new Pair<>("y", "other"));

        assertNotNull(error.getExpectedWithWitness());
        assertEquals("-1 < y", error.getExpectedWithWitness().getExpression().toDisplayString());
        assertTrue(error.getCounterexampleStr().contains("With witness: -1 < y"));
    }

    @SafeVarargs
    private static RefinementError error(String original, String finalExpression, List<String> binders,
            Pair<String, String>... assignments) {
        VCImplication first = null;
        VCImplication last = null;
        for (String binder : binders) {
            VCImplication current = new VCImplication(binder, FACTORY.Type().INTEGER_PRIMITIVE, new Predicate());
            if (last != null)
                last.setNext(current);
            if (first == null)
                first = current;
            last = current;
        }
        assertNotNull(first);
        return new RefinementError(null, null, predicate(original), predicate(finalExpression),
                new VCSimplificationResult(first), new TranslationTable(), new Counterexample(List.of(assignments)),
                null);
    }

    private static Predicate predicate(String source) {
        return new Predicate(RefinementsParser.createAST(source, ""));
    }
}
