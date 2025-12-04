package liquidjava.diagnostics.errors;

import liquidjava.diagnostics.TranslationTable;
import liquidjava.utils.Utils;
import spoon.reflect.cu.SourcePosition;

/**
 * Error indicating that an element referenced in a refinement was not found
 * 
 * @see LJError
 */
public class NotFoundError extends LJError {

    private final String name;
    private final String kind; // "Variable" | "Ghost" | "Alias"

    public NotFoundError(String name, String kind) {
        this(null, name, kind, null);
    }

    public NotFoundError(SourcePosition position, String name, String kind, TranslationTable translationTable) {
        super("Not Found Error", String.format("%s '%s' not found", kind, name), position, translationTable);
        this.name = Utils.getSimpleName(name);
        this.kind = kind;
    }

    public String getName() {
        return name;
    }

    public String getKind() {
        return kind;
    }
}
