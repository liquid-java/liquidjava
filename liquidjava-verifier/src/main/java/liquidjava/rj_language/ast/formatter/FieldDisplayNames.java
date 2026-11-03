package liquidjava.rj_language.ast.formatter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What a verification learns about field names that their internal form does not say, so that {@link VariableFormatter}
 * can write them as in code: which fields are static, and which field instances were created by a write through another
 * object. Filled during a verification and cleared when the next one starts
 */
public final class FieldDisplayNames {
    private static final Set<String> STATIC_FIELDS = ConcurrentHashMap.newKeySet();
    private static final Map<String, String> WRITE_RECEIVERS = new ConcurrentHashMap<>();

    private FieldDisplayNames() {
    }

    /**
     * Records that a field name ({@code this#<declaring type>.<field>}) belongs to a static field, written
     * {@code Class.field}
     */
    public static void recordStaticField(String name) {
        STATIC_FIELDS.add(name);
    }

    /**
     * Records that a field instance ({@code #this#<declaring type>.<field>_<n>}) was created by a write through
     * {@code receiver} ({@code receiver.field = v}), written {@code receiver.field}; an empty receiver writes the bare
     * field name. Only the display changes
     */
    public static void recordWriteReceiver(String instanceName, String receiver) {
        WRITE_RECEIVERS.put(instanceName, receiver);
    }

    /**
     * Forgets what the previous verification recorded
     */
    public static void clear() {
        STATIC_FIELDS.clear();
        WRITE_RECEIVERS.clear();
    }

    static boolean isStaticField(String name) {
        return STATIC_FIELDS.contains(name);
    }

    static String writeReceiver(String instanceName) {
        return WRITE_RECEIVERS.get(instanceName);
    }
}
