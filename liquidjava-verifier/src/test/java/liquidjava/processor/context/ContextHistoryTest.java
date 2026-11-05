package liquidjava.processor.context;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import liquidjava.api.CommandLineLauncher;
import liquidjava.rj_language.ast.formatter.ExpressionFormatter;

class ContextHistoryTest {

    @Test
    void refinementNamesTheVariableAndTheLineOfItsEarlierValue() {
        CommandLineLauncher.cmdArgs.lspMode = true;
        try {
            CommandLineLauncher.launch("../liquidjava-example/src/main/java/testSuite/ErrorUnaryOperators.java");
        } finally {
            CommandLineLauncher.cmdArgs.lspMode = false;
        }
        ContextHistory history = ContextHistory.getInstance();
        RefinedVariable b = history.getLocalVars().stream().filter(v -> v.getName().equals("b")).findFirst()
                .orElseThrow();
        assertEquals("b == b@19 && b > 0",
                ExpressionFormatter.format(b.getRefinement().getExpression(), history::lineOf));
    }
}
