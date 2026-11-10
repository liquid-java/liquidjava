package liquidjava.diagnostics;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Code inside a message, marked with backticks as in {@code found `x > 0`}. Clients that show plain text get the
 * backticks; the terminal shows the code in colour instead, or keeps the backticks when colour is off
 */
public final class Code {

    // a run of backticks, the code, and a run of the same length
    private static final Pattern SPAN = Pattern.compile("(`+)(.+?)(?<!`)\\1(?!`)", Pattern.DOTALL);

    private Code() {
    }

    /**
     * {@code code} marked as code, fenced by more backticks than any run it holds, padded if it starts or ends with one
     */
    public static String of(String code) {
        String fence = "`";
        while (code.contains(fence))
            fence += "`";
        String pad = code.startsWith("`") || code.endsWith("`") ? " " : "";
        return fence + pad + code + pad + fence;
    }

    /** {@code message} with each code span in colour and without its backticks, or as it is when colour is off */
    public static String render(String message) {
        return render(message, Colors.enabled());
    }

    static String render(String message, boolean colour) {
        if (message == null || !colour)
            return message;
        Matcher span = SPAN.matcher(message);
        StringBuilder rendered = new StringBuilder();
        while (span.find()) {
            String code = span.group(2);
            // the padding of() adds around code that starts or ends with a backtick, not spaces of the code
            String inner = code.length() > 2 && code.startsWith(" ") && code.endsWith(" ")
                    ? code.substring(1, code.length() - 1) : code;
            if (inner.startsWith("`") || inner.endsWith("`"))
                code = inner;
            span.appendReplacement(rendered, Matcher.quoteReplacement(Colors.BLUE + code + Colors.RESET));
        }
        span.appendTail(rendered);
        return rendered.toString();
    }
}
