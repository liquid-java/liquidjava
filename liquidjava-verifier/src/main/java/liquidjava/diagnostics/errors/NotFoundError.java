package liquidjava.diagnostics.errors;

import java.util.Collection;
import java.util.Locale;

import liquidjava.diagnostics.NameSuggester;
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
    private final Kind kind;

    public NotFoundError(String name, Kind kind, Collection<String> availableElements) {
        this(null, name, kind, null, availableElements);
    }

    public NotFoundError(SourcePosition position, String name, Kind kind, Collection<String> availableElements) {
        this(position, name, kind, null, availableElements);
    }

    public NotFoundError(SourcePosition position, String name, Kind kind, TranslationTable translationTable,
            Collection<String> availableElements) {
        super("Not Found Error", String.format("%s '%s' could not be found", kind, name), position, translationTable);
        this.name = Utils.getSimpleName(name);
        this.kind = kind;
        NameSuggester.findClosest(name, availableElements)
                .ifPresent(match -> setHint(String.format("Did you mean '%s'?", match)));
    }

    public String getName() {
        return name;
    }

    public Kind getKind() {
        return kind;
    }

    public enum Kind {
        VARIABLE, GHOST, ALIAS, CONSTANT;

        @Override
        public String toString() {
            String name = name().toLowerCase(Locale.ROOT);
            return Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
    }
}
