package liquidjava.rj_language.opt;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.parsing.RefinementsParser;

class VariableResolverTest {

    private static Expression parse(String refinement) {
        return RefinementsParser.createAST(refinement, "");
    }

    @Test
    void testSingleEqualityNotExtracted() {
        Expression expression = parse("x == 1");
        Map<String, Expression> result = VariableResolver.resolve(expression);

        assertTrue(result.isEmpty(), "Single equality should not extract variable mapping");
    }

    @Test
    void testConjunctionExtractsVariables() {
        Expression expression = parse("x + y && x == 1 && y == 2");
        Map<String, Expression> result = VariableResolver.resolve(expression);

        assertEquals(2, result.size(), "Should extract both variables");
        assertEquals("1", result.get("x").toString());
        assertEquals("2", result.get("y").toString());
    }

    @Test
    void testSingleComparisonNotExtracted() {
        Expression expression = parse("x > 0");
        Map<String, Expression> result = VariableResolver.resolve(expression);

        assertTrue(result.isEmpty(), "Single comparison should not extract variable mapping");
    }

    @Test
    void testSingleArithmeticExpression() {
        Expression expression = parse("x + 1");
        Map<String, Expression> result = VariableResolver.resolve(expression);

        assertTrue(result.isEmpty(), "Single arithmetic expression should not extract variable mapping");
    }

    @Test
    void testDisjunctionWithEqualities() {
        Expression expression = parse("x == 1 || y == 2");
        Map<String, Expression> result = VariableResolver.resolve(expression);

        assertTrue(result.isEmpty(), "Disjunction should not extract variable mappings");
    }

    @Test
    void testNegatedEquality() {
        Expression expression = parse("!(x == 1)");
        Map<String, Expression> result = VariableResolver.resolve(expression);

        assertTrue(result.isEmpty(), "Negated equality should not extract variable mapping");
    }

    @Test
    void testGroupedEquality() {
        Expression expression = parse("(x == 1)");
        Map<String, Expression> result = VariableResolver.resolve(expression);

        assertTrue(result.isEmpty(), "Grouped single equality should not extract variable mapping");
    }

    @Test
    void testCircularDependency() {
        Expression expression = parse("x == y && y == x");
        Map<String, Expression> result = VariableResolver.resolve(expression);

        assertTrue(result.isEmpty(), "Circular dependency should not extract variable mappings");
    }

    @Test
    void testUnusedEqualitiesShouldBeIgnored() {
        Expression expression = parse("z > 0 && x == 1 && y == 2 && z == 3");
        Map<String, Expression> result = VariableResolver.resolve(expression);

        assertEquals(1, result.size(), "Should only extract used variable z");
        assertEquals("3", result.get("z").toString());
    }

    @Test
    void testDifferentEqualityInIteConditionCountsAsUsage() {
        Expression expression = parse("mode == 1 && (mode == 2 ? explicit(param) : start(param))");
        Map<String, Expression> result = VariableResolver.resolve(expression);

        assertEquals(1, result.size(), "Ternary condition should count as a use of mode");
        assertEquals("1", result.get("mode").toString());
    }

    @Test
    void testRepeatedEqualDefinitionCountsAsUsage() {
        Expression expression = parse("mode == 2 && (mode == 2 ? explicit(param) : start(param))");
        Map<String, Expression> result = VariableResolver.resolve(expression);

        assertEquals(1, result.size(), "Repeated equalities should keep one definition and treat the other as usage");
        assertEquals("2", result.get("mode").toString());
    }

    @Test
    void testReturnVariableIsNotSubstituted() {
        Expression expression = parse("x > 0 && #ret_1 == x");
        Map<String, Expression> result = VariableResolver.resolve(expression);

        assertTrue(result.isEmpty(), "Return variables should not be substituted with another variable");
    }

    @Test
    void testFreshVariableIsNotUsedAsSubstitutionTarget() {
        Expression expression = parse("#tmp_1 > 0 && #tmp_1 == #fresh_2");
        Map<String, Expression> result = VariableResolver.resolve(expression);

        assertTrue(result.isEmpty(), "Fresh variables should not replace another variable");
    }

    @Test
    void testFunctionInvocationEqualityExtractsFunctionKey() {
        Expression expression = parse("size(stack) > 0 && size(stack) == 1");
        Map<String, Expression> result = VariableResolver.resolve(expression);

        assertEquals(1, result.size(), "Should extract the function invocation as a substitution key");
        assertEquals("1", result.get("size(stack)").toString());
    }

    @Test
    void testLiteralOnLeftExtractsFunctionInvocationKey() {
        Expression expression = parse("size(stack) > 0 && 1 == size(stack)");
        Map<String, Expression> result = VariableResolver.resolve(expression);

        assertEquals(1, result.size(), "Should extract function invocation equalities from either side");
        assertEquals("1", result.get("size(stack)").toString());
    }

    @Test
    void testFunctionInvocationEqualitiesResolveTransitively() {
        Expression expression = parse("func(a) > 0 && func(a) == func(b) && func(b) == 1");
        Map<String, Expression> result = VariableResolver.resolve(expression);

        assertEquals(2, result.size(), "Should keep the function invocation chain that was used");
        assertEquals("1", result.get("func(a)").toString());
        assertEquals("1", result.get("func(b)").toString());
    }

    @Test
    void testFunctionInvocationNamesAreMatchedStructurally() {
        Expression expression = parse("f(a) > 0 && f(a) == ff(a) + b");
        Map<String, Expression> result = VariableResolver.resolve(expression);

        assertEquals(1, result.size(), "Should not treat ff(a) as a use of f(a)");
        assertEquals("ff(a) + b", result.get("f(a)").toString());
    }

    @Test
    void testUnusedFunctionInvocationEqualityIsIgnored() {
        Expression expression = parse("x > 0 && size(stack) == 1");
        Map<String, Expression> result = VariableResolver.resolve(expression);

        assertTrue(result.isEmpty(), "Function invocation definitions with no usage should be ignored");
    }
}
