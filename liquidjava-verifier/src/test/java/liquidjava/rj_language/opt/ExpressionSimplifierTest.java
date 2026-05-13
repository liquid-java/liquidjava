package liquidjava.rj_language.opt;

import static org.junit.jupiter.api.Assertions.*;
import static liquidjava.utils.TestUtils.*;

import java.util.List;
import java.util.Map;

import liquidjava.processor.facade.AliasDTO;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.LiteralBoolean;
import liquidjava.rj_language.ast.LiteralInt;
import liquidjava.rj_language.ast.Var;
import liquidjava.rj_language.opt.derivation_node.BinaryDerivationNode;
import liquidjava.rj_language.opt.derivation_node.IteDerivationNode;
import liquidjava.rj_language.opt.derivation_node.UnaryDerivationNode;
import liquidjava.rj_language.opt.derivation_node.ValDerivationNode;
import liquidjava.rj_language.opt.derivation_node.VarDerivationNode;
import liquidjava.rj_language.parsing.RefinementsParser;
import org.junit.jupiter.api.Test;

/**
 * Test suite for expression simplification
 */
class ExpressionSimplifierTest {

    private static Expression parse(String sut) {
        return RefinementsParser.createAST(sut, "");
    }

    @Test
    void testNegation() {
        Expression expression = parse("-a && a == 7");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

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
        Expression expression = parse("a + b && a == 3 && b == 5");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

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
        Expression expression = parse("((y || true) && !true) && y == false");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        assertNotNull(result, "Result should not be null");
        assertInstanceOf(LiteralBoolean.class, result.getValue(), "Result should be a boolean");
        assertFalse((result.getValue()).isBooleanTrue(), "Expected result to be false");
    }

    @Test
    void testArithmeticWithConstants() {
        Expression expression = parse("((a / b + (-5)) + x) && (a == 6 && b == 2)");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

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
        Expression expression = parse("a * 2 + b - 3 == c && a == 5 && b == 7 && c == 14");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        // boolean literals are unwrapped to show the verified conditions
        assertNotNull(result, "Result should not be null");
        assertNotNull(result.getValue(), "Result value should not be null");
        assertEquals("14 == 14 && 5 == 5 && 7 == 7 && 14 == 14", result.getValue().toString(),
                "All verified conditions should be visible instead of collapsed to true");
    }

    @Test
    void testFixedPointSimplification() {
        Expression expression = parse("x == -y && y == a / b && a == 6 && b == 3");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

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
        Expression xEquals1 = parse("x == 1");
        ValDerivationNode result = ExpressionSimplifier.simplify(xEquals1);

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
        Expression expression = parse("x == 1 && y == 2");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

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
        Expression expression = parse("x && x");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        assertNotNull(result, "Result should not be null");
        assertEquals("x", result.getValue().toString(),
                "Same variable twice should be simplified to a single variable");
    }

    @Test
    void testSameEqualityTwiceShouldSimplifyToSingle() {
        Expression expression = parse("x == 1 && x == 1");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        assertNotNull(result, "Result should not be null");
        assertEquals("x == 1", result.getValue().toString(),
                "Same equality twice should be simplified to a single equality");
    }

    @Test
    void testSameExpressionTwiceShouldSimplifyToSingle() {
        Expression expression = parse("a + b == 1 && a + b == 1");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        assertNotNull(result, "Result should not be null");
        assertEquals("a + b == 1", result.getValue().toString(),
                "Same expression twice should be simplified to a single equality");
    }

    @Test
    void testSymmetricEqualityShouldSimplify() {
        Expression expression = parse("x == y && y == x");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        assertNotNull(result, "Result should not be null");
        assertEquals("x == y", result.getValue().toString(),
                "Symmetric equality should be simplified to a single equality");
    }

    @Test
    void testRealExpression() {
        String input = "#a_5 == -#fresh_4 && #fresh_4 == #x_2 / #y_3 && #x_2 == #x_0 && #x_0 == 6 && #y_3 == #y_1 && #y_1 == 3";
        Expression expression = parse(input);
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        assertNotNull(result, "Result should not be null");
        assertEquals("#a_5 == -2", result.getValue().toString(), "Expected result to be #a_5 == -2");

    }

    @Test
    void testTransitive() {
        Expression expression = parse("a == b && b == 1");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        assertNotNull(result, "Result should not be null");
        assertEquals("a == 1", result.getValue().toString(), "Expected result to be a == 1");
    }

