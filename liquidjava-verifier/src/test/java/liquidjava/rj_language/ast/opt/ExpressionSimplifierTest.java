package liquidjava.rj_language.opt;

import static org.junit.jupiter.api.Assertions.*;

import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.LiteralBoolean;
import liquidjava.rj_language.ast.LiteralInt;
import liquidjava.rj_language.ast.UnaryExpression;
import liquidjava.rj_language.ast.Var;
import liquidjava.rj_language.opt.derivation_node.BinaryDerivationNode;
import liquidjava.rj_language.opt.derivation_node.DerivationNode;
import liquidjava.rj_language.opt.derivation_node.UnaryDerivationNode;
import liquidjava.rj_language.opt.derivation_node.ValDerivationNode;
import liquidjava.rj_language.opt.derivation_node.VarDerivationNode;
import org.junit.jupiter.api.Test;

/**
 * Test suite for expression simplification using constant propagation and folding
 */
class ExpressionSimplifierTest {

    @Test
    void testNegation() {
        // Given: -a && a == 7
        // Expected: -7

        Expression varA = new Var("a");
        Expression negA = new UnaryExpression("-", varA);
        Expression seven = new LiteralInt(7);
        Expression aEquals7 = new BinaryExpression(varA, "==", seven);
        Expression fullExpression = new BinaryExpression(negA, "&&", aEquals7);

        // When
        ValDerivationNode result = ExpressionSimplifier.simplify(fullExpression);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals("-7", result.getValue().toString(), "Expected result to be -7");

        // 7 from variable a
        ValDerivationNode val7 = new ValDerivationNode(new LiteralInt(7), new VarDerivationNode("a"));

        // -7
        UnaryDerivationNode negation = new UnaryDerivationNode(val7, "-");
        ValDerivationNode expected = new ValDerivationNode(new LiteralInt(-7), negation);

        // Compare the derivation trees
        assertDerivationEquals(expected, result, "");
    }

    @Test
    void testSimpleAddition() {
        // Given: a + b && a == 3 && b == 5
        // Expected: 8 (3 + 5)

        Expression varA = new Var("a");
        Expression varB = new Var("b");
        Expression addition = new BinaryExpression(varA, "+", varB);

        Expression three = new LiteralInt(3);
        Expression aEquals3 = new BinaryExpression(varA, "==", three);

        Expression five = new LiteralInt(5);
        Expression bEquals5 = new BinaryExpression(varB, "==", five);

        Expression conditions = new BinaryExpression(aEquals3, "&&", bEquals5);
        Expression fullExpression = new BinaryExpression(addition, "&&", conditions);

        // When
        ValDerivationNode result = ExpressionSimplifier.simplify(fullExpression);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals("8", result.getValue().toString(), "Expected result to be 8");

        // 3 from variable a
        ValDerivationNode val3 = new ValDerivationNode(new LiteralInt(3), new VarDerivationNode("a"));

        // 5 from variable b
        ValDerivationNode val5 = new ValDerivationNode(new LiteralInt(5), new VarDerivationNode("b"));

        // 3 + 5
        BinaryDerivationNode add3Plus5 = new BinaryDerivationNode(val3, val5, "+");
        ValDerivationNode expected = new ValDerivationNode(new LiteralInt(8), add3Plus5);

        // Compare the derivation trees
        assertDerivationEquals(expected, result, "");
    }

