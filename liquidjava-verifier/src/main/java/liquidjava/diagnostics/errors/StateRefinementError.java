package liquidjava.diagnostics.errors;

import liquidjava.diagnostics.TranslationTable;
import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import spoon.reflect.declaration.CtElement;

/**
 * Error indicating that a state refinement transition was violated
 * 
 * @see LJError
 */
public class StateRefinementError extends LJError {

    private final Predicate expected;
    private final VCImplication found;
    private final String element;

    public StateRefinementError(CtElement element, Predicate expected, VCImplication found,
            TranslationTable translationTable, String customMessage) {
        super("State Refinement Error",
                String.format("Expected state %s but found %s", expected.getExpression().toDisplayString(),
                        found.toPredicate().getExpression().toDisplayString()),
                element.getPosition(), translationTable, customMessage);
        this.expected = expected;
        this.found = found;
        this.element = element.toString();
    }

    @Override
    public String getTitleMessage() {
        return "Failed to check state transitions. Expected possible states:" + expected;
    }

    @Override
    public String getFullMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(SEPARATOR).append("\n");
        sb.append("Failed to check state transitions when calling ").append(element).append(" in:\n\n");
        sb.append(element).append("\n\n");
        sb.append("Expected possible states:").append(expected).append("\n");
        sb.append("\nState found:\n");
        sb.append(TABLE_SEPARATOR).append("\n");
        sb.append(found).append("\n");
        sb.append(TABLE_SEPARATOR).append("\n\n");
        sb.append(formatTranslationTable());
        sb.append("Location: ").append(getPosition()).append("\n");
        sb.append(SEPARATOR).append("\n");
        return sb.toString();
    }

    public Predicate getExpected() {
        return expected;
    }

    public VCImplication getFound() {
        return found;
    }
}
