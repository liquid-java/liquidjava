package liquidjava.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import liquidjava.processor.context.Context;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.*;
import liquidjava.rj_language.opt.derivation_node.ValDerivationNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import spoon.Launcher;

/**
 * Integration tests for Predicate and Expression classes working together Tests realistic scenarios of expression
 * building, manipulation, and evaluation
 */
class PredicateExpressionIntegrationTest {

    @BeforeEach
    void setUp() {
        Launcher launcher = new Launcher();
        launcher.getFactory();
        Context.getInstance().reinitializeAllContext();
    }

    @Test
    void testComplexPredicateConstruction() {
        // Build: (x > 5 && y < 10) || (x == 0 && y == 0)
        Predicate x = Predicate.createVar("x");
        Predicate y = Predicate.createVar("y");
        Predicate five = Predicate.createLit("5", "int");
        Predicate ten = Predicate.createLit("10", "int");
        Predicate zero = Predicate.createLit("0", "int");

        Predicate xGreater5 = Predicate.createOperation(x, ">", five);
        Predicate yLess10 = Predicate.createOperation(y, "<", ten);
        Predicate leftBranch = Predicate.createConjunction(xGreater5, yLess10);

        Predicate xEquals0 = Predicate.createEquals(x, zero);
        Predicate yEquals0 = Predicate.createEquals(y, zero);
        Predicate rightBranch = Predicate.createConjunction(xEquals0, yEquals0);

        Predicate complex = Predicate.createDisjunction(leftBranch, rightBranch);

        String result = complex.toString();
        assertNotNull(result, "Should produce valid string");
        assertTrue(result.contains("x") && result.contains("y"), "Should contain both variables");
        assertTrue(result.contains("||"), "Should contain disjunction");
        assertTrue(result.contains("&&"), "Should contain conjunction");
    }

    @Test
    void testVariableSubstitutionInComplexExpression() {
        // Create: x + y * z
        Predicate x = Predicate.createVar("x");
        Predicate y = Predicate.createVar("y");
        Predicate z = Predicate.createVar("z");

        Predicate yTimesZ = Predicate.createOperation(y, "*", z);
        Predicate expr = Predicate.createOperation(x, "+", yTimesZ);

        // Substitute x with a
        Predicate substituted = expr.substituteVariable("x", "a");
        String result = substituted.toString();

        assertTrue(result.contains("a"), "Should contain substituted variable");
        assertFalse(result.contains("x"), "Should not contain original variable");
        assertTrue(result.contains("y") && result.contains("z"), "Other variables unchanged");
    }

    @Test
    void testExpressionCloningAndModification() {
        // Create expression and clone it
        Predicate original = Predicate.createOperation(Predicate.createVar("x"), "+", Predicate.createLit("10", "int"));

        Predicate cloned = original.clone();

        // Modify clone
        Predicate modified = cloned.substituteVariable("x", "y");

        // Original should be unchanged
        assertTrue(original.toString().contains("x"), "Original unchanged");
        assertTrue(modified.toString().contains("y"), "Clone modified");
        assertFalse(modified.toString().contains("x"), "Clone doesn't have old var");
    }

    @Test
    void testNestedFunctionInvocations() {
        // Create: outer(inner(x, y), z)
        Predicate x = Predicate.createVar("x");
        Predicate y = Predicate.createVar("y");
        Predicate z = Predicate.createVar("z");

        Predicate inner = Predicate.createInvocation("inner", x, y);
        Predicate outer = Predicate.createInvocation("outer", inner, z);

        String result = outer.toString();
        assertTrue(result.contains("outer"), "Should contain outer function");
        assertTrue(result.contains("inner"), "Should contain inner function");
        assertTrue(result.contains("x") && result.contains("y") && result.contains("z"),
                "Should contain all variables");
    }

    @Test
    void testIfThenElsePredicates() {
        // Create: if (x > 0) then x else -x
        Predicate x = Predicate.createVar("x");
        Predicate zero = Predicate.createLit("0", "int");
        Predicate condition = Predicate.createOperation(x, ">", zero);

        Predicate negX = Predicate.createOperation(Predicate.createLit("-1", "int"), "*", x);

        Predicate ite = Predicate.createITE(condition, x, negX);

        assertNotNull(ite, "ITE predicate should be created");
        Expression iteExpr = ite.getExpression();
        assertTrue(iteExpr instanceof Ite, "Should be Ite expression");

        Ite iteNode = (Ite) iteExpr;
        assertNotNull(iteNode.getCondition(), "Should have condition");
        assertNotNull(iteNode.getThen(), "Should have then branch");
        assertNotNull(iteNode.getElse(), "Should have else branch");
    }