    @Test
    void testShouldNotOversimplifyToTrue() {
        Expression expression = parse("x > 5 && x == y && y == 10");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        assertNotNull(result, "Result should not be null");
        assertFalse(result.getValue() instanceof LiteralBoolean,
                "Should not oversimplify to a boolean literal, but got: " + result.getValue());
        assertEquals("x > 5 && x == 10", result.getValue().toString(),
                "Should stop simplification before collapsing to true");
    }

    @Test
    void testShouldUnwrapBooleanInEquality() {
        Expression expression = parse("x == (1 > 0)");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        assertNotNull(result, "Result should not be null");
        assertEquals("x == (1 > 0)", result.getValue().toDisplayString(),
                "Boolean in equality should be unwrapped to show the original comparison");
    }

    @Test
    void testShouldUnwrapBooleanInEqualityWithPropagation() {
        Expression expression = parse("x == (a > b) && a == 3 && b == 1");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        assertNotNull(result, "Result should not be null");
        assertEquals("x == (3 > 1)", result.getValue().toDisplayString(),
                "Boolean in equality should be unwrapped after propagation");
    }

    @Test
    void testShouldNotUnwrapBooleanWithBooleanChildren() {
        Expression expression = parse("(y || true) && !true && y == false");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        // false stays as false since both sides in the derivation are booleans
        assertNotNull(result, "Result should not be null");
        assertInstanceOf(LiteralBoolean.class, result.getValue(), "Result should remain a boolean");
        assertFalse(result.getValue().isBooleanTrue(), "Expected result to be false");
    }

    @Test
    void testShouldUnwrapNestedBooleanInEquality() {
        Expression expression = parse("x == (a + b > 10) && a == 3 && b == 5");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        assertNotNull(result, "Result should not be null");
        assertEquals("x == (8 > 10)", result.getValue().toDisplayString(),
                "Boolean in equality should be unwrapped to show the computed comparison");
    }

    @Test
    void testVarToVarPropagationWithInternalVariable() {
        Expression expression = parse("#x_0 == a && #x_0 > 5");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        assertNotNull(result, "Result should not be null");
        assertEquals("a > 5", result.getValue().toString(),
                "Internal variable #x_0 should be substituted with user-facing variable a");
    }

    @Test
    void testVarToVarInternalToInternal() {
        Expression expression = parse("#a_1 == #b_2 && #b_2 == 5 && x == #a_1 + 1");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        assertNotNull(result, "Result should not be null");
        assertEquals("x == 6", result.getValue().toString(),
                "#a should resolve through #b to 5 across passes, then x == 5 + 1 = x == 6");
    }

    @Test
    void testVarToVarDoesNotAffectUserFacingVariables() {
        Expression expression = parse("x == y && x > 5");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        assertNotNull(result, "Result should not be null");
        assertEquals("x == y && x > 5", result.getValue().toString(),
                "User-facing variable equalities should not trigger var-to-var propagation");
    }

    @Test
    void testVarToVarRemovesRedundantEquality() {
        Expression expression = parse("#ret_1 == #b_0 - 100 && #b_0 == b && b >= -128 && b <= 127");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        assertNotNull(result, "Result should not be null");
        assertEquals("#ret_1 == b - 100 && b >= -128 && b <= 127", result.getValue().toString(),
                "Internal variable #b_0 should be replaced with b and redundant equality removed");
        assertNotNull(result.getOrigin(), "Origin should be present showing the var-to-var derivation");
    }

    @Test
    void testInternalToInternalReducesRedundantVariable() {
        Expression expression = parse("#a_3 == #b_7 && #a_3 > 5");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        assertNotNull(result);
        assertEquals("#b_7 > 5", result.getValue().toString(),
                "#a_3 (lower counter) should be substituted with #b_7 (higher counter)");
    }

    @Test
    void testInternalToInternalChainWithUserFacingVariableUserFacingFirst() {
        Expression expression = parse("#b_7 == x && #a_3 == #b_7 && x > 0");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        assertNotNull(result);
        assertEquals("x > 0", result.getValue().toString(),
                "Both internal variables should be eliminated via chain resolution");
    }

    @Test
    void testInternalToInternalChainWithUserFacingVariableInternalFirst() {
        Expression expression = parse("#a_3 == #b_7 && #b_7 == x && x > 0");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        assertNotNull(result);
        assertEquals("x > 0", result.getValue().toString(),
                "Both internal variables should be eliminated via fixed-point iteration");
    }

