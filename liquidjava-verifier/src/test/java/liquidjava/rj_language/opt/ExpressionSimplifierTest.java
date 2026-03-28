package liquidjava.rj_language.opt;

import static org.junit.jupiter.api.Assertions.*;
import static liquidjava.utils.TestUtils.*;

import java.util.List;
import java.util.Map;

import liquidjava.processor.facade.AliasDTO;
import liquidjava.rj_language.ast.AliasInvocation;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.Ite;
import liquidjava.rj_language.ast.LiteralBoolean;
import liquidjava.rj_language.ast.LiteralInt;
import liquidjava.rj_language.ast.UnaryExpression;
import liquidjava.rj_language.ast.Var;
import liquidjava.rj_language.opt.derivation_node.BinaryDerivationNode;
import liquidjava.rj_language.opt.derivation_node.IteDerivationNode;
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
        assertFalse((result.getValue()).isBooleanTrue(), "Expected result to be false");

        // (y || true) && y == false => false || true = true
        ValDerivationNode valFalseForY = new ValDerivationNode(new LiteralBoolean(false), new VarDerivationNode("y"));
        ValDerivationNode valTrue1 = new ValDerivationNode(new LiteralBoolean(true), null);
        BinaryDerivationNode orFalseTrue = new BinaryDerivationNode(valFalseForY, valTrue1, "||");
        ValDerivationNode trueFromOr = new ValDerivationNode(new LiteralBoolean(true), orFalseTrue);

        // !true = false
        ValDerivationNode falseFromNot = new ValDerivationNode(new LiteralBoolean(false), null);

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

        // -5 is a literal with no origin
        ValDerivationNode valNeg5 = new ValDerivationNode(new LiteralInt(-5), null);

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

        // Then: boolean literals are unwrapped to show the verified conditions
        assertNotNull(result, "Result should not be null");
        assertNotNull(result.getValue(), "Result value should not be null");
        assertEquals("14 == 14 && 5 == 5 && 7 == 7 && 14 == 14", result.getValue().toString(),
                "All verified conditions should be visible instead of collapsed to true");

        // 5 * 2 + 7 - 3 = 14
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

        // 14 == 14 (unwrapped from true)
        BinaryDerivationNode compare14 = new BinaryDerivationNode(val14Left, val14Right, "==");
        Expression expr14Eq14 = new BinaryExpression(new LiteralInt(14), "==", new LiteralInt(14));
        ValDerivationNode compare14Node = new ValDerivationNode(expr14Eq14, compare14);

        // a == 5 => 5 == 5 (unwrapped from true)
        ValDerivationNode val5ForCompA = new ValDerivationNode(new LiteralInt(5), new VarDerivationNode("a"));
        ValDerivationNode val5Literal = new ValDerivationNode(new LiteralInt(5), null);
        BinaryDerivationNode compareA5 = new BinaryDerivationNode(val5ForCompA, val5Literal, "==");
        Expression expr5Eq5 = new BinaryExpression(new LiteralInt(5), "==", new LiteralInt(5));
        ValDerivationNode compare5Node = new ValDerivationNode(expr5Eq5, compareA5);

        // b == 7 => 7 == 7 (unwrapped from true)
        ValDerivationNode val7ForCompB = new ValDerivationNode(new LiteralInt(7), new VarDerivationNode("b"));
        ValDerivationNode val7Literal = new ValDerivationNode(new LiteralInt(7), null);
        BinaryDerivationNode compareB7 = new BinaryDerivationNode(val7ForCompB, val7Literal, "==");
        Expression expr7Eq7 = new BinaryExpression(new LiteralInt(7), "==", new LiteralInt(7));
        ValDerivationNode compare7Node = new ValDerivationNode(expr7Eq7, compareB7);

        // (5 == 5) && (7 == 7) (unwrapped from true)
        BinaryDerivationNode andAB = new BinaryDerivationNode(compare5Node, compare7Node, "&&");
        Expression expr5And7 = new BinaryExpression(expr5Eq5, "&&", expr7Eq7);
        ValDerivationNode and5And7Node = new ValDerivationNode(expr5And7, andAB);

        // c == 14 => 14 == 14 (unwrapped from true)
        ValDerivationNode val14ForCompC = new ValDerivationNode(new LiteralInt(14), new VarDerivationNode("c"));
        ValDerivationNode val14Literal = new ValDerivationNode(new LiteralInt(14), null);
        BinaryDerivationNode compareC14 = new BinaryDerivationNode(val14ForCompC, val14Literal, "==");
        Expression expr14Eq14b = new BinaryExpression(new LiteralInt(14), "==", new LiteralInt(14));
        ValDerivationNode compare14bNode = new ValDerivationNode(expr14Eq14b, compareC14);

        // ((5 == 5) && (7 == 7)) && (14 == 14) (unwrapped from true)
        BinaryDerivationNode andABC = new BinaryDerivationNode(and5And7Node, compare14bNode, "&&");
        Expression exprConditions = new BinaryExpression(expr5And7, "&&", expr14Eq14b);
        ValDerivationNode conditionsNode = new ValDerivationNode(exprConditions, andABC);

        // (14 == 14) && ((5 == 5 && 7 == 7) && 14 == 14)
        BinaryDerivationNode finalAnd = new BinaryDerivationNode(compare14Node, conditionsNode, "&&");
        ValDerivationNode expected = new ValDerivationNode(result.getValue(), finalAnd);

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

    @Test
    void testShouldNotOversimplifyToTrue() {
        // Given: x > 5 && x == y && y == 10
        // Iteration 1: resolves y == 10, substitutes y -> 10: x > 5 && x == 10
        // Iteration 2: resolves x == 10, substitutes x -> 10: 10 > 5 && 10 == 10 -> true
        // Expected: x > 5 && x == 10 (should NOT simplify to true)

        Expression varX = new Var("x");
        Expression varY = new Var("y");
        Expression five = new LiteralInt(5);
        Expression ten = new LiteralInt(10);

        Expression xGreater5 = new BinaryExpression(varX, ">", five);
        Expression xEqualsY = new BinaryExpression(varX, "==", varY);
        Expression yEquals10 = new BinaryExpression(varY, "==", ten);

        Expression firstAnd = new BinaryExpression(xGreater5, "&&", xEqualsY);
        Expression fullExpression = new BinaryExpression(firstAnd, "&&", yEquals10);

        // When
        ValDerivationNode result = ExpressionSimplifier.simplify(fullExpression);

        // Then
        assertNotNull(result, "Result should not be null");
        assertFalse(result.getValue() instanceof LiteralBoolean,
                "Should not oversimplify to a boolean literal, but got: " + result.getValue());
        assertEquals("x > 5 && x == 10", result.getValue().toString(),
                "Should stop simplification before collapsing to true");
    }

    @Test
    void testShouldUnwrapBooleanInEquality() {
        // Given: x == (1 > 0)
        // Without unwrapping: x == true (unhelpful - hides what "true" came from)
        // Expected: x == 1 > 0 (unwrapped to show the original comparison)

        Expression varX = new Var("x");
        Expression one = new LiteralInt(1);
        Expression zero = new LiteralInt(0);
        Expression oneGreaterZero = new BinaryExpression(one, ">", zero);
        Expression fullExpression = new BinaryExpression(varX, "==", oneGreaterZero);

        // When
        ValDerivationNode result = ExpressionSimplifier.simplify(fullExpression);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals("x == 1 > 0", result.getValue().toString(),
                "Boolean in equality should be unwrapped to show the original comparison");
    }

    @Test
    void testShouldUnwrapBooleanInEqualityWithPropagation() {
        // Given: x == (a > b) && a == 3 && b == 1
        // Without unwrapping: x == true (unhelpful)
        // Expected: x == 3 > 1 (unwrapped and propagated)

        Expression varX = new Var("x");
        Expression varA = new Var("a");
        Expression varB = new Var("b");
        Expression aGreaterB = new BinaryExpression(varA, ">", varB);
        Expression xEqualsComp = new BinaryExpression(varX, "==", aGreaterB);

        Expression three = new LiteralInt(3);
        Expression aEquals3 = new BinaryExpression(varA, "==", three);
        Expression one = new LiteralInt(1);
        Expression bEquals1 = new BinaryExpression(varB, "==", one);

        Expression conditions = new BinaryExpression(aEquals3, "&&", bEquals1);
        Expression fullExpression = new BinaryExpression(xEqualsComp, "&&", conditions);

        // When
        ValDerivationNode result = ExpressionSimplifier.simplify(fullExpression);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals("x == 3 > 1", result.getValue().toString(),
                "Boolean in equality should be unwrapped after propagation");
    }

    @Test
    void testShouldNotUnwrapBooleanWithBooleanChildren() {
        // Given: (y || true) && !true && y == false
        // Expected: false (both children of the fold are boolean, so no unwrapping needed)

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

        // Then: false stays as false since both sides in the derivation are booleans
        assertNotNull(result, "Result should not be null");
        assertInstanceOf(LiteralBoolean.class, result.getValue(), "Result should remain a boolean");
        assertFalse(result.getValue().isBooleanTrue(), "Expected result to be false");
    }

    @Test
    void testShouldUnwrapNestedBooleanInEquality() {
        // Given: x == (a + b > 10) && a == 3 && b == 5
        // Without unwrapping: x == true (unhelpful)
        // Expected: x == 8 > 10 (shows the actual comparison that produced the boolean)

        Expression varX = new Var("x");
        Expression varA = new Var("a");
        Expression varB = new Var("b");
        Expression aPlusB = new BinaryExpression(varA, "+", varB);
        Expression ten = new LiteralInt(10);
        Expression comparison = new BinaryExpression(aPlusB, ">", ten);
        Expression xEqualsComp = new BinaryExpression(varX, "==", comparison);

        Expression three = new LiteralInt(3);
        Expression aEquals3 = new BinaryExpression(varA, "==", three);
        Expression five = new LiteralInt(5);
        Expression bEquals5 = new BinaryExpression(varB, "==", five);

        Expression conditions = new BinaryExpression(aEquals3, "&&", bEquals5);
        Expression fullExpression = new BinaryExpression(xEqualsComp, "&&", conditions);

        // When
        ValDerivationNode result = ExpressionSimplifier.simplify(fullExpression);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals("x == 8 > 10", result.getValue().toString(),
                "Boolean in equality should be unwrapped to show the computed comparison");
    }

    @Test
    void testVarToVarPropagationWithInternalVariable() {
        // Given: #x_0 == a && #x_0 > 5
        // Expected: a > 5 (internal #x_0 substituted with user-facing a)

        Expression varX0 = new Var("#x_0");
        Expression varA = new Var("a");
        Expression x0EqualsA = new BinaryExpression(varX0, "==", varA);
        Expression x0Greater5 = new BinaryExpression(varX0, ">", new LiteralInt(5));
        Expression fullExpression = new BinaryExpression(x0EqualsA, "&&", x0Greater5);

        // When
        ValDerivationNode result = ExpressionSimplifier.simplify(fullExpression);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals("a > 5", result.getValue().toString(),
                "Internal variable #x_0 should be substituted with user-facing variable a");
    }

    @Test
    void testVarToVarInternalToInternal() {
        // Given: #a_1 == #b_2 && #b_2 == 5 && x == #a_1 + 1
        // Expected: x == 5 + 1 = x == 6

        Expression varA = new Var("#a_1");
        Expression varB = new Var("#b_2");
        Expression varX = new Var("x");
        Expression five = new LiteralInt(5);
        Expression aEqualsB = new BinaryExpression(varA, "==", varB);
        Expression bEquals5 = new BinaryExpression(varB, "==", five);
        Expression aPlus1 = new BinaryExpression(varA, "+", new LiteralInt(1));
        Expression xEqualsAPlus1 = new BinaryExpression(varX, "==", aPlus1);
        Expression firstAnd = new BinaryExpression(aEqualsB, "&&", bEquals5);
        Expression fullExpression = new BinaryExpression(firstAnd, "&&", xEqualsAPlus1);

        // When
        ValDerivationNode result = ExpressionSimplifier.simplify(fullExpression);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals("x == 6", result.getValue().toString(),
                "#a should resolve through #b to 5 across passes, then x == 5 + 1 = x == 6");
    }

    @Test
    void testVarToVarDoesNotAffectUserFacingVariables() {
        // Given: x == y && x > 5
        // Expected: x == y && x > 5 (user-facing var-to-var should not be propagated)

        Expression varX = new Var("x");
        Expression varY = new Var("y");
        Expression xEqualsY = new BinaryExpression(varX, "==", varY);
        Expression xGreater5 = new BinaryExpression(varX, ">", new LiteralInt(5));
        Expression fullExpression = new BinaryExpression(xEqualsY, "&&", xGreater5);

        // When
        ValDerivationNode result = ExpressionSimplifier.simplify(fullExpression);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals("x == y && x > 5", result.getValue().toString(),
                "User-facing variable equalities should not trigger var-to-var propagation");
    }

    @Test
    void testVarToVarRemovesRedundantEquality() {
        // Given: #ret_1 == #b_0 - 100 && #b_0 == b && b >= -128 && b <= 127
        // Expected: #ret_1 == b - 100 && b >= -128 && b <= 127 (#b_0 replaced with b, #b_0 == b removed)

        Expression ret1 = new Var("#ret_1");
        Expression b0 = new Var("#b_0");
        Expression b = new Var("b");
        Expression ret1EqB0Minus100 = new BinaryExpression(ret1, "==",
                new BinaryExpression(b0, "-", new LiteralInt(100)));
        Expression b0EqB = new BinaryExpression(b0, "==", b);
        Expression bGeMinus128 = new BinaryExpression(b, ">=", new UnaryExpression("-", new LiteralInt(128)));
        Expression bLe127 = new BinaryExpression(b, "<=", new LiteralInt(127));
        Expression and1 = new BinaryExpression(ret1EqB0Minus100, "&&", b0EqB);
        Expression and2 = new BinaryExpression(bGeMinus128, "&&", bLe127);
        Expression fullExpression = new BinaryExpression(and1, "&&", and2);

        // When
        ValDerivationNode result = ExpressionSimplifier.simplify(fullExpression);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals("#ret_1 == b - 100 && b >= -128 && b <= 127", result.getValue().toString(),
                "Internal variable #b_0 should be replaced with b and redundant equality removed");
        assertNotNull(result.getOrigin(), "Origin should be present showing the var-to-var derivation");
    }

    @Test
    void testInternalToInternalReducesRedundantVariable() {
        // Given: #a_3 == #b_7 && #a_3 > 5
        // Expected: #b_7 > 5 (#a_3 has lower counter, so #a_3 -> #b_7)

        Expression a3 = new Var("#a_3");
        Expression b7 = new Var("#b_7");
        Expression a3EqualsB7 = new BinaryExpression(a3, "==", b7);
        Expression a3Greater5 = new BinaryExpression(a3, ">", new LiteralInt(5));
        Expression fullExpression = new BinaryExpression(a3EqualsB7, "&&", a3Greater5);

        ValDerivationNode result = ExpressionSimplifier.simplify(fullExpression);

        assertNotNull(result);
        assertEquals("#b_7 > 5", result.getValue().toString(),
                "#a_3 (lower counter) should be substituted with #b_7 (higher counter)");
    }

    @Test
    void testInternalToInternalChainWithUserFacingVariableUserFacingFirst() {
        // Given: #b_7 == x && #a_3 == #b_7 && x > 0
        // Expected: x > 0 (#b_7 -> x (user-facing); #a_3 has lower counter so #a_3 -> #b_7)

        Expression a3 = new Var("#a_3");
        Expression b7 = new Var("#b_7");
        Expression x = new Var("x");
        Expression b7EqualsX = new BinaryExpression(b7, "==", x);
        Expression a3EqualsB7 = new BinaryExpression(a3, "==", b7);
        Expression xGreater0 = new BinaryExpression(x, ">", new LiteralInt(0));
        Expression and1 = new BinaryExpression(b7EqualsX, "&&", a3EqualsB7);
        Expression fullExpression = new BinaryExpression(and1, "&&", xGreater0);

        ValDerivationNode result = ExpressionSimplifier.simplify(fullExpression);

        assertNotNull(result);
        assertEquals("x > 0", result.getValue().toString(),
                "Both internal variables should be eliminated via chain resolution");
    }

    @Test
    void testInternalToInternalChainWithUserFacingVariableInternalFirst() {
        // Given: #a_3 == #b_7 && #b_7 == x && x > 0
        // Expected: x > 0 (#a_3 has lower counter so #a_3 -> #b_7; #b_7 -> x (user-facing) overwrites)

        Expression a3 = new Var("#a_3");
        Expression b7 = new Var("#b_7");
        Expression x = new Var("x");
        Expression a3EqualsB7 = new BinaryExpression(a3, "==", b7);
        Expression b7EqualsX = new BinaryExpression(b7, "==", x);
        Expression xGreater0 = new BinaryExpression(x, ">", new LiteralInt(0));
        Expression and1 = new BinaryExpression(a3EqualsB7, "&&", b7EqualsX);
        Expression fullExpression = new BinaryExpression(and1, "&&", xGreater0);

        ValDerivationNode result = ExpressionSimplifier.simplify(fullExpression);

        assertNotNull(result);
        assertEquals("x > 0", result.getValue().toString(),
                "Both internal variables should be eliminated via fixed-point iteration");
    }

    @Test
    void testInternalToInternalBothResolvingToLiteral() {
        // Given: #a_3 == #b_7 && #b_7 == 5
        // Expected: 5 == 5 && 5 == 5 (#a_3 has lower counter so #a_3 -> #b_7; #b_7 -> 5)

        Expression a3 = new Var("#a_3");
        Expression b7 = new Var("#b_7");
        Expression five = new LiteralInt(5);
        Expression a3EqualsB7 = new BinaryExpression(a3, "==", b7);
        Expression b7Equals5 = new BinaryExpression(b7, "==", five);
        Expression fullExpression = new BinaryExpression(a3EqualsB7, "&&", b7Equals5);

        ValDerivationNode result = ExpressionSimplifier.simplify(fullExpression);

        assertNotNull(result);
        assertEquals("5 == 5 && 5 == 5", result.getValue().toString(),
                "#a_3 -> #b_7 -> 5 and #b_7 -> 5; both equalities collapse to 5 == 5");
    }

    @Test
    void testInternalToInternalNoFurtherResolution() {
        // Given: #a_3 == #b_7 && #b_7 + 1 > 0
        // Expected: #b_7 + 1 > 0 (#a_3 has lower counter, so #a_3 -> #b_7)

        Expression a3 = new Var("#a_3");
        Expression b7 = new Var("#b_7");
        Expression a3EqualsB7 = new BinaryExpression(a3, "==", b7);
        Expression b7Plus1 = new BinaryExpression(b7, "+", new LiteralInt(1));
        Expression b7Plus1Greater0 = new BinaryExpression(b7Plus1, ">", new LiteralInt(0));
        Expression fullExpression = new BinaryExpression(a3EqualsB7, "&&", b7Plus1Greater0);

        ValDerivationNode result = ExpressionSimplifier.simplify(fullExpression);

        assertNotNull(result);
        assertEquals("#b_7 + 1 > 0", result.getValue().toString(),
                "#a_3 (lower counter) replaced by #b_7 (higher counter); equality collapses to trivial");
    }

    @Test
    void testIteTrueConditionSimplifiesToThenBranch() {
        // Given: true ? a : b
        // Expected: a

        Expression expr = new Ite(new LiteralBoolean(true), new Var("a"), new Var("b"));

        // When
        ValDerivationNode result = ExpressionSimplifier.simplify(expr);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals("a", result.getValue().toString(), "Expected result to be a");

        ValDerivationNode conditionNode = new ValDerivationNode(new LiteralBoolean(true), null);
        ValDerivationNode thenNode = new ValDerivationNode(new Var("a"), null);
        ValDerivationNode elseNode = new ValDerivationNode(new Var("b"), null);
        IteDerivationNode iteOrigin = new IteDerivationNode(conditionNode, thenNode, elseNode);
        ValDerivationNode expected = new ValDerivationNode(new Var("a"), iteOrigin);

        assertDerivationEquals(expected, result, "");
    }

    @Test
    void testIteFalseConditionSimplifiesToElseBranch() {
        // Given: false ? a : b
        // Expected: b

        Expression expr = new Ite(new LiteralBoolean(false), new Var("a"), new Var("b"));

        // When
        ValDerivationNode result = ExpressionSimplifier.simplify(expr);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals("b", result.getValue().toString(), "Expected result to be b");

        ValDerivationNode conditionNode = new ValDerivationNode(new LiteralBoolean(false), null);
        ValDerivationNode thenNode = new ValDerivationNode(new Var("a"), null);
        ValDerivationNode elseNode = new ValDerivationNode(new Var("b"), null);
        IteDerivationNode iteOrigin = new IteDerivationNode(conditionNode, thenNode, elseNode);
        ValDerivationNode expected = new ValDerivationNode(new Var("b"), iteOrigin);

        assertDerivationEquals(expected, result, "");
    }

    @Test
    void testIteEqualBranchesSimplifiesToBranch() {
        // Given: cond ? b : b
        // Expected: b

        Expression branch = new Var("b");
        Expression expr = new Ite(new Var("cond"), branch, branch.clone());

        // When
        ValDerivationNode result = ExpressionSimplifier.simplify(expr);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals("b", result.getValue().toString(), "Expected result to be b");

        ValDerivationNode conditionNode = new ValDerivationNode(new Var("cond"), null);
        ValDerivationNode thenNode = new ValDerivationNode(new Var("b"), null);
        ValDerivationNode elseNode = new ValDerivationNode(new Var("b"), null);
        IteDerivationNode iteOrigin = new IteDerivationNode(conditionNode, thenNode, elseNode);
        ValDerivationNode expected = new ValDerivationNode(new Var("b"), iteOrigin);

        assertDerivationEquals(expected, result, "");
    }

    @Test
    void testByteAliasExpansion() {
        // Given: Byte(b) with alias Byte(int b) { b >= -128 && b <= 127 }
        AliasDTO byteAlias = new AliasDTO("Byte", List.of("int"), List.of("b"), "b >= -128 && b <= 127");
        byteAlias.parse("");
        Map<String, AliasDTO> aliases = Map.of("Byte", byteAlias);
        Expression exp = new AliasInvocation("Byte", List.of(new Var("b")));

        // When
        ValDerivationNode result = ExpressionSimplifier.simplify(exp, aliases);

        // Then
        assertEquals("Byte(b)", result.getValue().toString());
        assertNotNull(result.getOrigin(), "Origin should contain the expanded body");
        ValDerivationNode origin = (ValDerivationNode) result.getOrigin();
        assertEquals("b >= -128 && b <= 127", origin.getValue().toString());
    }

    @Test
    void testPositiveAliasExpansion() {
        // Given: Positive(x) with alias Positive(int v) { v > 0 }
        AliasDTO positiveAlias = new AliasDTO("Positive", List.of("int"), List.of("v"), "v > 0");
        positiveAlias.parse("");
        Map<String, AliasDTO> aliases = Map.of("Positive", positiveAlias);
        Expression exp = new AliasInvocation("Positive", List.of(new Var("x")));

        // When
        ValDerivationNode result = ExpressionSimplifier.simplify(exp, aliases);

        // Then
        assertEquals("Positive(x)", result.getValue().toString());
        assertNotNull(result.getOrigin(), "Origin should contain the expanded body");
        ValDerivationNode origin = (ValDerivationNode) result.getOrigin();
        assertEquals("x > 0", origin.getValue().toString());
    }

    @Test
    void testTwoArgAliasWithNormalExpression() {
        // Given: Bounded(v, 100) && v > 50 with alias Bounded(int x, int n) { x > 0 && x < n }
        AliasDTO boundedAlias = new AliasDTO("Bounded", List.of("int", "int"), List.of("x", "n"), "x > 0 && x < n");
        boundedAlias.parse("");
        Map<String, AliasDTO> aliases = Map.of("Bounded", boundedAlias);

        Expression varV = new Var("v");
        Expression bounded = new AliasInvocation("Bounded", List.of(varV, new LiteralInt(100)));
        Expression vGt50 = new BinaryExpression(varV, ">", new LiteralInt(50));
        Expression fullExpression = new BinaryExpression(bounded, "&&", vGt50);

        // When
        ValDerivationNode result = ExpressionSimplifier.simplify(fullExpression, aliases);

        // Then
        assertEquals("Bounded(v, 100) && v > 50", result.getValue().toString());
        assertInstanceOf(BinaryDerivationNode.class, result.getOrigin());
        BinaryDerivationNode binOrigin = (BinaryDerivationNode) result.getOrigin();
        assertEquals("&&", binOrigin.getOp());
        ValDerivationNode leftNode = binOrigin.getLeft();
        assertEquals("Bounded(v, 100)", leftNode.getValue().toString());
        assertNotNull(leftNode.getOrigin(), "Alias invocation should have expanded body as origin");
        ValDerivationNode expandedBody = (ValDerivationNode) leftNode.getOrigin();
        assertEquals("v > 0 && v < 100", expandedBody.getValue().toString());
        ValDerivationNode rightNode = binOrigin.getRight();
        assertEquals("v > 50", rightNode.getValue().toString());
        assertNull(rightNode.getOrigin());
    }

    @Test
    void testEntailedConjunctIsRemovedButOriginIsPreserved() {
        // Given: b >= 100 && b > 0
        // Expected: b >= 100 (b >= 100 implies b > 0)

        addIntVariableToContext("b");
        Expression b = new Var("b");
        Expression bGe100 = new BinaryExpression(b, ">=", new LiteralInt(100));
        Expression bGt0 = new BinaryExpression(b, ">", new LiteralInt(0));
        Expression fullExpression = new BinaryExpression(bGe100, "&&", bGt0);

        ValDerivationNode result = ExpressionSimplifier.simplify(fullExpression);

        assertNotNull(result);
        assertEquals("b >= 100", result.getValue().toString(),
                "The weaker conjunct should be removed when implied by the stronger one");

        ValDerivationNode expectedLeft = new ValDerivationNode(bGe100, null);
        ValDerivationNode expectedRight = new ValDerivationNode(bGt0, null);
        ValDerivationNode expected = new ValDerivationNode(bGe100,
                new BinaryDerivationNode(expectedLeft, expectedRight, "&&"));

        assertDerivationEquals(expected, result, "Entailment simplification should preserve conjunction origin");
    }

    @Test
    void testStrictComparisonImpliesNonStrictComparison() {
        // Given: x > y && x >= y
        // Expected: x > y (x > y implies x >= y)

        addIntVariableToContext("x");
        addIntVariableToContext("y");
        Expression x = new Var("x");
        Expression y = new Var("y");
        Expression xGtY = new BinaryExpression(x, ">", y);
        Expression xGeY = new BinaryExpression(x, ">=", y);
        Expression fullExpression = new BinaryExpression(xGtY, "&&", xGeY);

        ValDerivationNode result = ExpressionSimplifier.simplify(fullExpression);

        assertNotNull(result);
        assertEquals("x > y", result.getValue().toString(),
                "The stricter comparison should be kept when it implies the weaker one");

        ValDerivationNode expectedLeft = new ValDerivationNode(xGtY, null);
        ValDerivationNode expectedRight = new ValDerivationNode(xGeY, null);
        ValDerivationNode expected = new ValDerivationNode(xGtY,
                new BinaryDerivationNode(expectedLeft, expectedRight, "&&"));

        assertDerivationEquals(expected, result, "Strict comparison simplification should preserve conjunction origin");
    }

    @Test
    void testEquivalentBoundsKeepOneSide() {
        // Given: i >= 0 && 0 <= i
        // Expected: 0 <= i (both conjuncts express the same condition)
        addIntVariableToContext("i");
        Expression i = new Var("i");
        Expression zeroLeI = new BinaryExpression(new LiteralInt(0), "<=", i);
        Expression iGeZero = new BinaryExpression(i, ">=", new LiteralInt(0));
        Expression fullExpression = new BinaryExpression(zeroLeI, "&&", iGeZero);

        ValDerivationNode result = ExpressionSimplifier.simplify(fullExpression);

        assertNotNull(result);
        assertEquals("0 <= i", result.getValue().toString(), "Equivalent bounds should collapse to a single conjunct");

        ValDerivationNode expectedLeft = new ValDerivationNode(zeroLeI, null);
        ValDerivationNode expectedRight = new ValDerivationNode(iGeZero, null);
        ValDerivationNode expected = new ValDerivationNode(zeroLeI,
                new BinaryDerivationNode(expectedLeft, expectedRight, "&&"));

        assertDerivationEquals(expected, result, "Equivalent bounds simplification should preserve conjunction origin");
    }
}
