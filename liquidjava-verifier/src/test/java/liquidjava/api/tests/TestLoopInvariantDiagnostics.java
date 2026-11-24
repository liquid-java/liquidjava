package liquidjava.api.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import liquidjava.api.CommandLineLauncher;
import liquidjava.diagnostics.Diagnostics;
import liquidjava.diagnostics.InferredFacts;
import liquidjava.diagnostics.InferredFacts.Point;
import liquidjava.diagnostics.SourceReference;
import liquidjava.diagnostics.errors.RefinementError;
import liquidjava.diagnostics.hints.Hint;
import liquidjava.rj_language.ast.formatter.ExpressionFormatter;

class TestLoopInvariantDiagnostics {

    private static final String TEST_SUITE = "../liquidjava-example/src/main/java/testSuite/";

    @Test
    void factsInferredAtTheLoopHeadAreMarkedWithTheLoop() {
        RefinementError error = errorAt("ErrorLoopInvariants.java", 48);
        assertEquals("found `i@46#1 >= 0 && i@46#1 < 10 && i == i@46#1 + 3` but expected `i <= 10`",
                error.getMessage());
        InferredFacts inferred = error.getInferredFacts().get(0);
        assertEquals(1, error.getInferredFacts().size());
        assertEquals("i@46#1", error.getDisplayNames().get(inferred.variable()));
        assertEquals(List.of("i@46#1 >= i@45"), shown(error, inferred));
        assertEquals(46, inferred.loop().getLine());
        assertEquals(Point.ITERATION_START, inferred.point());
    }

    @Test
    void factsKnownAfterTheLoopAreMarkedWithTheLoop() {
        RefinementError error = errorAt("ErrorLoopExitFacts.java", 45);
        InferredFacts inferred = error.getInferredFacts().get(0);
        assertEquals(1, error.getInferredFacts().size());
        assertEquals(List.of("i >= i@41", "!(i < 10)"), shown(error, inferred));
        assertEquals(42, inferred.loop().getLine());
        assertEquals(Point.AFTER_LOOP, inferred.point());
    }

    @Test
    void hintSuggestsStatingExpectedOnTheDeclarationOfTheVariableTheLoopChanges() {
        RefinementError error = errorAt("ErrorLoopInvariants.java", 25);
        Hint hint = error.getHints().get(0);
        assertEquals(1, error.getHints().size());
        assertEquals("`i` changes in the loop at line 24, and LiquidJava only keeps what holds for it on every "
                + "iteration. If `i >= 0` should hold on every iteration, state it on the declaration: "
                + "`@Refinement(\"_ >= 0\") int i = 0;`", hint.message());
        SourceReference reference = hint.reference();
        assertEquals("line 24", reference.text());
        assertEquals(24, reference.line());
    }

    @Test
    void noHintWhenExpectedFailsOnTheFirstIterationToo() {
        assertEquals(List.of(), errorAt("ErrorLoopUnprovedInvariant.java", 19).getHints());
    }

    private static List<String> shown(RefinementError error, InferredFacts inferred) {
        return inferred.facts().stream().map(fact -> ExpressionFormatter.format(fact, error.getDisplayNames()))
                .toList();
    }

    private static RefinementError errorAt(String test, int line) {
        CommandLineLauncher.launch(TEST_SUITE + test);
        return Diagnostics.getInstance().getErrors().stream().filter(e -> e.getPosition().getLine() == line)
                .map(RefinementError.class::cast).findFirst().orElseThrow();
    }
}
