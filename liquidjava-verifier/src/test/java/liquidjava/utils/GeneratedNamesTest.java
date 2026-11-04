package liquidjava.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GeneratedNamesTest {

    @Test
    void namesTheValueAfterWhatItIs() {
        assertEquals("#if_condition_7", GeneratedNames.of(GeneratedNames.IF_CONDITION, 7));
        assertEquals("#catch_branch_12", GeneratedNames.of(GeneratedNames.CATCH_BRANCH, 12));
    }

    @Test
    void replacesCharactersTheRefinementLanguageDoesNotAccept() {
        assertEquals("#new_Map_Entry_3", GeneratedNames.of("new_Map$Entry", 3));
        assertEquals("#t_ma_result_4", GeneratedNames.of("tæma_result", 4));
    }

    @Test
    void recognizesEveryGeneratedName() {
        for (String description : new String[] { GeneratedNames.IF_CONDITION, GeneratedNames.LOOP_CONDITION,
                GeneratedNames.TERNARY_CONDITION, GeneratedNames.TRY_BRANCH, GeneratedNames.CATCH_BRANCH,
                GeneratedNames.ARRAY_LENGTH, "size_result", "count_value", "operation_result", "new_File" })
            assertTrue(GeneratedNames.isGenerated(GeneratedNames.of(description, 5)), description);
    }

    @Test
    void doesNotTakeProgramVariablesForGeneratedNames() {
        assertFalse(GeneratedNames.isGenerated("#x_3"));
        assertFalse(GeneratedNames.isGenerated("#condition_3"));
        assertFalse(GeneratedNames.isGenerated("#ret_3"));
        assertFalse(GeneratedNames.isGenerated("#this_3"));
        assertFalse(GeneratedNames.isGenerated("if_condition"));
    }
}
