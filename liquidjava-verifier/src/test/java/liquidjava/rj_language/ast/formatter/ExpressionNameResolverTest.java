package liquidjava.rj_language.ast.formatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.parsing.RefinementsParser;

class ExpressionNameResolverTest {

    private static Expression parse(String refinement, String prefix) {
        return RefinementsParser.createAST(refinement, prefix);
    }

    @Test
    void stripsQualifiedNamesWhenUnambiguous() {
        ExpressionNameResolver resolver = ExpressionNameResolver
                .forExpression(parse("size(this) == size(this)", "java.util.List"));

        assertEquals("size", resolver.resolveFunction("java.util.List.size"));
    }

    @Test
    void keepsQualifiedNamesForDifferentPrefixes() {
        Expression expression = new BinaryExpression(parse("size(this)", "java.util.ArrayList"), "==",
                parse("size(this)", "java.util.ArrayDeque"));
        ExpressionNameResolver resolver = ExpressionNameResolver.forExpression(expression);

        assertEquals("java.util.ArrayList.size", resolver.resolveFunction("java.util.ArrayList.size"));
        assertEquals("java.util.ArrayDeque.size", resolver.resolveFunction("java.util.ArrayDeque.size"));
    }

    @Test
    void keepsQualifiedNamesForDifferentInstances() {
        Predicate differentInstances = Predicate.createEquals(Predicate.createVar("#java.util.ArrayList.size_1"),
                Predicate.createVar("#java.util.ArrayDeque.size_2"));
        ExpressionNameResolver resolver = ExpressionNameResolver.forExpression(differentInstances.getExpression());

        assertEquals("java.util.ArrayList.size¹", resolver.resolveVariable("#java.util.ArrayList.size_1"));
        assertEquals("java.util.ArrayDeque.size²", resolver.resolveVariable("#java.util.ArrayDeque.size_2"));
    }
}
