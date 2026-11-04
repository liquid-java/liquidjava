package liquidjava.api.tests;

import java.util.ArrayList;
import java.util.List;

import liquidjava.processor.VCImplication;
import liquidjava.rj_language.ast.formatter.VariableFormatter;

/**
 * The rows of Found as the IDE writes them, without the instance counters that depend on verification order
 */
final class FoundRows {

    private FoundRows() {
    }

    // binder name, then its refinement
    static List<String> rows(VCImplication found) {
        List<String> rows = new ArrayList<>();
        for (VCImplication current = found; current != null; current = current.getNext()) {
            String refinement = current.getRefinement().getExpression().toDisplayString();
            rows.add(plain(current.hasBinder() ? "∀" + VariableFormatter.format(current.getName()) + " " + refinement
                    : refinement));
        }
        return rows;
    }

    static String plain(String text) {
        return text.replaceAll("[⁰¹²³⁴⁵⁶⁷⁸⁹]", "");
    }
}
