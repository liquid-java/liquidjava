package liquidjava.rj_language.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class ExpressionFormatterTest {

    @Test
    void formatsUnaryAtoms() {
        assertEquals("!x", new UnaryExpression("!", new Var("x")).toDisplayString());
        assertEquals("!false", new UnaryExpression("!", new LiteralBoolean(false)).toDisplayString());
    }

    @Test
    void formatsInternalVariables() {
        assertEquals("x", new Var("x").toDisplayString());
        assertEquals("x²", new Var("#x_2").toDisplayString());
        assertEquals("#fresh¹²", new Var("#fresh_12").toDisplayString());
        assertEquals("#ret³", new Var("#ret_3").toDisplayString());
        assertEquals("this#Class", new Var("this#Class").toDisplayString());
    }

    @Test
    void formatsEnums() {
        assertEquals("Color.RED", new Enum("Color", "RED").toDisplayString());
    }

    @Test
    void formatsUnaryCompounds() {
        Expression comparison = new BinaryExpression(new Var("x"), ">", new LiteralInt(0));

        assertEquals("x > 0", comparison.toDisplayString());
        assertEquals("!(x > 0)", new UnaryExpression("!", comparison).toDisplayString());
        assertEquals("-(-x)", new UnaryExpression("-", new GroupExpression(new UnaryExpression("-", new Var("x"))))
                .toDisplayString());
    }

    @Test
    void formatsBinaryPrecedence() {
        Expression sum = new BinaryExpression(new Var("a"), "+", new Var("b"));
        Expression product = new BinaryExpression(new Var("b"), "*", new Var("c"));

        assertEquals("(a + b) * c", new BinaryExpression(sum, "*", new Var("c")).toDisplayString());
        assertEquals("a * (a + b)", new BinaryExpression(new Var("a"), "*", sum).toDisplayString());
        assertEquals("a + b * c", new BinaryExpression(new Var("a"), "+", product).toDisplayString());
        assertEquals("a - (a + b)", new BinaryExpression(new Var("a"), "-", sum).toDisplayString());
        assertEquals("a + b + c", new BinaryExpression(sum, "+", new Var("c")).toDisplayString());
        assertEquals("b * c * c", new BinaryExpression(product, "*", new Var("c")).toDisplayString());
    }

    @Test
    void formatsRightGrouping() {
        Expression groupedSum = new GroupExpression(new BinaryExpression(new Var("b"), "+", new Var("c")));
        Expression groupedComparison = new GroupExpression(
                new BinaryExpression(new LiteralInt(1), ">", new LiteralInt(0)));

        assertEquals("a - (b + c)", new BinaryExpression(new Var("a"), "-", groupedSum).toDisplayString());
        assertEquals("x == (1 > 0)", new BinaryExpression(new Var("x"), "==", groupedComparison).toDisplayString());
    }

    @Test
    void formatsLogicalExpressions() {
        Expression andExpression = new BinaryExpression(new Var("a"), "&&", new Var("b"));
        Expression orExpression = new BinaryExpression(new Var("b"), "||", new Var("c"));
        Expression implication = new BinaryExpression(new Var("b"), "-->", new Var("c"));

        assertEquals("a && b || c", new BinaryExpression(andExpression, "||", new Var("c")).toDisplayString());
        assertEquals("a && (b || c)", new BinaryExpression(new Var("a"), "&&", orExpression).toDisplayString());
        assertEquals("a --> (b --> c)", new BinaryExpression(new Var("a"), "-->", implication).toDisplayString());
        assertEquals("a && b && c", new BinaryExpression(andExpression, "&&", new Var("c")).toDisplayString());
        assertEquals("a || b || c",
                new BinaryExpression(new BinaryExpression(new Var("a"), "||", new Var("b")), "||", new Var("c"))
                        .toDisplayString());
    }

    @Test
    void formatsTernaryExpressions() {
        Expression ite = new Ite(new Var("a"), new Var("b"), new Var("c"));
        Expression nestedElse = new Ite(new Var("c"), new Var("d"), new Var("e"));

        assertEquals("(a ? b : c) + d", new BinaryExpression(ite, "+", new Var("d")).toDisplayString());
        assertEquals("(a ? b : c) ? d : e", new Ite(ite, new Var("d"), new Var("e")).toDisplayString());
        assertEquals("a ? (b ? c : d) : e",
                new Ite(new Var("a"), new Ite(new Var("b"), new Var("c"), new Var("d")), new Var("e"))
                        .toDisplayString());
        assertEquals("a ? b : c ? d : e", new Ite(new Var("a"), new Var("b"), nestedElse).toDisplayString());
        assertEquals("(a ? b : c) ? d : e",
                new Ite(new GroupExpression(ite), new Var("d"), new Var("e")).toDisplayString());
        assertEquals("a ? b : (c ? d : e)",
                new Ite(new Var("a"), new Var("b"), new GroupExpression(nestedElse)).toDisplayString());
        assertEquals("a ? b : c", new Ite(new Var("a"), new Var("b"), new Var("c")).toDisplayString());
    }
}
