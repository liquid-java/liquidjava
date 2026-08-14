package liquidjava.diagnostics.warnings;

import spoon.reflect.cu.SourcePosition;

/**
 * Warning indicating that a method referenced in an external refinement was not found
 * 
 * @see LJWarning
 */
public class ExternalMethodNotFoundWarning extends LJWarning {

    private final String signature;
    private final String className;
    private final String[] overloads;

    public ExternalMethodNotFoundWarning(SourcePosition position, String message, String signature, String className,
            String[] overloads) {
        super(message, position);
        this.signature = signature;
        this.className = className;
        this.overloads = overloads;
    }

    public String getSignature() {
        return signature;
    }

    public String getClassName() {
        return className;
    }

    public String[] getOverloads() {
        return overloads;
    }

    @Override
    public String getHint() {
        if (overloads.length == 0)
            return null;
        return String.format("Available overloads:\n  %s", String.join("\n  ", overloads));
    }
}
