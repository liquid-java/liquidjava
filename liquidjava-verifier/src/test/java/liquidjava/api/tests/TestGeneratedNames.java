package liquidjava.api.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import liquidjava.api.CommandLineLauncher;
import liquidjava.diagnostics.Diagnostics;
import liquidjava.diagnostics.errors.RefinementError;

/**
 * Checks that the messages for {@code classes/generated_names_error} name each value the verifier introduces after what
 * it is, without the instance counters that depend on verification order
 */
class TestGeneratedNames {

    @Test
    void namesEachGeneratedValueAfterWhatItIs() {
        CommandLineLauncher.launch("../liquidjava-example/src/main/java/testSuite/classes/generated_names_error");
        // the whole Found of a refinement error, as the message keeps only the facts about the expected values
        Map<Integer, String> messages = Diagnostics.getInstance().getErrors().stream()
                .collect(Collectors.toMap(error -> error.getPosition().getLine(),
                        error -> (error instanceof RefinementError refinement
                                ? refinement.getFound().getImplication().toPredicate().getExpression().toDisplayString()
                                : error.getMessage()).replaceAll("[⁰¹²³⁴⁵⁶⁷⁸⁹]", "")));

        assertEquals(Map.of(25, "#if_condition == #ready_result && #if_condition", 30,
                "#loop_condition == #ready_result && #loop_condition == true", 34,
                "#ternary_condition == #ready_result && #ternary_condition == true", 43,
                "!#try_branch && !#catch_branch", 48, "x == #read_result - 1", 54,
                "#if_condition == #operation_result && #if_condition && x == 0", 58,
                "found closed(#new_Door) but expected opened(#new_Door)"), messages);
    }
}
