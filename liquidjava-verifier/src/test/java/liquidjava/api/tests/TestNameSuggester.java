package liquidjava.api.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import liquidjava.diagnostics.NameSuggester;

class TestNameSuggester {

    @Test
    void findsClosestName() {
        assertEquals("amount", NameSuggester.findClosest("ammount", List.of("total", "amount")).orElseThrow());
    }

    @Test
    void recognizesTransposedCharacters() {
        assertEquals("length", NameSuggester.findClosest("lenght", List.of("length")).orElseThrow());
    }

    @Test
    void normalizesQualifiedAndGeneratedNames() {
        assertEquals("length", NameSuggester.findClosest("Example.lenght", List.of("Example.length")).orElseThrow());
        assertEquals("amount", NameSuggester.findClosest("ammount", List.of("#amount_12")).orElseThrow());
        assertEquals("value", NameSuggester.findClosest("valuee", List.of("this#value")).orElseThrow());
    }

    @Test
    void doesNotSuggestUnrelatedOrVeryShortNames() {
        assertTrue(NameSuggester.findClosest("counter", List.of("result", "value")).isEmpty());
        assertTrue(NameSuggester.findClosest("x", List.of("y")).isEmpty());
    }

    @Test
    void handlesMissingAndEmptyInputs() {
        assertTrue(NameSuggester.findClosest(null, List.of("value")).isEmpty());
        assertTrue(NameSuggester.findClosest("value", null).isEmpty());
        assertTrue(NameSuggester.findClosest("value", List.of()).isEmpty());
        assertTrue(NameSuggester.findClosest("", List.of("value")).isEmpty());
    }

    @Test
    void ignoresNullBlankAndExactCandidates() {
        assertEquals("amount", NameSuggester.findClosest("ammount", Arrays.asList(null, "", "amount")).orElseThrow());
        assertTrue(NameSuggester.findClosest("amount", List.of("amount")).isEmpty());
        assertTrue(NameSuggester.findClosest("amount", List.of("Example.amount", "#amount_1")).isEmpty());
    }

    @Test
    void preservesCandidateCapitalization() {
        assertEquals("Amount", NameSuggester.findClosest("amount", List.of("Amount")).orElseThrow());
    }

    @Test
    void supportsNamesAtMinimumLength() {
        assertEquals("size", NameSuggester.findClosest("siz", List.of("size")).orElseThrow());
    }

    @Test
    void rejectsNamesJustBelowSimilarityThreshold() {
        assertTrue(NameSuggester.findClosest("abc", List.of("abd")).isEmpty());
    }

    @Test
    void prefersTheMostSimilarCandidate() {
        assertEquals("availableValue",
                NameSuggester.findClosest("availableValu", List.of("availableValues", "availableValue")).orElseThrow());
    }

    @Test
    void breaksEquivalentMatchesAlphabetically() {
        assertEquals("foobart", NameSuggester.findClosest("foobaru", List.of("foobarv", "foobart")).orElseThrow());
    }
}