    @Test
    void testInternalToInternalBothResolvingToLiteral() {
        Expression expression = parse("#a_3 == #b_7 && #b_7 == 5");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        assertNotNull(result);
        assertEquals("5 == 5 && 5 == 5", result.getValue().toString(),
                "#a_3 -> #b_7 -> 5 and #b_7 -> 5; both equalities collapse to 5 == 5");
    }

    @Test
    void testInternalToInternalNoFurtherResolution() {
        Expression expression = parse("#a_3 == #b_7 && #b_7 + 1 > 0");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        assertNotNull(result);
        assertEquals("#b_7 + 1 > 0", result.getValue().toString(),
                "#a_3 (lower counter) replaced by #b_7 (higher counter); equality collapses to trivial");
    }

    @Test
    void testIteTrueConditionSimplifiesToThenBranch() {
        Expression expr = parse("true ? a : b");
        ValDerivationNode result = ExpressionSimplifier.simplify(expr);

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
        Expression expr = parse("false ? a : b");
        ValDerivationNode result = ExpressionSimplifier.simplify(expr);

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
        Expression expr = parse("cond ? b : b");
        ValDerivationNode result = ExpressionSimplifier.simplify(expr);

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
    void testIteConditionUsesEqualityFromConjunction() {
        Expression expr = parse("mode == 1 && (mode == 2 ? explicit(param) : start(param))");
        ValDerivationNode result = ExpressionSimplifier.simplify(expr);

        assertNotNull(result, "Result should not be null");
        assertEquals("start(param)", result.getValue().toString(),
                "mode == 1 should make the mode == 2 ternary condition false");
    }

    @Test
    void testByteAliasExpansion() {
        String sut = "Byte(b)";
        AliasDTO byteAlias = new AliasDTO("Byte", List.of("int"), List.of("b"), "b >= -128 && b <= 127");
        byteAlias.parse("");
        Map<String, AliasDTO> aliases = Map.of("Byte", byteAlias);
        Expression exp = parse(sut);
        ValDerivationNode result = ExpressionSimplifier.simplify(exp, aliases);

        assertEquals("Byte(b)", result.getValue().toString());
        assertNotNull(result.getOrigin(), "Origin should contain the expanded body");
        ValDerivationNode origin = (ValDerivationNode) result.getOrigin();
        assertEquals("b >= -128 && b <= 127", origin.getValue().toString());
    }

    @Test
    void testPositiveAliasExpansion() {
        String sut = "Positive(x)";
        AliasDTO positiveAlias = new AliasDTO("Positive", List.of("int"), List.of("v"), "v > 0");
        positiveAlias.parse("");
        Map<String, AliasDTO> aliases = Map.of("Positive", positiveAlias);
        Expression exp = parse(sut);
        ValDerivationNode result = ExpressionSimplifier.simplify(exp, aliases);

        assertEquals("Positive(x)", result.getValue().toString());
        assertNotNull(result.getOrigin(), "Origin should contain the expanded body");
        ValDerivationNode origin = (ValDerivationNode) result.getOrigin();
        assertEquals("x > 0", origin.getValue().toString());
    }

    @Test
    void testTwoArgAliasWithNormalExpression() {
        String sut = "Bounded(v, 100) && v > 50";
        AliasDTO boundedAlias = new AliasDTO("Bounded", List.of("int", "int"), List.of("x", "n"), "x > 0 && x < n");
        boundedAlias.parse("");
        Map<String, AliasDTO> aliases = Map.of("Bounded", boundedAlias);
        Expression expression = parse(sut);
        ValDerivationNode result = ExpressionSimplifier.simplify(expression, aliases);

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
        String sut = "b >= 100 && b > 0";
        addIntVariableToContext("b");
        Expression expression = parse(sut);
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        assertNotNull(result);
        assertEquals("b >= 100", result.getValue().toString(),
                "The weaker conjunct should be removed when implied by the stronger one");

        BinaryExpression parsed = (BinaryExpression) expression;
        Expression bGe100 = parsed.getFirstOperand();
        Expression bGt0 = parsed.getSecondOperand();
        ValDerivationNode expectedLeft = new ValDerivationNode(bGe100, null);
        ValDerivationNode expectedRight = new ValDerivationNode(bGt0, null);
        ValDerivationNode expected = new ValDerivationNode(bGe100,
                new BinaryDerivationNode(expectedLeft, expectedRight, "&&"));

        assertDerivationEquals(expected, result, "Entailment simplification should preserve conjunction origin");
    }

    @Test
    void testStrictComparisonImpliesNonStrictComparison() {
        String sut = "x > y && x >= y";
        addIntVariableToContext("x");
        addIntVariableToContext("y");
        Expression expression = parse(sut);
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        assertNotNull(result);
        assertEquals("x > y", result.getValue().toString(),
                "The stricter comparison should be kept when it implies the weaker one");

        BinaryExpression parsed = (BinaryExpression) expression;
        Expression xGtY = parsed.getFirstOperand();
        Expression xGeY = parsed.getSecondOperand();
        ValDerivationNode expectedLeft = new ValDerivationNode(xGtY, null);
        ValDerivationNode expectedRight = new ValDerivationNode(xGeY, null);
        ValDerivationNode expected = new ValDerivationNode(xGtY,
                new BinaryDerivationNode(expectedLeft, expectedRight, "&&"));

        assertDerivationEquals(expected, result, "Strict comparison simplification should preserve conjunction origin");
    }

    @Test
    void testEquivalentBoundsKeepOneSide() {
        String sut = "0 <= i && i >= 0";
        addIntVariableToContext("i");
        Expression expression = parse(sut);
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        assertNotNull(result);
        assertEquals("0 <= i", result.getValue().toString(), "Equivalent bounds should collapse to a single conjunct");

        BinaryExpression parsed = (BinaryExpression) expression;
        Expression zeroLeI = parsed.getFirstOperand();
        Expression iGeZero = parsed.getSecondOperand();
        ValDerivationNode expectedLeft = new ValDerivationNode(zeroLeI, null);
        ValDerivationNode expectedRight = new ValDerivationNode(iGeZero, null);
        ValDerivationNode expected = new ValDerivationNode(zeroLeI,
                new BinaryDerivationNode(expectedLeft, expectedRight, "&&"));

        assertDerivationEquals(expected, result, "Equivalent bounds simplification should preserve conjunction origin");
    }

    @Test
    void testSubstitutesVariableDefinedByArithmeticExpression() {
        Expression expression = parse("z == y - 2 && y == x + 1");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        assertNotNull(result, "Result should not be null");
        assertEquals("z == x - 1", result.getValue().toString(), "Expected variable definition to be substituted");
    }

    @Test
    void testFoldsAdjacentIntegerConstantsInLeftAssociatedArithmetic() {
        assertEquals("x - 1", ExpressionSimplifier.simplify(parse("x + 1 - 2")).getValue().toString());
        assertEquals("x + 1", ExpressionSimplifier.simplify(parse("x - 1 + 2")).getValue().toString());
        assertEquals("x + 3", ExpressionSimplifier.simplify(parse("x + 1 + 2")).getValue().toString());
        assertEquals("x", ExpressionSimplifier.simplify(parse("x + 1 - 1")).getValue().toString());
    }

    @Test
    void testFunctionInvocationEqualitiesPropagateTransitively() {
        Expression expression = parse("size(x3) == size(x2) - 1 && size(x2) == size(x1) + 1 && size(x1) == 0");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        assertEquals("size(x3) == 0", result.getValue().toString());
    }

    @Test
    void testFunctionInvocationOnLeftBehavesLikeVariable() {
        Expression expression = parse("func(a) == func(b) && func(b) == 1");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        assertEquals("func(a) == 1", result.getValue().toString());
    }

    @Test
    void testFunctionInvocationEqualitiesMixWithVariables() {
        Expression expression = parse("func(a) + x && func(a) == y && y == 1 && x == 2");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        assertEquals("3", result.getValue().toString());
    }

    @Test
    void testEnumConstantsPropagateIntoVariableEquality() {
        Expression expression = parse("current == mode && mode == Mode.Photo");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        assertEquals("current == Mode.Photo", result.getValue().toString());
    }

    @Test
    void testEnumConstantsPropagateTransitively() {
        Expression expression = parse("target == current && current == mode && mode == Mode.Photo");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        assertEquals("target == Mode.Photo", result.getValue().toString());
    }

    @Test
    void testEnumConstantsPropagateThroughFunctionInvocations() {
        Expression expression = parse("modeOf(x) == Mode.Photo && current == modeOf(x)");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        assertEquals("current == Mode.Photo", result.getValue().toString());
    }

    @Test
    void testEnumConstantsPropagateIntoTernaryCondition() {
        Expression expression = parse("mode == Mode.Photo && (mode == Mode.Video ? explicit(param) : start(param))");
        ValDerivationNode result = ExpressionSimplifier.simplify(expression);

        assertEquals("start(param)", result.getValue().toString());
    }
}
