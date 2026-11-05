package liquidjava.rj_language.ast.formatter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The code of the call arguments that a verification names after the callee's parameter, so that
 * {@link VariableFormatter} can write such an argument as the caller wrote it ({@code items.size()} rather than
 * {@code bound}). Filled during a verification and cleared when the next one starts
 */
public final class ArgumentDisplayNames {
    private static final Map<String, String> ARGUMENTS = new ConcurrentHashMap<>();

    private ArgumentDisplayNames() {
    }

    /**
     * Records that an argument instance ({@code #<parameter>_<n>}) stands for the argument {@code code}. Only the
     * display changes
     */
    public static void recordArgument(String instanceName, String code) {
        ARGUMENTS.put(instanceName, code);
    }

    /**
     * Forgets what the previous verification recorded
     */
    public static void clear() {
        ARGUMENTS.clear();
    }

    static String argumentCode(String instanceName) {
        return ARGUMENTS.get(instanceName);
    }
}
