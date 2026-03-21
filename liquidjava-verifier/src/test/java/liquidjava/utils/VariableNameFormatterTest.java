package liquidjava.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import liquidjava.rj_language.ast.Var;
import liquidjava.rj_language.opt.derivation_node.ValDerivationNode;
import liquidjava.rj_language.opt.derivation_node.VarDerivationNode;
import org.junit.jupiter.api.Test;

class VariableNameFormatterTest {

    @Test
    void testInstanceVariableDisplayNameFormatting() {
        assertEquals("x", VariableNameFormatter.formatVariable("x"));
        assertEquals("x²", VariableNameFormatter.formatVariable("#x_2"));
        assertEquals("#fresh¹²", VariableNameFormatter.formatVariable("#fresh_12"));
        assertEquals("#ret³", VariableNameFormatter.formatVariable("#ret_3"));
        assertEquals("this#Class", VariableNameFormatter.formatVariable("this#Class"));
    }

    @Test
    void testDerivationNodeUsesSuperscriptNotation() {
        ValDerivationNode node = new ValDerivationNode(new Var("#x_2"), new VarDerivationNode("#x_2"));
        String serialized = node.toString();
        assertTrue(serialized.contains("\"value\": \"x²\""), "Expected derivation value to use superscript notation");
        assertTrue(serialized.contains("\"var\": \"x²\""), "Expected derivation origin to use superscript notation");
    }
}
