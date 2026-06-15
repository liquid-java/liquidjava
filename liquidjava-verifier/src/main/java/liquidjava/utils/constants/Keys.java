package liquidjava.utils.constants;

public final class Keys {
    public static final String REFINEMENT = "refinement";
    public static final String REFINEMENT_SAT_CHECK = "refinement_sat_check";
    public static final String TARGET = "target";
    public static final String RETURN_VAR_NAME = "return_var_name";
    public static final String THIS = "this";
    public static final String WILDCARD = "_";
    public static final String OLD = "old";
    public static final String VARIABLE = "Variable";
    public static final String GHOST = "Ghost";
    public static final String ALIAS = "Alias";
    /**
     * Reserved builtin function name modeling a Java floating-point-to-integral narrowing cast (e.g. {@code (int) d}),
     * which truncates its single argument toward zero (JLS §5.1.3). Translated directly by the SMT backend (see
     * {@link liquidjava.smt.TranslatorToZ3#makeFunctionInvocation}); it is not a user-visible ghost, so it never
     * participates in ghost overload resolution.
     */
    public static final String TRUNCATE = "$truncateToZero";
}