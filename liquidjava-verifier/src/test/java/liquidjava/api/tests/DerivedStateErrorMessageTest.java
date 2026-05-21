package liquidjava.api.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import liquidjava.api.CommandLineLauncher;
import liquidjava.diagnostics.Diagnostics;
import liquidjava.diagnostics.errors.StateRefinementError;

/**
 * End-to-end check that {@link StateRefinementError} diagnostics present the found-state conjunction with
 * developer-facing typestate names — not the internal {@code stateN(x) == stateN(y)} equalities the SMT layer threads
 * across call boundaries.
 *
 * <p>
 * {@code TestExamples} only matches an error's title and line, never its message body, so the state-equality derivation
 * rewrite needs a dedicated assertion on the rendered text. The {@code imagewrite_error} scenario is a two-dimension
 * external-typestate spec whose found-state relates the same {@code param} across several SSA versions — exactly the
 * shape that produces those equalities.
 */
class DerivedStateErrorMessageTest {

    private static final String IMAGEWRITE_ERROR = "../liquidjava-example/src/main/java/testSuite/classes/imagewrite_error";

    @Test
    void stateRefinementErrorShowsDeveloperStatesNotInternalEqualities() {
        CommandLineLauncher.launch(IMAGEWRITE_ERROR);

        StateRefinementError error = Diagnostics.getInstance().getErrors().stream()
                .filter(StateRefinementError.class::isInstance).map(StateRefinementError.class::cast).findFirst()
                .orElseThrow(() -> new AssertionError("expected a StateRefinementError from imagewrite_error"));

        // the rendered, developer-visible message string ("Expected state ... but found ...")
        String message = error.getMessage();

        // derivation rewrote every cross-version ghost-state equality into a named typestate
        assertTrue(message.contains("startTiling("), "message should name the tiling typestate, got: " + message);
        assertTrue(message.contains("compressionExplicit("),
                "message should name the compression typestate, got: " + message);

        // the internal ghost-state functions and their equalities must not leak into the diagnostic
        assertFalse(message.contains("state1("), "internal state1(...) leaked into the message: " + message);
        assertFalse(message.contains("state2("), "internal state2(...) leaked into the message: " + message);
        assertFalse(message.contains("=="), "a raw ghost-state equality leaked into the message: " + message);
    }
}
