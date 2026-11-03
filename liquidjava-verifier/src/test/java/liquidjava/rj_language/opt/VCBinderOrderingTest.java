package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class VCBinderOrderingTest {

    @Test
    void movesTrueBinderBeforeItsFirstUse() {
        VCSimplificationResult result = assertSimplificationSteps(new VCBinderOrdering(),
                vc("∀y:int. y < i", "∀i:int. true"), step("true", "y < i"));
        assertEquals(List.of("i", "y"), result.getBinders());
    }
}
