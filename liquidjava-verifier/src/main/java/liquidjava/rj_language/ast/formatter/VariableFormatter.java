package liquidjava.rj_language.ast.formatter;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formats internal variable names as they are written in code, without the instance counter
 */
public final class VariableFormatter {
    private static final Pattern INSTACE_VAR_PATTERN = Pattern.compile("^#(.+)_([0-9]+)$");
    // this#<declaring type>.<field>
    private static final Pattern FIELD_PATTERN = Pattern.compile("^this#([\\w.$]+)\\.([\\w$]+)$");

    public static String format(String name) {
        return format(name, instance -> "");
    }

    /**
     * Formats a name, appending to each variable instance in it the tag that tells it apart from other instances
     */
    static String format(String name, Function<String, String> tagOf) {
        if (name == null)
            return null;

        if (isApplication(name))
            return formatApplication(name, tagOf);

        Matcher matcher = INSTACE_VAR_PATTERN.matcher(name);
        if (!matcher.matches())
            return formatField(name);

        String baseName = matcher.group(1);
        // the tag tells apart calls written alike, which can return different values
        String tag = tagOf.apply(name);
        String argument = ArgumentDisplayNames.argumentCode(name);
        if (argument != null)
            return argument + tag;
        String receiver = FieldDisplayNames.writeReceiver(name);
        if (receiver != null && isField(baseName))
            return (receiver.isEmpty() ? "" : receiver + ".") + fieldOf(baseName) + tag;
        String prefix = isReturn(baseName) ? "#" : "";
        return prefix + formatField(baseName) + tag;
    }

    /**
     * The instance counter of a name, or -1 for a name that is not an instance
     */
    static int counterOf(String name) {
        Matcher matcher = INSTACE_VAR_PATTERN.matcher(name);
        return matcher.matches() ? Integer.parseInt(matcher.group(2)) : -1;
    }

    public static String withoutInstance(String name) {
        Matcher matcher = INSTACE_VAR_PATTERN.matcher(name);
        return matcher.matches() ? matcher.group(1) : name;
    }

    /**
     * Whether a name is that of a field, {@code this#<declaring type>.<field>}, possibly with an instance counter
     */
    public static boolean isField(String name) {
        return FIELD_PATTERN.matcher(withoutInstance(name)).matches();
    }

    /**
     * Writes a field name as it is written in code: {@code this.field} for an instance field and {@code Class.field}
     * for a static one, leaving any other name unchanged; the internal name keeps the declaring type to tell fields of
     * different classes apart
     */
    private static String formatField(String name) {
        Matcher matcher = FIELD_PATTERN.matcher(name);
        if (!matcher.matches())
            return name;

        if (FieldDisplayNames.isStaticField(name))
            return simpleTypeName(matcher.group(1)) + "." + matcher.group(2);
        return "this." + matcher.group(2);
    }

    private static String fieldOf(String name) {
        Matcher matcher = FIELD_PATTERN.matcher(name);
        return matcher.matches() ? matcher.group(2) : name;
    }

    private static String simpleTypeName(String qualifiedName) {
        int start = Math.max(qualifiedName.lastIndexOf('.'), qualifiedName.lastIndexOf('$'));
        return qualifiedName.substring(start + 1);
    }

    private static boolean isReturn(String id) {
        return id.equals("ret");
    }

    private static boolean isApplication(String name) {
        int open = name.indexOf('(');
        return open > 0 && name.endsWith(")");
    }

    /**
     * Formats the arguments of a ghost application such as {@code size(#s_2)}, splitting them only at top-level commas
     * so nested applications keep theirs
     */
    private static String formatApplication(String name, Function<String, String> tagOf) {
        int open = name.indexOf('(');
        String args = name.substring(open + 1, name.length() - 1);
        StringBuilder formatted = new StringBuilder(name.substring(0, open)).append('(');
        int depth = 0;
        int start = 0;
        for (int i = 0; i <= args.length(); i++) {
            char c = i < args.length() ? args.charAt(i) : ',';
            if (c == '(')
                depth++;
            else if (c == ')')
                depth--;
            else if (c == ',' && depth == 0) {
                if (start > 0)
                    formatted.append(", ");
                formatted.append(format(args.substring(start, i).trim(), tagOf));
                start = i + 1;
            }
        }
        return formatted.append(')').toString();
    }
}
