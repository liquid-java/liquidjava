package liquidjava.rj_language.ast.formatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class VariableFormatterTest {

    @AfterEach
    void forgetRecordedNames() {
        FieldDisplayNames.clear();
    }

    @Test
    void formatsVariables() {
        assertEquals("x", VariableFormatter.format("x"));
        assertEquals("x²", VariableFormatter.format("#x_2"));
        assertEquals("#if_condition¹²", VariableFormatter.format("##if_condition_12"));
        assertEquals("#size_result⁴", VariableFormatter.format("##size_result_4"));
        assertEquals("size_result⁴", VariableFormatter.format("#size_result_4"));
        assertEquals("#ret³", VariableFormatter.format("#ret_3"));
        assertEquals("this#Class", VariableFormatter.format("this#Class"));
        // ghost applications
        assertEquals("size(s²)", VariableFormatter.format("size(#s_2)"));
        assertEquals("size(bag)", VariableFormatter.format("size(bag)"));
        assertEquals("at(list³, #get_result¹²)", VariableFormatter.format("at(#list_3, ##get_result_12)"));
        assertEquals("size(this)", VariableFormatter.format("size(this)"));
        assertEquals("f(g(a¹, b²), c³)", VariableFormatter.format("f(g(#a_1, #b_2), #c_3)"));
        assertEquals("f(g(h(a¹)), b)", VariableFormatter.format("f(g(h(#a_1)),b)"));
        assertEquals("size()", VariableFormatter.format("size()"));
    }

    @Test
    void writesInstanceFieldsAsInCode() {
        assertEquals("this.items", VariableFormatter.format("this#shop.Basket.items"));
        assertEquals("this.port⁴", VariableFormatter.format("#this#pkg.Outer$Job.port_4"));
    }

    @Test
    void writesStaticFieldsWithTheirClass() {
        FieldDisplayNames.recordStaticField("this#pkg.Outer$Job.count");
        assertEquals("Job.count³", VariableFormatter.format("#this#pkg.Outer$Job.count_3"));
    }

    @Test
    void writesAFieldWrittenThroughAnotherObjectWithThatObject() {
        FieldDisplayNames.recordWriteReceiver("#this#pkg.W.level_1", "other");
        assertEquals("other.level¹", VariableFormatter.format("#this#pkg.W.level_1"));
        assertEquals("this.level²", VariableFormatter.format("#this#pkg.W.level_2"));
        FieldDisplayNames.clear();
        assertEquals("this.level¹", VariableFormatter.format("#this#pkg.W.level_1"));
    }
}
