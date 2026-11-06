package liquidjava.rj_language.ast.formatter;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The names of the variables of one scope, such as an error or a refinement, as they are shown. A name is written as in
 * code; when the scope has several variables written alike, one keeps the plain name and each other one gets the line
 * its value comes from, {@code x@12}, numbered when they share a line or have none, {@code x@12#1} and {@code x@12#2}.
 * The plain name goes to the declared variable, or else to its latest instance, preferring those shown first
 */
public final class DisplayNames {
    private final Map<String, String> tags = new HashMap<>();

    private DisplayNames() {
    }

    /**
     * @param shown
     *            the names shown first, such as those of an error message
     * @param others
     *            the other names of the scope
     * @param lineOf
     *            the line where the value of a variable comes from, or null when unknown
     */
    public static DisplayNames of(Collection<String> shown, Collection<String> others,
            Function<String, Integer> lineOf) {
        DisplayNames names = new DisplayNames();
        Set<String> all = new LinkedHashSet<>(shown);
        all.addAll(others);
        Map<String, List<String>> alike = all.stream().collect(Collectors.groupingBy(VariableFormatter::format));
        for (List<String> variables : alike.values()) {
            if (variables.size() > 1)
                names.tag(variables, shown, lineOf);
        }
        return names;
    }

    /**
     * The display name of a variable, or of a ghost application over variables such as {@code size(#s_2)}
     */
    public String get(String name) {
        return VariableFormatter.format(name, variable -> tags.getOrDefault(variable, ""));
    }

    private void tag(List<String> alike, Collection<String> shown, Function<String, Integer> lineOf) {
        String plain = plainOf(alike, shown);
        Map<String, List<String>> byLine = alike.stream().filter(name -> !name.equals(plain))
                .sorted(Comparator.comparingInt(VariableFormatter::counterOf))
                .collect(Collectors.groupingBy(name -> lineTag(lineOf.apply(name))));
        byLine.forEach((line, variables) -> {
            for (int i = 0; i < variables.size(); i++)
                tags.put(variables.get(i), tagFor(line, i, variables.size()));
        });
    }

    // The declared variable, or else the latest instance, preferring those shown
    private static String plainOf(List<String> alike, Collection<String> shown) {
        List<String> candidates = alike.stream().filter(shown::contains).toList();
        return (candidates.isEmpty() ? alike : candidates).stream()
                .max(Comparator.comparing((String name) -> VariableFormatter.counterOf(name) < 0)
                        .thenComparing(VariableFormatter::counterOf))
                .orElseThrow();
    }

    // The line alone tells apart a single value from it; values that share a line, or have none, are numbered
    private static String tagFor(String line, int index, int count) {
        return count == 1 && !line.isEmpty() ? line : line + "#" + (index + 1);
    }

    private static String lineTag(Integer line) {
        return line == null ? "" : "@" + line;
    }
}
