package liquidjava.rj_language.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ExpressionPrinterTest {

    @Test
    void printsUnaryWithoutExtraParenthesesForAtoms() {
        assertEquals("!x", new UnaryExpression("!", new Var("x")).toString());
        assertEquals("!false", new UnaryExpression("!", new LiteralBoolean(false)).toString());
    }

    @Test
    void printsUnaryWithParenthesesForCompoundOperands() {
        Expression comparison = new BinaryExpression(new Var("x"), ">", new LiteralInt(0));

        assertEquals("x > 0", comparison.toString());
        assertEquals("!(x > 0)", new UnaryExpression("!", comparison).toString());
    }

    @Test
    void printsBinaryExpressionsWithOperatorPrecedence() {
        Expression sum = new BinaryExpression(new Var("a"), "+", new Var("b"));
        Expression product = new BinaryExpression(new Var("b"), "*", new Var("c"));

        assertEquals("(a + b) * c", new BinaryExpression(sum, "*", new Var("c")).toString());
        assertEquals("a * (a + b)", new BinaryExpression(new Var("a"), "*", sum).toString());
        assertEquals("a + b * c", new BinaryExpression(new Var("a"), "+", product).toString());
        assertEquals("a - (a + b)", new BinaryExpression(new Var("a"), "-", sum).toString());
        assertEquals("a + b + c", new BinaryExpression(sum, "+", new Var("c")).toString());
        assertEquals("b * c * c", new BinaryExpression(product, "*", new Var("c")).toString());
    }

    @Test
    void printsLogicalExpressionsWithNeededParentheses() {
        Expression andExpression = new BinaryExpression(new Var("a"), "&&", new Var("b"));
        Expression orExpression = new BinaryExpression(new Var("b"), "||", new Var("c"));
        Expression implication = new BinaryExpression(new Var("b"), "-->", new Var("c"));

        assertEquals("a && b || c", new BinaryExpression(andExpression, "||", new Var("c")).toString());
        assertEquals("a && (b || c)", new BinaryExpression(new Var("a"), "&&", orExpression).toString());
        assertEquals("a --> (b --> c)", new BinaryExpression(new Var("a"), "-->", implication).toString());
        assertEquals("a && b && c", new BinaryExpression(andExpression, "&&", new Var("c")).toString());
        assertEquals("a || b || c",
                new BinaryExpression(new BinaryExpression(new Var("a"), "||", new Var("b")), "||", new Var("c"))
                        .toString());
    }

    @Test
    void printsTernaryExpressionsWithNeededParentheses() {
        Expression ite = new Ite(new Var("a"), new Var("b"), new Var("c"));
        Expression nestedElse = new Ite(new Var("c"), new Var("d"), new Var("e"));

        assertEquals("(a ? b : c) + d", new BinaryExpression(ite, "+", new Var("d")).toString());
        assertEquals("(a ? b : c) ? d : e", new Ite(ite, new Var("d"), new Var("e")).toString());
        assertEquals("a ? (b ? c : d) : e",
                new Ite(new Var("a"), new Ite(new Var("b"), new Var("c"), new Var("d")), new Var("e")).toString());
        assertEquals("a ? b : c ? d : e", new Ite(new Var("a"), new Var("b"), nestedElse).toString());
        assertEquals("a ? b : c", new Ite(new Var("a"), new Var("b"), new Var("c")).toString());
    }
}