    @Test
    void testOldVariableTracking() {
        // Create: old(x) + y
        Predicate oldX = Predicate.createInvocation("old", Predicate.createVar("x"));
        Predicate y = Predicate.createVar("y");
        Predicate expr = Predicate.createOperation(oldX, "+", y);

        List<String> oldVars = expr.getOldVariableNames();
        assertEquals(1, oldVars.size(), "Should have 1 old variable");
        assertTrue(oldVars.contains("x"), "Should contain x");

        // Test changeOldMentions method executes
        Predicate changed = expr.changeOldMentions("x", "newX");
        assertNotNull(changed, "Change should produce result");

        // Verify original expression still accessible
        assertNotNull(expr.getExpression(), "Original expression should exist");
        assertTrue(expr.toString().contains("old"), "Original should still contain old reference");
    }

    @Test
    void testVariableNameExtraction() {
        // Create complex expression and extract all variables
        Predicate expr = Predicate.createOperation(
                Predicate.createOperation(Predicate.createVar("a"), "+", Predicate.createVar("b")), "*",
                Predicate.createOperation(Predicate.createVar("c"), "-", Predicate.createVar("d")));

        List<String> vars = expr.getVariableNames();
        assertEquals(4, vars.size(), "Should find 4 variables");
        assertTrue(vars.contains("a"), "Should contain a");
        assertTrue(vars.contains("b"), "Should contain b");
        assertTrue(vars.contains("c"), "Should contain c");
        assertTrue(vars.contains("d"), "Should contain d");
    }

    @Test
    void testPredicateSimplificationIntegration() {
        // Create: (2 + 3) * 4 - should simplify to 20
        Predicate two = Predicate.createLit("2", "int");
        Predicate three = Predicate.createLit("3", "int");
        Predicate four = Predicate.createLit("4", "int");

        Predicate sum = Predicate.createOperation(two, "+", three);
        Predicate product = Predicate.createOperation(sum, "*", four);

        ValDerivationNode simplified = product.simplify();
        assertNotNull(simplified, "Simplification should succeed");
        assertNotNull(simplified.getValue(), "Should have simplified value");
    }

    @Test
    void testPredicateNegation() {
        // Create predicate and negate it
        Predicate x = Predicate.createVar("x");
        Predicate five = Predicate.createLit("5", "int");
        Predicate xGreater5 = Predicate.createOperation(x, ">", five);

        Predicate negated = xGreater5.negate();
        assertTrue(negated.toString().contains("!"), "Negated should contain !");

        // Double negation
        Predicate doubleNegated = negated.negate();
        assertTrue(doubleNegated.toString().contains("!"), "Should have negation operator");
    }

    @Test
    void testBooleanLiteralPredicates() {
        Predicate truePred = Predicate.createLit("true", "boolean");
        Predicate falsePred = Predicate.createLit("false", "boolean");

        assertTrue(truePred.isBooleanTrue(), "True literal is boolean true");
        assertFalse(falsePred.isBooleanTrue(), "False literal is not boolean true");

        // Combine with logic operators
        Predicate andResult = Predicate.createConjunction(truePred, falsePred);
        assertFalse(andResult.isBooleanTrue(), "true && false = false");

        Predicate orResult = Predicate.createDisjunction(truePred, falsePred);
        assertNotNull(orResult, "true || false should create valid predicate");
    }

    @Test
    void testArithmeticWithVariablesAndConstants() {
        // Create: (x * 2 + 5) / (y - 3)
        Predicate x = Predicate.createVar("x");
        Predicate y = Predicate.createVar("y");
        Predicate two = Predicate.createLit("2", "int");
        Predicate five = Predicate.createLit("5", "int");
        Predicate three = Predicate.createLit("3", "int");

        Predicate xTimes2 = Predicate.createOperation(x, "*", two);
        Predicate numerator = Predicate.createOperation(xTimes2, "+", five);

        Predicate denominator = Predicate.createOperation(y, "-", three);

        Predicate division = Predicate.createOperation(numerator, "/", denominator);

        List<String> vars = division.getVariableNames();
        assertEquals(2, vars.size(), "Should have x and y");

        String result = division.toString();
        assertTrue(result.contains("x") && result.contains("y"), "Should contain both variables");
        assertTrue(result.contains("2") && result.contains("5") && result.contains("3"),
                "Should contain all constants");
    }

