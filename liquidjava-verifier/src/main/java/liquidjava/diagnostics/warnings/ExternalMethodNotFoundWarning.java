package liquidjava.diagnostics.warnings;

import spoon.reflect.cu.SourcePosition;

/**
 * Warning indicating that a method referenced in an external refinement was not found
 * 
 * @see LJWarning
 */
public class ExternalMethodNotFoundWarning extends LJWarning {

    private final String methodName;
    private final String className;
    private final String[] overloads;

    public ExternalMethodNotFoundWarning(SourcePosition position, String message, String methodName, String className,
            String[] overloads) {
        super(message, position);
        this.methodName = methodName;
        this.className = className;
        this.overloads = overloads;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getClassName() {
        return className;
    }

    public String[] getOverloads() {
        return overloads;
    }

    @Override
    public String getDetails() {
        return overloads.length > 0 ? String.format("Available overloads:\n  %s", String.join("\n  ", overloads)) : "";
    }
}
