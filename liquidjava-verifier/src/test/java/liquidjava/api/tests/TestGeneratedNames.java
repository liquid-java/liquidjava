package liquidjava.api.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import liquidjava.api.CommandLineLauncher;
import liquidjava.diagnostics.Diagnostics;

/**
 * Checks that the messages for {@code classes/generated_names_error} name each value the verifier introduces after what
 * it is, without the instance counters that depend on verification order
 */
class TestGeneratedNames {

    private static Map<Integer, String> messages;

    @BeforeAll
    static void verify() {
        CommandLineLauncher.launch("../liquidjava-example/src/main/java/testSuite/classes/generated_names_error");
        // a second error on a line fails here instead of replacing the first
        messages = Diagnostics.getInstance().getErrors().stream().collect(
                Collectors.toMap(error -> error.getPosition().getLine(), error -> plain(error.getMessage()), (a, b) -> {
                    throw new AssertionError("two errors on one line: " + a + " and " + b);
                }, TreeMap::new));
    }

    @Test
    void reportsOneErrorPerOrigin() {
        assertEquals(List.of(25, 30, 34, 43, 48, 54, 58), List.copyOf(messages.keySet()));
    }

    @Test
    void ifConditionIsNamedAfterTheIf() {
        assertEquals("#if_condition == ready && #if_condition is not a subtype of a > 0", messages.get(25));
    }

    @Test
    void loopConditionIsNamedAfterTheLoop() {
        assertEquals("#loop_condition == ready && #loop_condition == true is not a subtype of a > 0", messages.get(30));
    }

    @Test
    void ternaryConditionIsNamedAfterTheTernary() {
        assertEquals("#ternary_condition == ready && #ternary_condition == true is not a subtype of a < 0",
                messages.get(34));
    }

    @Test
    void tryAndCatchPathsAreNamedAfterTheirBranches() {
        assertEquals("!#try_branch && !#catch_branch is not a subtype of a > 0", messages.get(43));
    }

    @Test
    void callResultIsNamedAfterTheMethod() {
        assertEquals("x == #read_result - 1 is not a subtype of x > 0", messages.get(48));
    }

    @Test
    void untranslatedOperationIsNamedAsAnOperationResult() {
        assertEquals("#if_condition == #operation_result && #if_condition && x == 0 is not a subtype of x > 0",
                messages.get(54));
    }

    @Test
    void newReceiverIsNamedAfterItsClass() {
        assertEquals("found closed(#new_Door) but expected opened(#new_Door)", messages.get(58));
    }

    private static String plain(String text) {
        return text.replaceAll("[⁰¹²³⁴⁵⁶⁷⁸⁹]", "");
    }
}
