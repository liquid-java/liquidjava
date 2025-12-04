package liquidjava.diagnostics.warnings;

import spoon.reflect.cu.SourcePosition;

/**
 * Custom warning with a message
 * 
 * @see LJWarning
 */
public class CustomWarning extends LJWarning {

    public CustomWarning(SourcePosition position, String message) {
        super(message, position);
    }

    public CustomWarning(String message) {
        super(message, null);
    }

}
