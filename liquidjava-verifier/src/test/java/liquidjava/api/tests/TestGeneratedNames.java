package liquidjava.api.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import liquidjava.api.CommandLineLauncher;
import liquidjava.diagnostics.Diagnostics;
import liquidjava.diagnostics.errors.LJError;

/**
 * Checks that the messages for {@code classes/generated_names_error} name each value the verifier introduces after what
 * it is, without the instance counters that depend on verification order
 */
class TestGeneratedNames {

    private static final Map<Integer, String> MESSAGES = new TreeMap<>();

    @BeforeAll
    static void verify() {
        CommandLineLauncher.launch("../liquidjava-example/src/main/java/testSuite/classes/generated_names_error");
        for (LJError error : Diagnostics.getInstance().getErrors())
            MESSAGES.put(error.getPosition().getLine(), plain(error.getMessage()));
    }

    @Test
    void ifConditionIsNamedAfterTheIf() {
        assertEquals("#if_condition == ready && #if_condition is not a subtype of a > 0", MESSAGES.get(25));
    }

    @Test
    void loopConditionIsNamedAfterTheLoop() {
        assertEquals("#loop_condition == ready && #loop_condition == true is not a subtype of a > 0", MESSAGES.get(30));
    }

    @Test
    void ternaryConditionIsNamedAfterTheTernary() {
        assertEquals("#ternary_condition == ready && #ternary_condition == true is not a subtype of a < 0",
                MESSAGES.get(34));
    }

    @Test
    void tryAndCatchPathsAreNamedAfterTheirBranches() {
        assertEquals("!#try_branch && !#catch_branch is not a subtype of a > 0", MESSAGES.get(43));
    }

    @Test
    void callResultIsNamedAfterTheMethod() {
        assertEquals("x == #read_result - 1 is not a subtype of x > 0", MESSAGES.get(48));
    }

    @Test
    void untranslatedOperationIsNamedAsAnOperationResult() {
        assertEquals("#if_condition == #operation_result && #if_condition && x == 0 is not a subtype of x > 0",
                MESSAGES.get(54));
    }

    @Test
    void newReceiverIsNamedAfterItsClass() {
        assertEquals("found closed(#new_Door) but expected opened(#new_Door)", MESSAGES.get(58));
    }

    private static String plain(String text) {
        return text.replaceAll("[⁰¹²³⁴⁵⁶⁷⁸⁹]", "");
    }
}
