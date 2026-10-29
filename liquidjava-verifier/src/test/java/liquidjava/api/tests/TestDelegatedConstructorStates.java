package liquidjava.api.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import liquidjava.api.CommandLineLauncher;
import liquidjava.diagnostics.Diagnostics;
import liquidjava.diagnostics.errors.StateRefinementError;

/**
 * The test suite only checks where errors are reported. These check the state found there: a JDK constructor that
 * delegates to a specified one gets that spec's state, not an unknown one.
 */
class TestDelegatedConstructorStates {

    private static final String TEST_SUITE = "../liquidjava-example/src/main/java/testSuite/";
    private static final List<List<String>> WITH_CAUSE = List.of(List.of("withThrowable"));

    @Test
    void delegatingJdkConstructorsGiveTheSpecifiedState() {
        Map<Integer, List<List<String>>> found = foundStatesByLine("classes/throwable_jdk_subclass_error");
        assertEquals(WITH_CAUSE, found.get(10)); // new ClassNotFoundException()
        assertEquals(WITH_CAUSE, found.get(16)); // new RuntimeException(message, cause)
        assertEquals(WITH_CAUSE, found.get(23)); // subclass calling ClassNotFoundException(String)
        assertNotEquals(WITH_CAUSE, found.get(31)); // MissingResourceException: not delegating, default state
    }

    private static Map<Integer, List<List<String>>> foundStatesByLine(String test) {
        CommandLineLauncher.launch(TEST_SUITE + test);
        return Diagnostics.getInstance().getErrors().stream().map(e -> assertInstanceOf(StateRefinementError.class, e))
                .collect(Collectors.toMap(e -> e.getPosition().getLine(), e -> e.getContext().possibleStates()));
    }
}
