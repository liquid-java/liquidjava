package liquidjava.rj_language.parsing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import liquidjava.diagnostics.errors.SyntaxError;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;

class RefinementsParserTest {

    @Test
    void hintsArrowVsImplication() {
        SyntaxError e = assertThrows(SyntaxError.class,
                () -> RefinementsParser.createAST("_ == true -> markSupported(this)", ""));
        assertEquals("Logical implication is --> (two dashes), not ->", e.getDetails());
    }

    @Test
    void hintsAssignVsEquals() {
        SyntaxError e = assertThrows(SyntaxError.class, () -> RefinementsParser.createAST("_ = 1", ""));
        assertEquals("Predicates must be compared with == instead of =", e.getDetails());
    }

    @Test
    void noHintForValidImplication() {
        // double-dash is the real operator — should parse cleanly, no exception
        RefinementsParser.createAST("_ == true --> markSupported(this)", "");
    }

    @Test
    void noHintForUnrelatedSyntaxError() {
        SyntaxError e = assertThrows(SyntaxError.class, () -> RefinementsParser.createAST("a +", ""));
        assertEquals("", e.getDetails());
    }

    @Test
    void parenthesesAreRepresentedByExpressionStructure() {
        Expression expression = RefinementsParser.createAST("(a + b) * c", "");
        BinaryExpression product = (BinaryExpression) expression;
        BinaryExpression sum = (BinaryExpression) product.getFirstOperand();

        assertEquals("*", product.getOperator());
        assertEquals("+", sum.getOperator());
    }
}
