package liquidjava.rj_language.ast.formatter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formats internal variable names into a cleaner display representation using superscript notation
 */
public final class VariableFormatter {
    private static final Pattern INSTACE_VAR_PATTERN = Pattern.compile("^#(.+)_([0-9]+)$");
    private static final char[] SUPERSCRIPT_CHARS = { '⁰', '¹', '²', '³', '⁴', '⁵', '⁶', '⁷', '⁸', '⁹' };

    public static String format(String name) {
        if (name == null)
            return null;

        if (isApplication(name))
            return formatApplication(name);

        Matcher matcher = INSTACE_VAR_PATTERN.matcher(name);
        if (!matcher.matches())
            return name;

        String baseName = matcher.group(1);
        String counter = matcher.group(2);
        String prefix = isSpecialIdentifier(baseName) ? "#" : "";
        return prefix + baseName + toSuperscript(counter);
    }

    public static String withoutInstance(String name) {
        Matcher matcher = INSTACE_VAR_PATTERN.matcher(name);
        return matcher.matches() ? matcher.group(1) : name;
    }

    private static String toSuperscript(String number) {
        StringBuilder sb = new StringBuilder(number.length());
        for (char c : number.toCharArray()) {
            int index = c - '0';
            if (index < 0 || index >= SUPERSCRIPT_CHARS.length)
                return number;
            sb.append(SUPERSCRIPT_CHARS[index]);
        }
        return sb.toString();
    }

    private static boolean isSpecialIdentifier(String id) {
        return id.equals("fresh") || id.equals("ret");
    }

    private static boolean isApplication(String name) {
        int open = name.indexOf('(');
        return open > 0 && name.endsWith(")");
    }

    /**
     * Formats the arguments of a ghost application such as {@code size(#s_2)}, splitting them only at top-level commas
     * so nested applications keep theirs
     */
    private static String formatApplication(String name) {
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
                formatted.append(format(args.substring(start, i).trim()));
                start = i + 1;
            }
        }
        return formatted.append(')').toString();
    }
}
