package liquidjava.rj_language.ast.formatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VariableFormatterTest {

    @Test
    void formatsVariables() {
        assertEquals("x", VariableFormatter.format("x"));
        assertEquals("x²", VariableFormatter.format("#x_2"));
        assertEquals("#fresh¹²", VariableFormatter.format("#fresh_12"));
        assertEquals("#ret³", VariableFormatter.format("#ret_3"));
        assertEquals("this#Class", VariableFormatter.format("this#Class"));
    }
}
