package liquidjava.diagnostics;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CodeTest {

    @Test
    void codeHoldingABacktickGetsALongerFence() {
        assertEquals("`x > 0`", Code.of("x > 0"));
        assertEquals("``c == '`'``", Code.of("c == '`'"));
        assertEquals("`` `a ``", Code.of("`a"));
    }

    @Test
    void terminalShowsCodeInColourWithoutItsBackticks() {
        String message = "found " + Code.of("x > 0") + " but expected " + Code.of("`a");
        assertEquals(
                "found " + Colors.BLUE + "x > 0" + Colors.RESET + " but expected " + Colors.BLUE + "`a" + Colors.RESET,
                Code.render(message, true));
        assertEquals(message, Code.render(message, false));
        // spaces and line breaks of the code itself are kept
        assertEquals(Colors.BLUE + " a " + Colors.RESET, Code.render(Code.of(" a "), true));
        assertEquals(Colors.BLUE + "a\nb" + Colors.RESET, Code.render(Code.of("a\nb"), true));
    }
}
