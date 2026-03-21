package liquidjava.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VariableFormatter {
    private static final Pattern INSTACE_VAR_PATTERN = Pattern.compile("^#(.+)_([0-9]+)$");
    private static final Pattern INSTANCE_VAR_TEXT_PATTERN = Pattern.compile("#[^\\s,;:()\\[\\]{}]+_[0-9]+");
    private static final char[] SUPERSCRIPT_CHARS = { '⁰', '¹', '²', '³', '⁴', '⁵', '⁶', '⁷', '⁸', '⁹' };

    public static String formatVariable(String name) {
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

    public static String formatText(String text) {
        if (text == null)
            return null;

        Matcher textMatcher = INSTANCE_VAR_TEXT_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (textMatcher.find()) {
            String token = textMatcher.group();
            textMatcher.appendReplacement(sb, Matcher.quoteReplacement(formatVariable(token)));
        }
        textMatcher.appendTail(sb);
        return sb.toString();
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
