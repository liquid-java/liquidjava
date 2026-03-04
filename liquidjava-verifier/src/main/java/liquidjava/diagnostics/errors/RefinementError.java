package liquidjava.diagnostics.errors;

import liquidjava.diagnostics.TranslationTable;
import liquidjava.rj_language.opt.derivation_node.ValDerivationNode;
import liquidjava.smt.Counterexample;
import spoon.reflect.cu.SourcePosition;

/**
 * Error indicating that a refinement constraint either was violated or cannot be proven
 * 
 * @see LJError
 */
public class RefinementError extends LJError {

    private final ValDerivationNode expected;
    private final ValDerivationNode found;
    private final Counterexample counterexample;

    public RefinementError(SourcePosition position, ValDerivationNode expected, ValDerivationNode found,
            TranslationTable translationTable, Counterexample counterexample, String customMessage) {
        super("Refinement Error", String.format("%s is not a subtype of %s", found.getValue(), expected.getValue()),
                position, translationTable, customMessage);
        this.expected = expected;
        this.found = found;
        this.counterexample = counterexample;
    }

    @Override
    public String getDetails() {
        String counterexampleString = getCounterExampleString();
        if (counterexampleString == null)
            return "";
        return "Counterexample: " + counterexampleString;
    }

    public String getCounterExampleString() {
        if (counterexample == null || counterexample.assignments().isEmpty())
            return null;

        // join assignements with &&
        String counterexampleExp = String.join(" && ", counterexample.assignments());

        // check if counterexample is trivial (same as the found value)
        if (counterexampleExp.equals(found.getValue().toString()))
            return null;

        return counterexampleExp;
    }

    public Counterexample getCounterexample() {
        return counterexample;
    }

    public ValDerivationNode getExpected() {
        return expected;
    }

    public ValDerivationNode getFound() {
        return found;
    }
}