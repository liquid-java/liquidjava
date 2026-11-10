package liquidjava.api.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import liquidjava.api.CommandLineLauncher;
import liquidjava.diagnostics.Diagnostics;
import liquidjava.diagnostics.errors.LJError;
import liquidjava.diagnostics.errors.RefinementError;

/** Checks that a static final constant the code passes is shown by its name, as the spec writes it */
class TestStaticConstantNames {

    private static final Map<Integer, RefinementError> ERRORS = new TreeMap<>();

    @BeforeAll
    static void verify() {
        CommandLineLauncher.launch("../liquidjava-example/src/main/java/testSuite/ErrorStaticFinalConstantName.java");
        for (LJError error : Diagnostics.getInstance().getErrors())
            ERRORS.put(error.getPosition().getLine(), assertInstanceOf(RefinementError.class, error));
    }

    @Test
    void constantPassedDirectlyKeepsItsName() {
        assertShowsName(ERRORS.get(14));
    }

    @Test
    void constantPassedThroughLocalKeepsItsName() {
        assertShowsName(ERRORS.get(19));
    }

    private static void assertShowsName(RefinementError error) {
        assertEquals(
                "found `mode == ImageWriteParam.MODE_EXPLICIT` but expected `mode != ImageWriteParam.MODE_EXPLICIT`",
                error.getMessage().replaceAll("[⁰¹²³⁴⁵⁶⁷⁸⁹]", ""));
        assertTrue(error.getCounterexample().isEmpty());
    }
}
