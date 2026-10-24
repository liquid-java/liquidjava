package liquidjava.diagnostics;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import liquidjava.diagnostics.errors.CustomError;
import liquidjava.diagnostics.hints.Hint;

class LJDiagnosticTest {

    @Test
    void diagnosticWithoutHintsHasNoHintText() {
        assertNull(new CustomError("message", null).getHint());
    }

    @Test
    void hintsKeepTheirOrderAndAreJoinedOnePerLine() {
        CustomError error = new CustomError("message", null);
        error.addHint(Hint.general("first"));
        error.addHint(Hint.general("second"));
        assertEquals(List.of(Hint.general("first"), Hint.general("second")), error.getHints());
        assertEquals("first\nsecond", error.getHint());
    }

    @Test
    void hintsArePrintedInTheirOwnSection() {
        CustomError error = new CustomError("message", null);
        error.addHint(Hint.general("first"));
        error.addHint(Hint.general("second"));
        assertTrue(error.toString().endsWith("\nHints:\n  - first\n  - second\n"));
    }
}