    @Test
    void testSimpleComparison() {
        // Given: (y || true) && !true && y == false
        // Expected: false (true && false)

        Expression varY = new Var("y");
        Expression trueExp = new LiteralBoolean(true);
        Expression yOrTrue = new BinaryExpression(varY, "||", trueExp);

        Expression notTrue = new UnaryExpression("!", trueExp);

        Expression falseExp = new LiteralBoolean(false);
        Expression yEqualsFalse = new BinaryExpression(varY, "==", falseExp);

        Expression firstAnd = new BinaryExpression(yOrTrue, "&&", notTrue);
        Expression fullExpression = new BinaryExpression(firstAnd, "&&", yEqualsFalse);

        // When
        ValDerivationNode result = ExpressionSimplifier.simplify(fullExpression);

        // Then
        assertNotNull(result, "Result should not be null");
        assertInstanceOf(LiteralBoolean.class, result.getValue(), "Result should be a boolean");
        assertFalse(((LiteralBoolean) result.getValue()).isBooleanTrue(), "Expected result to befalse");

        // (y || true) && y == false => false || true = true
        ValDerivationNode valFalseForY = new ValDerivationNode(new LiteralBoolean(false), new VarDerivationNode("y"));
        ValDerivationNode valTrue1 = new ValDerivationNode(new LiteralBoolean(true), null);
        BinaryDerivationNode orFalseTrue = new BinaryDerivationNode(valFalseForY, valTrue1, "||");
        ValDerivationNode trueFromOr = new ValDerivationNode(new LiteralBoolean(true), orFalseTrue);

        // !true = false
        ValDerivationNode valTrue2 = new ValDerivationNode(new LiteralBoolean(true), null);
        UnaryDerivationNode notOp = new UnaryDerivationNode(valTrue2, "!");
        ValDerivationNode falseFromNot = new ValDerivationNode(new LiteralBoolean(false), notOp);

        // true && false = false
        BinaryDerivationNode andTrueFalse = new BinaryDerivationNode(trueFromOr, falseFromNot, "&&");
        ValDerivationNode falseFromFirstAnd = new ValDerivationNode(new LiteralBoolean(false), andTrueFalse);

        // y == false
        ValDerivationNode valFalseForY2 = new ValDerivationNode(new LiteralBoolean(false), new VarDerivationNode("y"));
        ValDerivationNode valFalse2 = new ValDerivationNode(new LiteralBoolean(false), null);
        BinaryDerivationNode compareFalseFalse = new BinaryDerivationNode(valFalseForY2, valFalse2, "==");
        ValDerivationNode trueFromCompare = new ValDerivationNode(new LiteralBoolean(true), compareFalseFalse);

        // false && true = false
        BinaryDerivationNode finalAnd = new BinaryDerivationNode(falseFromFirstAnd, trueFromCompare, "&&");
        ValDerivationNode expected = new ValDerivationNode(new LiteralBoolean(false), finalAnd);

        // Compare the derivation trees
        assertDerivationEquals(expected, result, "");
    }

    @Test
    void testArithmeticWithConstants() {
        // Given: (a / b + (-5)) + x && a == 6 && b == 2
        // Expected: -2 + x (6 / 2 = 3, 3 + (-5) = -2)

        Expression varA = new Var("a");
        Expression varB = new Var("b");
        Expression division = new BinaryExpression(varA, "/", varB);

        Expression five = new LiteralInt(5);
        Expression negFive = new UnaryExpression("-", five);

        Expression firstSum = new BinaryExpression(division, "+", negFive);
        Expression varX = new Var("x");
        Expression fullArithmetic = new BinaryExpression(firstSum, "+", varX);

        Expression six = new LiteralInt(6);
        Expression aEquals6 = new BinaryExpression(varA, "==", six);

        Expression two = new LiteralInt(2);
        Expression bEquals2 = new BinaryExpression(varB, "==", two);

        Expression allConditions = new BinaryExpression(aEquals6, "&&", bEquals2);
        Expression fullExpression = new BinaryExpression(fullArithmetic, "&&", allConditions);

        // When
        ValDerivationNode result = ExpressionSimplifier.simplify(fullExpression);

        // Then
        assertNotNull(result, "Result should not be null");
        assertNotNull(result.getValue(), "Result value should not be null");

        String resultStr = result.getValue().toString();
        assertEquals("-2 + x", resultStr, "Expected result to be -2 + x");

        // 6 from variable a
        ValDerivationNode val6 = new ValDerivationNode(new LiteralInt(6), new VarDerivationNode("a"));

        // 2 from variable b
        ValDerivationNode val2 = new ValDerivationNode(new LiteralInt(2), new VarDerivationNode("b"));

        // 6 / 2 = 3
        BinaryDerivationNode div6By2 = new BinaryDerivationNode(val6, val2, "/");
        ValDerivationNode val3 = new ValDerivationNode(new LiteralInt(3), div6By2);

        // -5 from unary negation of 5
        ValDerivationNode val5 = new ValDerivationNode(new LiteralInt(5), null);
        UnaryDerivationNode unaryNeg5 = new UnaryDerivationNode(val5, "-");
        ValDerivationNode valNeg5 = new ValDerivationNode(new LiteralInt(-5), unaryNeg5);

        // 3 + (-5) = -2
        BinaryDerivationNode add3AndNeg5 = new BinaryDerivationNode(val3, valNeg5, "+");
        ValDerivationNode valNeg2 = new ValDerivationNode(new LiteralInt(-2), add3AndNeg5);

        // x (variable with null origin)
        ValDerivationNode valX = new ValDerivationNode(new Var("x"), null);

        // -2 + x
        BinaryDerivationNode addNeg2AndX = new BinaryDerivationNode(valNeg2, valX, "+");
        Expression expectedResultExpr = new BinaryExpression(new LiteralInt(-2), "+", new Var("x"));
        ValDerivationNode expected = new ValDerivationNode(expectedResultExpr, addNeg2AndX);

        // Compare the derivation trees
        assertDerivationEquals(expected, result, "");
    }

