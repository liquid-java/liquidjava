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

        Matcher matcher = INSTACE_VAR_PATTERN.matcher(name);
        if (!matcher.matches())
            return name;

        String baseName = matcher.group(1);
        String counter = matcher.group(2);
        String prefix = isSpecialIdentifier(baseName) ? "#" : "";
        return prefix + baseName + toSuperscript(counter);
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
}