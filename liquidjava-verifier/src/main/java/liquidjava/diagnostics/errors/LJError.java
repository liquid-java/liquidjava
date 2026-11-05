package liquidjava.diagnostics.errors;

import liquidjava.diagnostics.LJDiagnostic;
import liquidjava.diagnostics.TranslationTable;
import liquidjava.diagnostics.Colors;
import liquidjava.rj_language.ast.formatter.DisplayNames;
import spoon.reflect.cu.SourcePosition;

/**
 * Base class for all LiquidJava errors
 */
public abstract class LJError extends LJDiagnostic {

    private final TranslationTable translationTable;
    private final DisplayNames displayNames;

    public LJError(String title, String message, SourcePosition pos, TranslationTable translationTable) {
        this(title, message, pos, translationTable, null);
    }

    public LJError(String title, String message, SourcePosition pos, TranslationTable translationTable,
            String customMessage) {
        this(title, new ErrorMessage(message, null), pos, translationTable, customMessage);
    }

    LJError(String title, ErrorMessage message, SourcePosition pos, TranslationTable translationTable,
            String customMessage) {
        super(title, message.text(), pos, Colors.BOLD_RED, customMessage);
        this.translationTable = translationTable != null ? translationTable : new TranslationTable();
        this.displayNames = message.names();
    }

    public TranslationTable getTranslationTable() {
        return translationTable;
    }

    /** The names the error gives its variables, for every view of it to show them alike, or null when it has none */
    public DisplayNames getDisplayNames() {
        return displayNames;
    }
}
