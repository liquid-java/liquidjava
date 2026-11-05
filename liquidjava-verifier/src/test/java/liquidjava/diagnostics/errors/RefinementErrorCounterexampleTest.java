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
import liquidjava.rj_language.opt.VCSimplificationResult;
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
        assertEquals(List.of(pair("y", "5"), pair("x", "1"), pair("a", "1")), error.getCounterexample().assignments());
    }

    @Test
    void keepsFoundConjunctsLinkedThroughAnEqualityChain() {
        RefinementError error = error("c >= 0", vc("∀a:int. a < 0", "∀b:int. b == a", "∀c:int. c == b", "true"), "c=-1",
                "b=-1", "a=-1");
        assertEquals(List.of(pair("c", "-1"), pair("a", "-1")), error.getCounterexample().assignments());
    }

    @Test
    void omitsUnmentionedPathBinderAndListsGeneratedNamesLast() {
        RefinementError error = error("t >= 0",
                vc("∀##read_result_1:int. true", "∀port:int. true", "∀##if_condition_2:boolean. port > 0",
                        "∀t:int. t == ##read_result_1 + port", "true"),
                "##if_condition_2=false", "t=-1", "##read_result_1=-2", "port=1");
        assertEquals(List.of(pair("t", "-1"), pair("port", "1"), pair("##read_result_1", "-2")),
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
    void omitsThePathConditionWhenFoundStatesEveryLinkedValue() {
        RefinementError error = error("x > 1", vc("∀x:int. x == 1", "∀port:int. port > 0", "true"), "x=1", "port=1");
        assertTrue(error.getCounterexample().isEmpty());
    }

    @Test
    void keepsAPathConditionTheExpectedValueIsComputedFrom() {
        RefinementError error = error("#ret_1 > 10",
                vc("∀port:int. port < 0", "∀#ret_1:int. #ret_1 == port + 1", "true"), "#ret_1=0", "port=-1");
        assertEquals(List.of(pair("#ret_1", "0"), pair("port", "-1")), error.getCounterexample().assignments());
    }

    @Test
    void hasNoCounterexampleWhenFoundStatesEveryValue() {
        RefinementError error = error("x > 1", vc("∀x:int. x == 1", "true"), "x=1");
        assertTrue(error.getCounterexample().isEmpty());
        assertNull(error.getCounterexampleStr());
    }

    @Test
    void splitsNegatedDisjunctionsSoEachGuardLinksOnlyItsOwnVariables() {
        RefinementError error = error("t >= 0",
                vc("∀port:int. true", "∀retries:int. true", "∀t:int. true",
                        "∀##if_condition_1:boolean. !(port < 0 || retries < 0 || t > 100)", "true"),
                "port=0", "retries=0", "t=-1");
        assertEquals(List.of(pair("t", "-1")), error.getCounterexample().assignments());
    }

    @Test
    void splitsNestedConjunctionsAndDoubleNegations() {
        RefinementError error = error("t >= 0",
                vc("∀port:int. true", "∀retries:int. true", "∀backoff:int. true", "∀t:int. true",
                        "∀##if_condition_1:boolean. !(port < 0 || !!(retries > 3 && backoff > 1) || t > 100)", "true"),
                "port=0", "retries=0", "backoff=0", "t=-1");
        assertEquals(List.of(pair("t", "-1")), error.getCounterexample().assignments());
    }

    @Test
    void keepsAGuardThatRelatesExpectedToAnotherVariable() {
        RefinementError error = error("t >= 0",
                vc("∀port:int. true", "∀retries:int. true", "∀t:int. true",
                        "∀##if_condition_1:boolean. !(port < 0 || retries > t)", "true"),
                "port=0", "retries=-2", "t=-1");
        assertEquals(List.of(pair("t", "-1"), pair("retries", "-2")), error.getCounterexample().assignments());
    }

    @Test
    void messageSaysExpectedCouldNotBeProvedWhenFoundOnlyGuardsOtherVariables() {
        RefinementError error = error("timeout >= 0", vc("∀port:int. true", "∀timeout:int. true",
                "∀##if_condition_1:boolean. !(port < 0 || port > 65535)", "true"), "port=0", "timeout=-1");
        assertEquals("expected timeout >= 0 but could not prove it", error.getMessage());
    }

    @Test
    void messageKeepsOnlyTheGuardsLinkedToExpected() {
        RefinementError error = error("t >= 0",
                vc("∀port:int. true", "∀retries:int. true", "∀t:int. true",
                        "∀##if_condition_1:boolean. !(port < 0 || retries > t)", "true"),
                "port=0", "retries=-2", "t=-1");
        assertEquals("!(retries > t) is not a subtype of t >= 0", error.getMessage());
    }

    @Test
    void messageKeepsFoundAsWrittenWhenEveryFactIsLinked() {
        RefinementError error = error("x >= 0", vc("∀x:int. !(x < 0 || x > 5)", "true"), "x=-1");
        assertEquals("!(x < 0 || x > 5) is not a subtype of x >= 0", error.getMessage());
    }

    @Test
    void stateMessageKeepsAStateReachingExpectedThroughAnAlias() {
        StateRefinementError error = new StateRefinementError(null, null,
                new Predicate(RefinementsParser.createAST("full(box)", "")),
                new VCSimplificationResult(
                        vc("∀b1:Object. empty(b1)", "∀x:int. x > 0", "∀box:Object. box == b1", "true")),
                new TranslationTable(), null, List.of(), null, null);
        assertEquals("found empty(b1) && box == b1 but expected full(box)", error.getMessage());
    }

    @Test
    void omitsEnumValuesFoundAlreadyStates() {
        RefinementError error = error("f == Format.PNG && n > 0", vc("∀f:int. f == Format.JPG", "∀n:int. true", "true"),
                "f=JPG", "n=0");
        assertEquals(List.of(pair("n", "0")), error.getCounterexample().assignments());
    }

    @Test
    void omitsGhostValuesWithSeveralArgumentsFoundAlreadyStates() {
        RefinementError error = error("at(#l_1, #i_2) > 0 && #i_2 > 0", vc("∀#i_2:int. at(#l_1, #i_2) == 0", "true"),
                "at(#l_1, #i_2)=0", "#i_2=0");
        assertEquals(List.of(pair("#i_2", "0")), error.getCounterexample().assignments());
        assertEquals("Counterexample: i² == 0", error.getCounterexampleStr());
    }

    @Test
    void keepsGhostValuesWithSeveralArgumentsFoundSaysNothingAbout() {
        RefinementError error = error("at(#l_1, #i_2) > 0", vc("∀#i_2:int. #i_2 > 0", "true"), "at(#l_1, #i_2)=0",
                "#i_2=1");
        assertEquals(List.of(pair("at(#l_1, #i_2)", "0"), pair("#i_2", "1")), error.getCounterexample().assignments());
        assertEquals("Counterexample: at(l¹, i²) == 0 && i² == 1", error.getCounterexampleStr());
    }

    private static RefinementError error(String expected, VCImplication found, String... assignments) {
        Counterexample counterexample = new Counterexample(
                Arrays.stream(assignments).map(a -> a.split("=")).map(parts -> pair(parts[0], parts[1])).toList());
        return new RefinementError(null, null, new Predicate(RefinementsParser.createAST(expected, "")),
                found.simplify(), new TranslationTable(), counterexample, null, List.of());
    }

    private static Pair<String, String> pair(String name, String value) {
        return new Pair<>(name, value);
    }
}
