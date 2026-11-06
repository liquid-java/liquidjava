package liquidjava.diagnostics.errors;

import static liquidjava.rj_language.opt.VCSimplificationUtils.linkedToExpected;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import liquidjava.diagnostics.TranslationTable;
import liquidjava.processor.context.PlacementInCode;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.formatter.DisplayNames;
import liquidjava.rj_language.ast.formatter.ExpressionFormatter;
import liquidjava.rj_language.opt.VCSimplificationResult;

/** The message of an error that found does not prove expected, and the names it gives the variables of both */
record ErrorMessage(String text, DisplayNames names) {

    /**
     * Writes the facts of found about the values expected constrains and expected in frame, or only expected when
     * showsFound rejects those facts; path conditions about other values stay in the derivation
     */
    static ErrorMessage of(Expression expected, VCSimplificationResult found, TranslationTable translationTable,
            Predicate<Expression> showsFound, String frame) {
        Expression shown = linkedToExpected(found.getImplication().toPredicate().getExpression(), expected);
        // the names of the message first, then those only the original found has, each with the line of its value
        List<String> message = new ArrayList<>();
        shown.getVariableNames(message);
        expected.getVariableNames(message);
        VCSimplificationResult original = found.getOriginal();
        List<String> others = original.getBinders();
        original.getImplication().toPredicate().getExpression().getVariableNames(others);
        DisplayNames names = DisplayNames.of(message, others, name -> {
            PlacementInCode placement = translationTable == null ? null : translationTable.get(name);
            return placement == null ? null : placement.getLine();
        });
        String expectedText = ExpressionFormatter.format(expected, names);
        String text = showsFound.test(shown)
                ? String.format(frame, ExpressionFormatter.format(shown, names), expectedText)
                : String.format("expected %s but could not prove it", expectedText);
        return new ErrorMessage(text, names);
    }
}