    @Test
    void testComplexArithmeticWithMultipleOperations() {
        // Given: (a * 2 + b - 3) == c && a == 5 && b == 7 && c == 14
        // Expected: (5 * 2 + 7 - 3) == 14 => 14 == 14 => true

        Expression varA = new Var("a");
        Expression varB = new Var("b");
        Expression varC = new Var("c");

        Expression two = new LiteralInt(2);
        Expression aTimes2 = new BinaryExpression(varA, "*", two);

        Expression sum = new BinaryExpression(aTimes2, "+", varB);

        Expression three = new LiteralInt(3);
        Expression arithmetic = new BinaryExpression(sum, "-", three);

        Expression comparison = new BinaryExpression(arithmetic, "==", varC);

        Expression five = new LiteralInt(5);
        Expression aEquals5 = new BinaryExpression(varA, "==", five);

        Expression seven = new LiteralInt(7);
        Expression bEquals7 = new BinaryExpression(varB, "==", seven);

        Expression fourteen = new LiteralInt(14);
        Expression cEquals14 = new BinaryExpression(varC, "==", fourteen);

        Expression conj1 = new BinaryExpression(aEquals5, "&&", bEquals7);
        Expression allConditions = new BinaryExpression(conj1, "&&", cEquals14);
        Expression fullExpression = new BinaryExpression(comparison, "&&", allConditions);

        // When
        ValDerivationNode result = ExpressionSimplifier.simplify(fullExpression);

        // Then
        assertNotNull(result, "Result should not be null");
        assertNotNull(result.getValue(), "Result value should not be null");
        assertInstanceOf(LiteralBoolean.class, result.getValue(), "Result should be a boolean literal");
        assertTrue(result.getValue().isBooleanTrue(), "Expected result to be true");

        // 5 * 2 + 7 - 3
        ValDerivationNode val5 = new ValDerivationNode(new LiteralInt(5), new VarDerivationNode("a"));
        ValDerivationNode val2 = new ValDerivationNode(new LiteralInt(2), null);
        BinaryDerivationNode mult5Times2 = new BinaryDerivationNode(val5, val2, "*");
        ValDerivationNode val10 = new ValDerivationNode(new LiteralInt(10), mult5Times2);

        ValDerivationNode val7 = new ValDerivationNode(new LiteralInt(7), new VarDerivationNode("b"));
        BinaryDerivationNode add10Plus7 = new BinaryDerivationNode(val10, val7, "+");
        ValDerivationNode val17 = new ValDerivationNode(new LiteralInt(17), add10Plus7);

        ValDerivationNode val3 = new ValDerivationNode(new LiteralInt(3), null);
        BinaryDerivationNode sub17Minus3 = new BinaryDerivationNode(val17, val3, "-");
        ValDerivationNode val14Left = new ValDerivationNode(new LiteralInt(14), sub17Minus3);

        // 14 from variable c
        ValDerivationNode val14Right = new ValDerivationNode(new LiteralInt(14), new VarDerivationNode("c"));

        // 14 == 14
        BinaryDerivationNode compare14 = new BinaryDerivationNode(val14Left, val14Right, "==");
        ValDerivationNode trueFromComparison = new ValDerivationNode(new LiteralBoolean(true), compare14);

        // a == 5 => true
        ValDerivationNode val5ForCompA = new ValDerivationNode(new LiteralInt(5), new VarDerivationNode("a"));
        ValDerivationNode val5Literal = new ValDerivationNode(new LiteralInt(5), null);
        BinaryDerivationNode compareA5 = new BinaryDerivationNode(val5ForCompA, val5Literal, "==");
        ValDerivationNode trueFromA = new ValDerivationNode(new LiteralBoolean(true), compareA5);

        // b == 7 => true
        ValDerivationNode val7ForCompB = new ValDerivationNode(new LiteralInt(7), new VarDerivationNode("b"));
        ValDerivationNode val7Literal = new ValDerivationNode(new LiteralInt(7), null);
        BinaryDerivationNode compareB7 = new BinaryDerivationNode(val7ForCompB, val7Literal, "==");
        ValDerivationNode trueFromB = new ValDerivationNode(new LiteralBoolean(true), compareB7);

        // (a == 5) && (b == 7) => true
        BinaryDerivationNode andAB = new BinaryDerivationNode(trueFromA, trueFromB, "&&");
        ValDerivationNode trueFromAB = new ValDerivationNode(new LiteralBoolean(true), andAB);

        // c == 14 => true
        ValDerivationNode val14ForCompC = new ValDerivationNode(new LiteralInt(14), new VarDerivationNode("c"));
        ValDerivationNode val14Literal = new ValDerivationNode(new LiteralInt(14), null);
        BinaryDerivationNode compareC14 = new BinaryDerivationNode(val14ForCompC, val14Literal, "==");
        ValDerivationNode trueFromC = new ValDerivationNode(new LiteralBoolean(true), compareC14);

        // ((a == 5) && (b == 7)) && (c == 14) => true
        BinaryDerivationNode andABC = new BinaryDerivationNode(trueFromAB, trueFromC, "&&");
        ValDerivationNode trueFromAllConditions = new ValDerivationNode(new LiteralBoolean(true), andABC);

        // 14 == 14 => true
        BinaryDerivationNode finalAnd = new BinaryDerivationNode(trueFromComparison, trueFromAllConditions, "&&");
        ValDerivationNode expected = new ValDerivationNode(new LiteralBoolean(true), finalAnd);

        // Compare the derivation trees
        assertDerivationEquals(expected, result, "");
    }

