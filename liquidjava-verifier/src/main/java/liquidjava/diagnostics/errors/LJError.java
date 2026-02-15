package liquidjava.diagnostics.errors;

import liquidjava.diagnostics.LJDiagnostic;
import liquidjava.diagnostics.TranslationTable;
import liquidjava.diagnostics.Colors;
import spoon.reflect.cu.SourcePosition;

/**
 * Base class for all LiquidJava errors
 */
public abstract class LJError extends LJDiagnostic {

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
}
