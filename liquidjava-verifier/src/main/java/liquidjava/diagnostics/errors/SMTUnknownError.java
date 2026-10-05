package liquidjava.diagnostics.errors;

/**
 * Error indicating that the SMT solver could not decide a verification condition
 */
public class SMTUnknownError extends LJError {

    public SMTUnknownError(String reason) {
        super("SMT Unknown Error",
                "The SMT solver returned UNKNOWN and could not verify the refinement. Reason: " + reason, null, null);
    }
}