    @Test
    void testFixedPointSimplification() {
        // Given: x == -y && y == a / b && a == 6 && b == 3
        // Expected: x == -2

        Expression varX = new Var("x");
        Expression varY = new Var("y");
        Expression varA = new Var("a");
        Expression varB = new Var("b");

        Expression aDivB = new BinaryExpression(varA, "/", varB);
        Expression yEqualsADivB = new BinaryExpression(varY, "==", aDivB);
        Expression negY = new UnaryExpression("-", varY);
        Expression xEqualsNegY = new BinaryExpression(varX, "==", negY);
        Expression six = new LiteralInt(6);
        Expression aEquals6 = new BinaryExpression(varA, "==", six);
        Expression three = new LiteralInt(3);
        Expression bEquals3 = new BinaryExpression(varB, "==", three);
        Expression firstAnd = new BinaryExpression(xEqualsNegY, "&&", yEqualsADivB);
        Expression secondAnd = new BinaryExpression(aEquals6, "&&", bEquals3);
        Expression fullExpression = new BinaryExpression(firstAnd, "&&", secondAnd);

        // When
        ValDerivationNode result = ExpressionSimplifier.simplify(fullExpression);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals("x == -2", result.getValue().toString(), "Expected result to be x == -2");

        // Compare derivation tree structure

        // Build the derivation chain for the right side:
        // 6 came from a, 3 came from b
        ValDerivationNode val6FromA = new ValDerivationNode(new LiteralInt(6), new VarDerivationNode("a"));
        ValDerivationNode val3FromB = new ValDerivationNode(new LiteralInt(3), new VarDerivationNode("b"));

        // 6 / 3 -> 2
        BinaryDerivationNode divOrigin = new BinaryDerivationNode(val6FromA, val3FromB, "/");

        // 2 came from y, and y's value came from 6 / 2
        VarDerivationNode yChainedOrigin = new VarDerivationNode("y", divOrigin);
        ValDerivationNode val2FromY = new ValDerivationNode(new LiteralInt(2), yChainedOrigin);

        // -2
        UnaryDerivationNode negOrigin = new UnaryDerivationNode(val2FromY, "-");
        ValDerivationNode rightNode = new ValDerivationNode(new LiteralInt(-2), negOrigin);

        // Left node x has no origin
        ValDerivationNode leftNode = new ValDerivationNode(new Var("x"), null);

        // Root equality
        BinaryDerivationNode rootOrigin = new BinaryDerivationNode(leftNode, rightNode, "==");
        ValDerivationNode expected = new ValDerivationNode(result.getValue(), rootOrigin);

        assertDerivationEquals(expected, result, "Derivation tree structure");
    }

