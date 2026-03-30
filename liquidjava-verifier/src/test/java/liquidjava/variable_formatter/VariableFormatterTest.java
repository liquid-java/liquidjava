package liquidjava.variable_formatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import liquidjava.rj_language.ast.Var;
import liquidjava.rj_language.opt.derivation_node.ValDerivationNode;
import liquidjava.rj_language.opt.derivation_node.VarDerivationNode;
import liquidjava.utils.VariableFormatter;

import org.junit.jupiter.api.Test;

class VariableFormatterTest {

    @Test
    void testInstanceVariableDisplayNameFormatting() {
        assertEquals("x", VariableFormatter.formatVariable("x"));
        assertEquals("x²", VariableFormatter.formatVariable("#x_2"));
        assertEquals("#fresh¹²", VariableFormatter.formatVariable("#fresh_12"));
        assertEquals("#ret³", VariableFormatter.formatVariable("#ret_3"));
        assertEquals("this#Class", VariableFormatter.formatVariable("this#Class"));
    }

    @Test
    void testDerivationNodeUsesSuperscriptNotation() {
        ValDerivationNode node = new ValDerivationNode(new Var("#x_2"), new VarDerivationNode("#x_2"));
        String serialized = node.toString();
        assertTrue(serialized.contains("\"value\": \"x²\""), "Expected derivation value to use superscript notation");
        assertTrue(serialized.contains("\"var\": \"x²\""), "Expected derivation origin to use superscript notation");
    }
}