    @Test
    void testExpressionWithMixedTypes() {
        // Create expressions with different literal types
        Predicate intVal = Predicate.createLit("42", "int");
        Predicate doubleVal = Predicate.createLit("3.14", "double");
        Predicate boolVal = Predicate.createLit("true", "boolean");
        Predicate longVal = Predicate.createLit("1000000", "long");

        assertNotNull(intVal.getExpression(), "Int literal created");
        assertNotNull(doubleVal.getExpression(), "Double literal created");
        assertNotNull(boolVal.getExpression(), "Boolean literal created");
        assertNotNull(longVal.getExpression(), "Long literal created");

        assertTrue(intVal.getExpression().isLiteral(), "Int is literal");
        assertTrue(doubleVal.getExpression().isLiteral(), "Double is literal");
        assertTrue(boolVal.getExpression().isLiteral(), "Boolean is literal");
    }

    @Test
    void testComparisonOperations() {
        // Test all comparison operators
        Predicate x = Predicate.createVar("x");
        Predicate five = Predicate.createLit("5", "int");

        Predicate eq = Predicate.createEquals(x, five);
        Predicate lt = Predicate.createOperation(x, "<", five);
        Predicate gt = Predicate.createOperation(x, ">", five);
        Predicate lte = Predicate.createOperation(x, "<=", five);
        Predicate gte = Predicate.createOperation(x, ">=", five);
        Predicate neq = Predicate.createOperation(x, "!=", five);

        assertTrue(eq.toString().contains("=="), "Equals uses ==");
        assertTrue(lt.toString().contains("<"), "Less than uses <");
        assertTrue(gt.toString().contains(">"), "Greater than uses >");
        assertTrue(lte.toString().contains("<="), "Less or equal uses <=");
        assertTrue(gte.toString().contains(">="), "Greater or equal uses >=");
        assertTrue(neq.toString().contains("!="), "Not equal uses !=");
    }

    @Test
    void testComplexSubstitutionScenario() {
        // Scenario: Function call with arguments that need substitution
        // Original: f(x, y) where we want to substitute x -> a+b and y -> c*d
        Predicate a = Predicate.createVar("a");
        Predicate b = Predicate.createVar("b");
        Predicate c = Predicate.createVar("c");
        Predicate d = Predicate.createVar("d");

        Predicate aPlusB = Predicate.createOperation(a, "+", b);
        Predicate cTimesD = Predicate.createOperation(c, "*", d);

        // Create function invocation
        Predicate func = Predicate.createInvocation("f", aPlusB, cTimesD);

        List<String> vars = func.getVariableNames();
        assertTrue(vars.contains("a") && vars.contains("b") && vars.contains("c") && vars.contains("d"),
                "Should contain all nested variables");
    }

    @Test
    void testChainedOperations() {
        // Create: a + b + c + d
        Predicate a = Predicate.createVar("a");
        Predicate b = Predicate.createVar("b");
        Predicate c = Predicate.createVar("c");
        Predicate d = Predicate.createVar("d");

        Predicate ab = Predicate.createOperation(a, "+", b);
        Predicate abc = Predicate.createOperation(ab, "+", c);
        Predicate abcd = Predicate.createOperation(abc, "+", d);

        List<String> vars = abcd.getVariableNames();
        assertEquals(4, vars.size(), "Should have all 4 variables");

        // Verify structure is maintained
        Expression expr = abcd.getExpression();
        assertTrue(expr instanceof BinaryExpression, "Top level should be binary");
        assertTrue(expr.hasChildren(), "Should have children");
    }

    @Test
    void testPredicateEquality() {
        // Test that identical predicates are recognized
        Predicate p1 = Predicate.createOperation(Predicate.createVar("x"), "+", Predicate.createLit("5", "int"));
        Predicate p2 = Predicate.createOperation(Predicate.createVar("x"), "+", Predicate.createLit("5", "int"));

        assertEquals(p1.toString(), p2.toString(), "Identical predicates have same string form");

        // Different predicates
        Predicate p3 = Predicate.createOperation(Predicate.createVar("x"), "+", Predicate.createLit("6", "int"));

        assertNotEquals(p1.toString(), p3.toString(), "Different predicates have different strings");
    }
}
