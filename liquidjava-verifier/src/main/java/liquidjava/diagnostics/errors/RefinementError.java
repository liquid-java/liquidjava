package liquidjava.diagnostics.errors;

import liquidjava.diagnostics.TranslationTable;
import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtElement;

/**
 * Error indicating that a refinement constraint either was violated or cannot be proven
 * 
 * @see LJError
 */
public class RefinementError extends LJError {

    private final Predicate expected;
    private final VCImplication found;
    private final String element;
    private final String moreInfo;

    public RefinementError(CtElement element, Predicate expected, VCImplication found,
            TranslationTable translationTable, String customMessage) {
        super("Refinement Error",
                String.format("%s is not a subtype of %s", found.toPredicate().getExpression().toDisplayString(),
                        expected.getExpression().toDisplayString()),
                element.getPosition(), translationTable, customMessage);
        this.expected = expected;
        this.found = found;
        this.element = element.toString();
        this.moreInfo = customMessage != null ? customMessage : getInvocationInfo(element);
    }

    @Override
    public String getTitleMessage() {
        return "Type expected:" + formatExpected();
    }

    @Override
    public String getFullMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(SEPARATOR).append("\n");
        sb.append("Failed to check refinement at: \n\n");
        if (moreInfo != null)
            sb.append(moreInfo).append("\n");
        sb.append(element).append("\n\n");
        sb.append("Type expected:").append(formatExpected()).append("\n");
        sb.append("Refinement found:").append(formatFound()).append("\n");
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

    private String formatExpected() {
        return "(" + expected + ")";
    }

    private String formatFound() {
        if (!found.hasBinder() && !found.hasNext() && found.getRefinement().isBooleanTrue())
            return "true";

        StringBuilder sb = new StringBuilder("true");
        for (VCImplication implication = found; implication != null; implication = implication.getNext())
            sb.append(" && ").append(implication.getRefinement());
        return sb.toString();
    }

    private static String getInvocationInfo(CtElement element) {
        if (!(element instanceof CtInvocation<?> invocation))
            return null;

        String invocationText = invocation.getExecutable().toString();
        if (invocation.getTarget() != null) {
            int targetLength = invocation.getTarget().toString().length();
            invocationText = invocation.toString().substring(targetLength + 1);
        }
        return "Method invocation " + invocationText + " in:";
    }
}
