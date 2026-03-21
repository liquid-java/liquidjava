package liquidjava.diagnostics.errors;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import liquidjava.diagnostics.TranslationTable;
import liquidjava.rj_language.opt.derivation_node.ValDerivationNode;
import liquidjava.smt.Counterexample;
import liquidjava.utils.VariableFormatter;
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
        super("Refinement Error",
                String.format("%s is not a subtype of %s", VariableFormatter.formatText(found.getValue().toString()),
                        VariableFormatter.formatText(expected.getValue().toString())),
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

        List<String> foundVarNames = new ArrayList<>();
        found.getValue().getVariableNames(foundVarNames);
        String counterexampleString = counterexample.assignments().stream()
                // only include variables that appear in the found value
                .filter(a -> foundVarNames.contains(a.first()))
                // format as "var == value"
                .map(a -> VariableFormatter.formatVariable(a.first()) + " == " + a.second())
                // join with "&&"
                .collect(Collectors.joining(" && "));

        if (counterexampleString.isEmpty() || counterexampleString.equals(found.getValue().toString()))
            return null;

        return counterexampleString;
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
