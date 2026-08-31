package liquidjava.diagnostics;

import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;

import liquidjava.utils.Utils;

/**
 * Finds the closest matching name among the elements available in the current context using Jaro-Winkler similarity
 */
public final class NameSuggester {

    private static final int MINIMUM_NAME_LENGTH = 3;
    private static final double MINIMUM_SIMILARITY = 0.9;
    private static final JaroWinklerSimilarity SIMILARITY = new JaroWinklerSimilarity();

    private NameSuggester() {
    }

    public static Optional<String> findClosest(String name, Collection<String> candidates) {
        if (name == null || candidates == null || candidates.isEmpty())
            return Optional.empty();

        String sourceName = getSourceName(name);
        if (sourceName.length() < MINIMUM_NAME_LENGTH)
            return Optional.empty(); // do not provide suggestions for very short names

        String normalizedSourceName = sourceName.toLowerCase(Locale.ROOT);
        return candidates.stream().filter(candidate -> candidate != null).map(NameSuggester::getSourceName).distinct()
                .filter(candidate -> candidate.length() >= MINIMUM_NAME_LENGTH && !candidate.equals(sourceName))
                .map(candidate -> new Match(candidate,
                        SIMILARITY.apply(normalizedSourceName, candidate.toLowerCase(Locale.ROOT))))
                .filter(match -> match.similarity() >= MINIMUM_SIMILARITY)
                .max(Comparator.comparingDouble(Match::similarity).thenComparing(Match::name,
                        String.CASE_INSENSITIVE_ORDER.reversed()))
                .map(Match::name);
    }

    private static String getSourceName(String name) {
        String simpleName = Utils.getSimpleName(name);
        if (simpleName.startsWith("this#"))
            return simpleName.substring("this#".length());
        if (simpleName.startsWith("#"))
            return simpleName.substring(1).replaceFirst("_\\d+$", "");
        return simpleName;
    }

    private record Match(String name, double similarity) {
    }
}
