package liquidjava.rj_language.ast.formatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class DisplayNamesTest {

    @Test
    void tagsEarlierValuesWithTheirLineAndKeepsTheLatestShownPlain() {
        Map<String, Integer> lines = Map.of("#s_3", 12, "#s_4", 13, "#s_5", 13, "#s_6", 14);
        DisplayNames names = DisplayNames.of(List.of("#s_4", "#s_5"), List.of("#s_3", "#s_6"), lines::get);
        assertEquals("s", names.get("#s_5"));
        assertEquals("s@13", names.get("#s_4"));
        assertEquals("s@12", names.get("#s_3"));
        assertEquals("s@14", names.get("#s_6"));
        assertEquals("size(s@13)", names.get("size(#s_4)"));
    }

    @Test
    void numbersValuesFromOneLineOrNoneAndKeepsTheDeclaredVariablePlain() {
        DisplayNames names = DisplayNames.of(List.of("n", "#n_1", "#n_2", "#m_1", "#m_2"), List.of(),
                name -> name.startsWith("#n") ? 11 : null);
        assertEquals("n", names.get("n"));
        assertEquals("n@11#1", names.get("#n_1"));
        assertEquals("n@11#2", names.get("#n_2"));
        assertEquals("m", names.get("#m_2"));
        assertEquals("m#1", names.get("#m_1"));
    }
}