    @Test
    void testSingleEqualityShouldNotSimplify() {
        // Given: x == 1
        // Expected: x == 1 (should not be simplified to "true")

        Expression varX = new Var("x");
        Expression one = new LiteralInt(1);
        Expression xEquals1 = new BinaryExpression(varX, "==", one);

        // When
        ValDerivationNode result = ExpressionSimplifier.simplify(xEquals1);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals("x == 1", result.getValue().toString(),
                "Single equality should not be simplified to a boolean literal");

        // The result should be the original expression unchanged
        assertInstanceOf(BinaryExpression.class, result.getValue(), "Result should still be a binary expression");
        BinaryExpression resultExpr = (BinaryExpression) result.getValue();
        assertEquals("==", resultExpr.getOperator(), "Operator should still be ==");
        assertEquals("x", resultExpr.getFirstOperand().toString(), "Left operand should be x");
        assertEquals("1", resultExpr.getSecondOperand().toString(), "Right operand should be 1");
    }

    @Test
    void testTwoEqualitiesShouldNotSimplify() {
        // Given: x == 1 && y == 2
        // Expected: x == 1 && y == 2 (should not be simplified to "true")

        Expression varX = new Var("x");
        Expression one = new LiteralInt(1);
        Expression xEquals1 = new BinaryExpression(varX, "==", one);

        Expression varY = new Var("y");
        Expression two = new LiteralInt(2);
        Expression yEquals2 = new BinaryExpression(varY, "==", two);
        Expression fullExpression = new BinaryExpression(xEquals1, "&&", yEquals2);

        // When
        ValDerivationNode result = ExpressionSimplifier.simplify(fullExpression);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals("x == 1 && y == 2", result.getValue().toString(),
                "Two equalities should not be simplified to a boolean literal");

        // The result should be the original expression unchanged
        assertInstanceOf(BinaryExpression.class, result.getValue(), "Result should still be a binary expression");
        BinaryExpression resultExpr = (BinaryExpression) result.getValue();
        assertEquals("&&", resultExpr.getOperator(), "Operator should still be &&");
        assertEquals("x == 1", resultExpr.getFirstOperand().toString(), "Left operand should be x == 1");
        assertEquals("y == 2", resultExpr.getSecondOperand().toString(), "Right operand should be y == 2");
    }

    @Test
    void testSameVarTwiceShouldSimplifyToSingle() {
        // Given: x && x
        // Expected: x

        Expression varX = new Var("x");
        Expression fullExpression = new BinaryExpression(varX, "&&", varX);
        // When

        ValDerivationNode result = ExpressionSimplifier.simplify(fullExpression);
        // Then

        assertNotNull(result, "Result should not be null");
        assertEquals("x", result.getValue().toString(),
                "Same variable twice should be simplified to a single variable");
    }

    @Test
    void testSameEqualityTwiceShouldSimplifyToSingle() {
        // Given: x == 1 && x == 1
        // Expected: x == 1

        Expression varX = new Var("x");
        Expression one = new LiteralInt(1);
        Expression xEquals1First = new BinaryExpression(varX, "==", one);
        Expression xEquals1Second = new BinaryExpression(varX, "==", one);
        Expression fullExpression = new BinaryExpression(xEquals1First, "&&", xEquals1Second);

        // When
        ValDerivationNode result = ExpressionSimplifier.simplify(fullExpression);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals("x == 1", result.getValue().toString(),
                "Same equality twice should be simplified to a single equality");
    }

    @Test
    void testSameExpressionTwiceShouldSimplifyToSingle() {
        // Given: a + b == 1 && a + b == 1
        // Expected: a + b == 1

        Expression varA = new Var("a");
        Expression varB = new Var("b");
        Expression sum = new BinaryExpression(varA, "+", varB);
        Expression one = new LiteralInt(1);
        Expression sumEquals3First = new BinaryExpression(sum, "==", one);
        Expression sumEquals3Second = new BinaryExpression(sum, "==", one);
        Expression fullExpression = new BinaryExpression(sumEquals3First, "&&", sumEquals3Second);

        // When
        ValDerivationNode result = ExpressionSimplifier.simplify(fullExpression);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals("a + b == 1", result.getValue().toString(),
                "Same expression twice should be simplified to a single equality");
    }

    @Test
    void testSymmetricEqualityShouldSimplify() {
        // Given: x == y && y == x
        // Expected: x == y

        Expression varX = new Var("x");
        Expression varY = new Var("y");
        Expression xEqualsY = new BinaryExpression(varX, "==", varY);
        Expression yEqualsX = new BinaryExpression(varY, "==", varX);
        Expression fullExpression = new BinaryExpression(xEqualsY, "&&", yEqualsX);

        // When
        ValDerivationNode result = ExpressionSimplifier.simplify(fullExpression);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals("x == y", result.getValue().toString(),
                "Symmetric equality should be simplified to a single equality");
    }

