package liquidjava.rj_language.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.parsing.RefinementsParser;

class ExpressionFormatterTest {

    private static Expression parse(String refinement) {
        return parse(refinement, "");
    }

    private static Expression parse(String refinement, String prefix) {
        return RefinementsParser.createAST(refinement, prefix);
    }

    @Test
    void formatsUnary() {
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
    void formatsGrouping() {
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

    @Test
    void formatsWithQualifiedNames() {
        Expression exp = new BinaryExpression(parse("size(this)", "java.util.ArrayList"), "==",
                parse("size(this)", "java.util.ArrayDeque"));
        assertEquals("java.util.ArrayList.size(this) == java.util.ArrayDeque.size(this)", exp.toDisplayString());

        Predicate differentInstances = Predicate.createEquals(Predicate.createVar("#java.util.ArrayList.size_1"),
                Predicate.createVar("#java.util.ArrayDeque.size_2"));
        assertEquals("java.util.ArrayList.size¹ == java.util.ArrayDeque.size²",
                differentInstances.getExpression().toDisplayString());
    }

    @Test
    void formatsWithoutQualifiedNames() {
        assertEquals("size(this)", parse("size(this)", "java.util.List").toDisplayString());
        assertEquals("size(this) == size(this)", parse("size(this) == size(this)", "java.util.List").toDisplayString());   
    }
}
