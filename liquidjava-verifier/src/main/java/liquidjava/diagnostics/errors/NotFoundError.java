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
    private final String kind; // "Variable" or "Ghost"

    public NotFoundError(String message, String name, String kind) {
        this(message, null, name, kind, null);
    }

    public NotFoundError(String message, SourcePosition position, String name, String kind,
            TranslationTable translationTable) {
        super("Not Found Error", message, position, translationTable);
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
