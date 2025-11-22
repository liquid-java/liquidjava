package liquidjava.diagnostics;

import java.util.LinkedHashSet;
import liquidjava.diagnostics.errors.LJError;
import liquidjava.diagnostics.warnings.LJWarning;

/**
 * Singleton class to store diagnostics (errors and warnings) during the verification process
 * 
 * @see LJError
 * @see LJWarning
 */
public class Diagnostics {
    private static final Diagnostics instance = new Diagnostics();

    private final LinkedHashSet<LJError> errors;
    private final LinkedHashSet<LJWarning> warnings;

    private Diagnostics() {
        this.errors = new LinkedHashSet<>();
        this.warnings = new LinkedHashSet<>();
    }

    public static Diagnostics getInstance() {
        return instance;
    }

    public void add(LJError error) {
        this.errors.add(error);
    }

    public void add(LJWarning warning) {
        this.warnings.add(warning);
    }

    public boolean foundError() {
        return !this.errors.isEmpty();
    }

    public boolean foundWarning() {
        return !this.warnings.isEmpty();
    }

    public LinkedHashSet<LJError> getErrors() {
        return this.errors;
    }

    public LinkedHashSet<LJWarning> getWarnings() {
        return this.warnings;
    }

    public void clear() {
        this.errors.clear();
        this.warnings.clear();
    }

    public String getErrorOutput() {
        return String.join("\n", errors.stream().map(LJError::toString).toList());
    }

    public String getWarningOutput() {
        return String.join("\n", warnings.stream().map(LJWarning::toString).toList());
    }
}
