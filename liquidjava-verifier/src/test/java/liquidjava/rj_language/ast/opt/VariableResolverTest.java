package liquidjava.rj_language.opt;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.GroupExpression;
import liquidjava.rj_language.ast.LiteralInt;
import liquidjava.rj_language.ast.UnaryExpression;
import liquidjava.rj_language.ast.Var;

class VariableResolverTest {

    @Test
    void testSingleEqualityNotExtracted() {
        // x == 1 should not extract because it's a single equality
        Expression varX = new Var("x");
        Expression one = new LiteralInt(1);
        Expression xEquals1 = new BinaryExpression(varX, "==", one);
        Map<String, Expression> result = VariableResolver.resolve(xEquals1);
        assertTrue(result.isEmpty(), "Single equality should not extract variable mapping");
    }

    @Test
    void testConjunctionExtractsVariables() {
        // x + y && x == 1 && y == 2 should extract x -> 1, y -> 2
        Expression varX = new Var("x");
        Expression varY = new Var("y");
        Expression one = new LiteralInt(1);
        Expression two = new LiteralInt(2);

        Expression xPlusY = new BinaryExpression(varX, "+", varY);
        Expression xEquals1 = new BinaryExpression(varX, "==", one);
        Expression yEquals2 = new BinaryExpression(varY, "==", two);

        Expression conditions = new BinaryExpression(xEquals1, "&&", yEquals2);
        Expression fullExpr = new BinaryExpression(xPlusY, "&&", conditions);

        Map<String, Expression> result = VariableResolver.resolve(fullExpr);
        assertEquals(2, result.size(), "Should extract both variables");
        assertEquals("1", result.get("x").toString());
        assertEquals("2", result.get("y").toString());
    }

    @Test
    void testSingleComparisonNotExtracted() {
        // x > 0 should not extract anything
        Expression varX = new Var("x");
        Expression zero = new LiteralInt(0);
        Expression xGreaterZero = new BinaryExpression(varX, ">", zero);

        Map<String, Expression> result = VariableResolver.resolve(xGreaterZero);
        assertTrue(result.isEmpty(), "Single comparison should not extract variable mapping");
    }

    @Test
    void testSingleArithmeticExpression() {
        // x + 1 should not extract anything
        Expression varX = new Var("x");
        Expression one = new LiteralInt(1);
        Expression xPlusOne = new BinaryExpression(varX, "+", one);

        Map<String, Expression> result = VariableResolver.resolve(xPlusOne);
        assertTrue(result.isEmpty(), "Single arithmetic expression should not extract variable mapping");
    }

    @Test
    void testDisjunctionWithEqualities() {
        // x == 1 || y == 2 should not extract anything
        Expression varX = new Var("x");
        Expression varY = new Var("y");
        Expression one = new LiteralInt(1);
        Expression two = new LiteralInt(2);

        Expression xEquals1 = new BinaryExpression(varX, "==", one);
        Expression yEquals2 = new BinaryExpression(varY, "==", two);
        Expression disjunction = new BinaryExpression(xEquals1, "||", yEquals2);

        Map<String, Expression> result = VariableResolver.resolve(disjunction);
        assertTrue(result.isEmpty(), "Disjunction should not extract variable mappings");
    }

    @Test
    void testNegatedEquality() {
        // !(x == 1) should not extract because it's a single equality
        Expression varX = new Var("x");
        Expression one = new LiteralInt(1);
        Expression xEquals1 = new BinaryExpression(varX, "==", one);
        Expression notXEquals1 = new UnaryExpression("!", xEquals1);

        Map<String, Expression> result = VariableResolver.resolve(notXEquals1);
        assertTrue(result.isEmpty(), "Negated equality should not extract variable mapping");
    }

    @Test
    void testGroupedEquality() {
        // (x == 1) should not extract because it's a single equality
        Expression varX = new Var("x");
        Expression one = new LiteralInt(1);
        Expression xEquals1 = new BinaryExpression(varX, "==", one);
        Expression grouped = new GroupExpression(xEquals1);

        Map<String, Expression> result = VariableResolver.resolve(grouped);
        assertTrue(result.isEmpty(), "Grouped single equality should not extract variable mapping");
    }

    @Test
    void testCircularDependency() {
        // x == y && y == x should not extract anything due to circular dependency
        Expression varX = new Var("x");
        Expression varY = new Var("y");

        Expression xEqualsY = new BinaryExpression(varX, "==", varY);
        Expression yEqualsX = new BinaryExpression(varY, "==", varX);
        Expression conjunction = new BinaryExpression(xEqualsY, "&&", yEqualsX);

        Map<String, Expression> result = VariableResolver.resolve(conjunction);
        assertTrue(result.isEmpty(), "Circular dependency should not extract variable mappings");
    }

    @Test
    void testUnusedEqualitiesShouldBeIgnored() {
        // z > 0 && x == 1 && y == 2 && z == 3
        Expression varX = new Var("x");
        Expression varY = new Var("y");
        Expression varZ = new Var("z");
        Expression one = new LiteralInt(1);
        Expression two = new LiteralInt(2);
        Expression three = new LiteralInt(3);
        Expression zero = new LiteralInt(0);
        Expression zGreaterZero = new BinaryExpression(varZ, ">", zero);
        Expression xEquals1 = new BinaryExpression(varX, "==", one);
        Expression yEquals2 = new BinaryExpression(varY, "==", two);
        Expression zEquals3 = new BinaryExpression(varZ, "==", three);
        Expression conditions = new BinaryExpression(xEquals1, "&&", new BinaryExpression(yEquals2, "&&", zEquals3));
        Expression fullExpr = new BinaryExpression(zGreaterZero, "&&", conditions);
        Map<String, Expression> result = VariableResolver.resolve(fullExpr);
        assertEquals(1, result.size(), "Should only extract used variable z");
        assertEquals("3", result.get("z").toString());
    }
}
