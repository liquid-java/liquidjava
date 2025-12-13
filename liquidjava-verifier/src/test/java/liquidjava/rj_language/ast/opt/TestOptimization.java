import static org.junit.Assert.assertEquals;
import org.junit.Test;

import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.LiteralInt;
import liquidjava.rj_language.opt.ConstantFolding;
import liquidjava.rj_language.opt.derivation_node.ValDerivationNode;

public class TestOptimization {
    @Test
    public void testBinaryFold() {
        BinaryExpression b = new BinaryExpression(new LiteralInt(1), "+", new LiteralInt(2));

        ValDerivationNode r = ConstantFolding.fold(new ValDerivationNode(b, null));
        assertEquals(r.getValue(), new LiteralInt(3));
    }

    @Test
    public void testBinaryExpressionWithLiteralInt() {
        LiteralInt num1 = new LiteralInt(40);
        LiteralInt num2 = new LiteralInt(60);

        BinaryExpression expression = new BinaryExpression(num1, "+", num2);

        String result = expression.toString();

        assertEquals("40 + 60", result);
    }

    @Test
    public void testIntegrationNestedAddition() {
        // Create integer literals
        LiteralInt num1 = new LiteralInt(5);
        LiteralInt num2 = new LiteralInt(10);
        LiteralInt num3 = new LiteralInt(20);

        // Create nested binary expressions
        BinaryExpression firstSum = new BinaryExpression(num1, "+", num2); // 5 + 10
        BinaryExpression nestedSum = new BinaryExpression(firstSum, "+", num3); // (5 + 10) + 20

        // Convert to string
        String result = nestedSum.toString();

        // Expected output: "(5 + 10) + 20"
        assertEquals("5 + 10 + 20", result);
    }

}