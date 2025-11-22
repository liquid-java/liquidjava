package liquidjava.diagnostics.errors;

import liquidjava.diagnostics.TranslationTable;
import spoon.reflect.cu.SourcePosition;

/**
 * Error indicating that the arguments provided to a function or method do not match the expected parameters either in
 * number or type of arguments
 * 
 * @see LJError
 */
public class ArgumentMismatchError extends LJError {

    public ArgumentMismatchError(String message) {
        super("Argument Mismatch Error", message, null, null);
    }

    public ArgumentMismatchError(String message, SourcePosition position, TranslationTable translationTable) {
        super("Argument Mismatch Error", message, position, translationTable);
    }
}
