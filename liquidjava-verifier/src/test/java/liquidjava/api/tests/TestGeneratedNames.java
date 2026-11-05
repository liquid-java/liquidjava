package liquidjava.api.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import liquidjava.api.CommandLineLauncher;
import liquidjava.diagnostics.Diagnostics;
import liquidjava.diagnostics.errors.RefinementError;
import liquidjava.rj_language.opt.VCSimplificationResult;

/**
 * Checks that the messages for {@code classes/generated_names_error} name each value the verifier introduces after what
 * it is, without the instance counters that depend on verification order
 */
class TestGeneratedNames {

    @Test
    void namesEachGeneratedValueAfterWhatItIs() {
        CommandLineLauncher.launch("../liquidjava-example/src/main/java/testSuite/classes/generated_names_error");
        // the original Found of a refinement error, as the message and the simplified Found keep only the facts about
        // the expected values
        Map<Integer, String> messages = Diagnostics.getInstance().getErrors().stream().collect(Collectors.toMap(
                error -> error.getPosition().getLine(),
                error -> (error instanceof RefinementError refinement ? originalFound(refinement) : error.getMessage())
                        .replaceAll("[⁰¹²³⁴⁵⁶⁷⁸⁹]", "")));

        assertEquals(Map.of(25, "#if_condition == #ready_result && #if_condition", 30,
                "#loop_condition == #ready_result && #loop_condition == true", 34,
                "#ternary_condition == #ready_result && #ternary_condition == true", 43,
                "!#try_branch && !#catch_branch", 48, "x == #read_result - 1", 54,
                "isNull == #operation_result && #if_condition == isNull && #if_condition && x == 0", 58,
                "found closed(#new_Door) but expected opened(#new_Door)"), messages);
    }

    private static String originalFound(RefinementError error) {
        VCSimplificationResult found = error.getFound();
        while (found.getOrigin() != null)
            found = found.getOrigin();
        return found.getImplication().toPredicate().getExpression().toDisplayString();
    }
}
