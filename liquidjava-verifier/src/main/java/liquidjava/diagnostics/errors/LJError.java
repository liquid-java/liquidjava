package liquidjava.diagnostics.errors;

import java.util.Formatter;
import java.util.Locale;

import liquidjava.diagnostics.LJDiagnostic;
import liquidjava.diagnostics.TranslationTable;
import liquidjava.diagnostics.Colors;
import liquidjava.processor.context.PlacementInCode;
import spoon.reflect.cu.SourcePosition;

/**
 * Base class for all LiquidJava errors
 */
public abstract class LJError extends LJDiagnostic {

    protected static final String SEPARATOR = "_".repeat(54);
    protected static final String TABLE_SEPARATOR = "-".repeat(130);

    private final TranslationTable translationTable;

    public LJError(String title, String message, SourcePosition pos, TranslationTable translationTable) {
        this(title, message, pos, translationTable, null);
    }

    public LJError(String title, String message, SourcePosition pos, TranslationTable translationTable,
            String customMessage) {
        super(title, message, pos, Colors.BOLD_RED, customMessage);
        this.translationTable = translationTable != null ? translationTable : new TranslationTable();
    }

    public TranslationTable getTranslationTable() {
        return translationTable;
    }

    public String getTitleMessage() {
        return getTitle();
    }

    public String getFullMessage() {
        return getMessage();
    }

    protected String formatTranslationTable() {
        if (translationTable.isEmpty())
            return "";

        StringBuilder sb = new StringBuilder();
        try (Formatter formatter = new Formatter(sb, Locale.US)) {
            formatter.format("%nInstance translation table:%n");
            formatter.format("%s%n", TABLE_SEPARATOR);
            formatter.format("| %-32s | %-60s | %-1s %n", "Variable Name", "Created in", "File");
            formatter.format("%s%n", TABLE_SEPARATOR);
            for (String name : translationTable.keySet()) {
                PlacementInCode placement = translationTable.get(name);
                formatter.format("| %-32s | %-60s | %-1s %n", name, placement.getText(), placement.getSimplePosition());
            }
            formatter.format("%s%n%n", TABLE_SEPARATOR);
        }
        return sb.toString();
    }
}