    @Test
    void testRealExpression() {
        // Given: #a_5 == -#fresh_4 && #fresh_4 == #x_2 / #y_3 && #x_2 == #x_0 && #x_0 == 6 && #y_3 == #y_1 && #y_1 == 3
        // Expected: #a_5 == -2

        Expression varA5 = new Var("#a_5");
        Expression varFresh4 = new Var("#fresh_4");
        Expression varX2 = new Var("#x_2");
        Expression varY3 = new Var("#y_3");
        Expression varX0 = new Var("#x_0");
        Expression varY1 = new Var("#y_1");
        Expression six = new LiteralInt(6);
        Expression three = new LiteralInt(3);
        Expression fresh4EqualsX2DivY3 = new BinaryExpression(varFresh4, "==", new BinaryExpression(varX2, "/", varY3));
        Expression x2EqualsX0 = new BinaryExpression(varX2, "==", varX0);
        Expression x0Equals6 = new BinaryExpression(varX0, "==", six);
        Expression y3EqualsY1 = new BinaryExpression(varY3, "==", varY1);
        Expression y1Equals3 = new BinaryExpression(varY1, "==", three);
        Expression negFresh4 = new UnaryExpression("-", varFresh4);
        Expression a5EqualsNegFresh4 = new BinaryExpression(varA5, "==", negFresh4);
        Expression firstAnd = new BinaryExpression(a5EqualsNegFresh4, "&&", fresh4EqualsX2DivY3);
        Expression secondAnd = new BinaryExpression(x2EqualsX0, "&&", x0Equals6);
        Expression thirdAnd = new BinaryExpression(y3EqualsY1, "&&", y1Equals3);
        Expression firstBigAnd = new BinaryExpression(firstAnd, "&&", secondAnd);
        Expression fullExpression = new BinaryExpression(firstBigAnd, "&&", thirdAnd);

        // When
        ValDerivationNode result = ExpressionSimplifier.simplify(fullExpression);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals("#a_5 == -2", result.getValue().toString(), "Expected result to be #a_5 == -2");

    }

    @Test
    void testTransitive() {
        // Given: a == b && b == 1
        // Expected: a == 1

        Expression varA = new Var("a");
        Expression varB = new Var("b");
        Expression one = new LiteralInt(1);
        Expression aEqualsB = new BinaryExpression(varA, "==", varB);
        Expression bEquals1 = new BinaryExpression(varB, "==", one);
        Expression fullExpression = new BinaryExpression(aEqualsB, "&&", bEquals1);

        // When
        ValDerivationNode result = ExpressionSimplifier.simplify(fullExpression);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals("a == 1", result.getValue().toString(), "Expected result to be a == 1");
    }

    /**
     * Helper method to compare two derivation nodes recursively
     */
    private void assertDerivationEquals(DerivationNode expected, DerivationNode actual, String message) {
        if (expected == null && actual == null)
            return;

        assertNotNull(expected);
        assertEquals(expected.getClass(), actual.getClass(), message + ": node types should match");
        if (expected instanceof ValDerivationNode expectedVal) {
            ValDerivationNode actualVal = (ValDerivationNode) actual;
            assertEquals(expectedVal.getValue().toString(), actualVal.getValue().toString(),
                    message + ": values should match");
            assertDerivationEquals(expectedVal.getOrigin(), actualVal.getOrigin(), message + " > origin");
        } else if (expected instanceof BinaryDerivationNode expectedBin) {
            BinaryDerivationNode actualBin = (BinaryDerivationNode) actual;
            assertEquals(expectedBin.getOp(), actualBin.getOp(), message + ": operators should match");
            assertDerivationEquals(expectedBin.getLeft(), actualBin.getLeft(), message + " > left");
            assertDerivationEquals(expectedBin.getRight(), actualBin.getRight(), message + " > right");
        } else if (expected instanceof VarDerivationNode expectedVar) {
            VarDerivationNode actualVar = (VarDerivationNode) actual;
            assertEquals(expectedVar.getVar(), actualVar.getVar(), message + ": variables should match");
        } else if (expected instanceof UnaryDerivationNode expectedUnary) {
            UnaryDerivationNode actualUnary = (UnaryDerivationNode) actual;
            assertEquals(expectedUnary.getOp(), actualUnary.getOp(), message + ": operators should match");
            assertDerivationEquals(expectedUnary.getOperand(), actualUnary.getOperand(), message + " > operand");
        }
    }
}
