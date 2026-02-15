package liquidjava.diagnostics.warnings;

import liquidjava.diagnostics.Colors;
import liquidjava.diagnostics.LJDiagnostic;
import spoon.reflect.cu.SourcePosition;

/**
 * Base class for all LiquidJava warnings
 */
public abstract class LJWarning extends LJDiagnostic {

    public LJWarning(String message, SourcePosition pos) {
        super("Warning", message, pos, Colors.BOLD_YELLOW, null);
    }
}
