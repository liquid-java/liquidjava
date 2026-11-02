package liquidjava.diagnostics.errors;

import static liquidjava.utils.VCTestUtils.vc;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import liquidjava.diagnostics.TranslationTable;
import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.parsing.RefinementsParser;
import liquidjava.smt.Counterexample;
import liquidjava.utils.Pair;

class RefinementErrorCounterexampleTest {

    @Test
    void keepsExpectedVariableSimplifiedOutOfFound() {
        RefinementError error = error("timeout >= 0", vc("∀port:int. port > 0", "∀timeout:int. true", "true"),
                "timeout=-1", "port=1");
        assertEquals(List.of(pair("timeout", "-1")), error.getCounterexample().assignments());
        assertEquals("Counterexample: timeout == -1", error.getCounterexampleStr());
    }

    @Test
    void omitsFoundConjunctsUnlinkedToExpected() {
        RefinementError error = error("x > y",
                vc("∀a:int. a > 0", "∀x:int. x == a", "∀b:int. b < 0", "∀y:int. true", "true"), "a=1", "b=-1", "x=1",
                "y=5");
        assertEquals(List.of(pair("y", "5"), pair("a", "1"), pair("x", "1")), error.getCounterexample().assignments());
    }

    @Test
    void keepsFoundConjunctsLinkedThroughAnEqualityChain() {
        RefinementError error = error("c >= 0", vc("∀a:int. a < 0", "∀b:int. b == a", "∀c:int. c == b", "true"), "c=-1",
                "b=-1", "a=-1");
        assertEquals(List.of(pair("a", "-1"), pair("c", "-1")), error.getCounterexample().assignments());
    }

    @Test
    void omitsUnmentionedPathBinderAndListsGeneratedNamesLast() {
        RefinementError error = error("t >= 0", vc("∀#fresh_1:int. true", "∀port:int. true",
                "∀#fresh_2:boolean. port > 0", "∀t:int. t == #fresh_1 + port", "true"), "#fresh_2=false", "t=-1",
                "#fresh_1=-2", "port=1");
        assertEquals(List.of(pair("port", "1"), pair("t", "-1"), pair("#fresh_1", "-2")),
                error.getCounterexample().assignments());
    }

    @Test
    void omitsValuesFoundAlreadyStates() {
        RefinementError error = error("capacity > size", vc("∀size:int. size == 10", "∀capacity:int. true", "true"),
                "size=10", "capacity=0");
        assertEquals(List.of(pair("capacity", "0")), error.getCounterexample().assignments());
    }

    @Test
    void keepsGhostApplicationsOfExpectedAndFormatsTheirArguments() {
        RefinementError error = error("size(#s_3) > 0", vc("∀port:int. port > 0", "true"), "port=1", "size(#s_3)=0",
                "length(#a_1)=4");
        assertEquals(List.of(pair("size(#s_3)", "0")), error.getCounterexample().assignments());
        assertEquals("Counterexample: size(s³) == 0", error.getCounterexampleStr());
    }

    @Test
    void hasNoCounterexampleWhenOnlyUnrelatedValuesRemain() {
        RefinementError error = error("x > 1", vc("∀x:int. x == 1", "∀port:int. port > 0", "true"), "x=1", "port=1");
        assertTrue(error.getCounterexample().isEmpty());
        assertNull(error.getCounterexampleStr());
    }

    private static RefinementError error(String expected, VCImplication found, String... assignments) {
        Counterexample counterexample = new Counterexample(
                Arrays.stream(assignments).map(a -> a.split("=")).map(parts -> pair(parts[0], parts[1])).toList());
        return new RefinementError(null, null, new Predicate(RefinementsParser.createAST(expected, "")),
                found.simplify(), new TranslationTable(), counterexample, null);
    }

    private static Pair<String, String> pair(String name, String value) {
        return new Pair<>(name, value);
    }
}
