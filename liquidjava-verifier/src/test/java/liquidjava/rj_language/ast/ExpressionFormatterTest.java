package liquidjava.rj_language.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import liquidjava.rj_language.parsing.RefinementsParser;

class ExpressionFormatterTest {

    private static Expression parse(String refinement) {
        return RefinementsParser.createAST(refinement, "");
    }

    @Test
    void formatsUnaryAtoms() {
        assertEquals("!x", parse("!x").toDisplayString());
        assertEquals("!false", parse("!false").toDisplayString());
    }

    @Test
    void formatsInternalVariables() {
        assertEquals("x", parse("x").toDisplayString());
        assertEquals("x²", parse("#x_2").toDisplayString());
        assertEquals("#fresh¹²", parse("#fresh_12").toDisplayString());
        assertEquals("#ret³", parse("#ret_3").toDisplayString());
        assertEquals("this#Class", parse("this#Class").toDisplayString());
    }

    @Test
    void formatsEnums() {
        assertEquals("Color.RED", parse("Color.RED").toDisplayString());
    }

    @Test
    void formatsUnaryCompounds() {
        assertEquals("x > 0", parse("x > 0").toDisplayString());
        assertEquals("!(x > 0)", parse("!(x > 0)").toDisplayString());
        assertEquals("-(-x)", parse("-(-x)").toDisplayString());
    }

    @Test
    void formatsBinaryPrecedence() {
        assertEquals("(a + b) * c", parse("(a + b) * c").toDisplayString());
        assertEquals("a * (a + b)", parse("a * (a + b)").toDisplayString());
        assertEquals("a + b * c", parse("a + b * c").toDisplayString());
        assertEquals("a - (a + b)", parse("a - (a + b)").toDisplayString());
        assertEquals("a + b + c", parse("a + b + c").toDisplayString());
        assertEquals("b * c * c", parse("b * c * c").toDisplayString());
    }

    @Test
    void omitsUnnecessaryGroupParentheses() {
        assertEquals("x", parse("(x)").toDisplayString());
        assertEquals("x", parse("((x))").toDisplayString());
        assertEquals("1", parse("(1)").toDisplayString());
        assertEquals("a > 0", parse("(a > 0)").toDisplayString());
        assertEquals("a + b + c", parse("a + (b + c)").toDisplayString());
        assertEquals("a + b * c", parse("a + (b * c)").toDisplayString());
        assertEquals("a && b > 0", parse("a && (b > 0)").toDisplayString());
        assertEquals("a && b && c", parse("a && (b && c)").toDisplayString());
        assertEquals("size(stack²⁹⁴) > 0", parse("(size(#stack_294) > 0)").toDisplayString());
        assertEquals("size(stack²⁹⁴) > 0 && ready", parse("(size(#stack_294) > 0) && ready").toDisplayString());
        assertEquals("ready && size(stack²⁹⁴) > 0", parse("ready && (size(#stack_294) > 0)").toDisplayString());
    }

    @Test
    void formatsRightGrouping() {
        assertEquals("a - (b + c)", parse("a - (b + c)").toDisplayString());
        assertEquals("a - (b - c)", parse("a - (b - c)").toDisplayString());
        assertEquals("a / (b * c)", parse("a / (b * c)").toDisplayString());
        assertEquals("(a || b) && c", parse("(a || b) && c").toDisplayString());
        assertEquals("x == (1 > 0)", parse("x == (1 > 0)").toDisplayString());
        assertEquals("a == (b == c)", parse("a == (b == c)").toDisplayString());
    }

    @Test
    void formatsLogicalExpressions() {
        assertEquals("a && b || c", parse("a && b || c").toDisplayString());
        assertEquals("a && (b || c)", parse("a && (b || c)").toDisplayString());
        assertEquals("a --> (b --> c)", parse("a --> b --> c").toDisplayString());
        assertEquals("a --> (b --> c)", parse("a --> (b --> c)").toDisplayString());
        assertEquals("a --> (b --> (c --> d))", parse("a --> b --> c --> d").toDisplayString());
        assertEquals("(a --> b) --> c", parse("(a --> b) --> c").toDisplayString());
        assertEquals("a && b && c", parse("a && b && c").toDisplayString());
        assertEquals("a || b || c", parse("a || b || c").toDisplayString());
    }

    @Test
    void formatsTernaryExpressions() {
        assertEquals("(a ? b : c) + d", parse("(a ? b : c) + d").toDisplayString());
        assertEquals("(a ? b : c) ? d : e", parse("(a ? b : c) ? d : e").toDisplayString());
        assertEquals("a ? (b ? c : d) : e", parse("a ? (b ? c : d) : e").toDisplayString());
        assertEquals("a ? b : (c ? d : e)", parse("a ? b : c ? d : e").toDisplayString());
        assertEquals("(a ? b : c) ? d : e", parse("(a ? b : c) ? d : e").toDisplayString());
        assertEquals("a ? b : (c ? d : e)", parse("a ? b : (c ? d : e)").toDisplayString());
        assertEquals("a ? b : (c ? d : (e ? f : g))", parse("a ? b : c ? d : e ? f : g").toDisplayString());
        assertEquals("a ? b : c", parse("a ? b : c").toDisplayString());
    }
}
