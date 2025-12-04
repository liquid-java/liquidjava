package liquidjava.diagnostics.warnings;

import spoon.reflect.cu.SourcePosition;

/**
 * Warning indicating that a class referenced in an external refinement was not found
 * 
 * @see LJWarning
 */
public class ExternalClassNotFoundWarning extends LJWarning {

    private final String className;

    public ExternalClassNotFoundWarning(SourcePosition position, String message, String className) {
        super(message, position);
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
