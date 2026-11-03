package liquidjava.rj_language.ast.formatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class VariableFormatterTest {

    @AfterEach
    void forgetStaticFields() {
        VariableFormatter.clearStaticFields();
    }

    @Test
    void formatsVariables() {
        assertEquals("x", VariableFormatter.format("x"));
        assertEquals("x²", VariableFormatter.format("#x_2"));
        assertEquals("#fresh¹²", VariableFormatter.format("#fresh_12"));
        assertEquals("#ret³", VariableFormatter.format("#ret_3"));
        assertEquals("this#Class", VariableFormatter.format("this#Class"));
        // ghost applications
        assertEquals("size(s²)", VariableFormatter.format("size(#s_2)"));
        assertEquals("size(bag)", VariableFormatter.format("size(bag)"));
        assertEquals("at(list³, #fresh¹²)", VariableFormatter.format("at(#list_3, #fresh_12)"));
        assertEquals("size(this)", VariableFormatter.format("size(this)"));
        assertEquals("f(g(a¹, b²), c³)", VariableFormatter.format("f(g(#a_1, #b_2), #c_3)"));
        assertEquals("f(g(h(a¹)), b)", VariableFormatter.format("f(g(h(#a_1)),b)"));
        assertEquals("size()", VariableFormatter.format("size()"));
    }

    @Test
    void writesInstanceFieldsAsInCode() {
        assertEquals("this.items", VariableFormatter.format("this#shop.Basket.items"));
        assertEquals("this.items", VariableFormatter.format("this#Basket.items"));
        assertEquals("this.interrupts⁴⁷⁵", VariableFormatter
                .format("#this#x_e_oss_a1_17_interruptmanager.E_OSS_A1_17_InterruptManager.interrupts_475"));
        assertEquals("this.port", VariableFormatter.format("this#pkg.Outer$Job.port"));
    }

    @Test
    void writesStaticFieldsWithTheirClass() {
        VariableFormatter.registerStaticField("this#shop.Basket.served");
        VariableFormatter.registerStaticField("this#pkg.Outer$Job.count");
        assertEquals("Basket.served", VariableFormatter.format("this#shop.Basket.served"));
        assertEquals("Basket.served³", VariableFormatter.format("#this#shop.Basket.served_3"));
        assertEquals("Job.count", VariableFormatter.format("this#pkg.Outer$Job.count"));
        assertEquals("this.items", VariableFormatter.format("this#shop.Basket.items"));
    }

    @Test
    void forgetsStaticFieldsOfThePreviousVerification() {
        VariableFormatter.registerStaticField("this#shop.Basket.served");
        VariableFormatter.clearStaticFields();
        assertEquals("this.served", VariableFormatter.format("this#shop.Basket.served"));
    }

    @Test
    void writesFieldsOfAnotherObjectWithThatObject() {
        assertEquals("other.level", VariableFormatter.format("other#otherwrite.W.level"));
        assertEquals("other.level¹", VariableFormatter.format("#other#otherwrite.W.level_1"));
    }

    @Test
    void recognisesFieldNames() {
        assertTrue(VariableFormatter.isField("this#shop.Basket.items"));
        assertTrue(VariableFormatter.isField("#this#shop.Basket.items_4"));
        assertTrue(VariableFormatter.isField("other#otherwrite.W.level"));
        assertFalse(VariableFormatter.isField("this#Basket"));
        assertFalse(VariableFormatter.isField("#this_4"));
        assertFalse(VariableFormatter.isField("items"));
        assertFalse(VariableFormatter.isField("java.util.ArrayList.size"));
    }
}
