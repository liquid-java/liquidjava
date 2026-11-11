package liquidjava.api.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import liquidjava.api.CommandLineLauncher;
import liquidjava.diagnostics.AliasExpansion;
import liquidjava.diagnostics.Diagnostics;
import liquidjava.diagnostics.errors.LJError;
import liquidjava.diagnostics.errors.RefinementError;

class TestAliasExpansions {

    private static final String TEST_SUITE = "../liquidjava-example/src/main/java/testSuite/";

    @Test
    void foundAndExpectedRecordTheirSubstitutedAliases() {
        RefinementError error = verify("ErrorAlias.java");
        assertExpansion(error.getFoundAliases(), "InRange(j, 10, 16)", "10 < j && j < 16");
        assertExpansion(error.getExpectedAliases(), "InRange(j, 10, 15)", "10 < j && j < 15");
        AliasExpansion expected = error.getExpectedAliases().get(0);
        assertEquals("InRange", expected.alias());
        assertEquals(List.of("val", "low", "up"), expected.parameters());
        assertEquals(7, expected.declarationPosition().getLine());
    }

    @Test
    void messageDeclaresEachAliasOnceAsWritten() {
        RefinementError error = verify("ErrorStaticFinalInAlias.java");
        assertEquals(List.of(
                "where `Mode(x) = x == ImageWriteParam.MODE_DISABLED || x == ImageWriteParam.MODE_DEFAULT || x == ImageWriteParam.MODE_EXPLICIT || x == ImageWriteParam.MODE_COPY_FROM_METADATA`"),
                error.getMessageNotes());
        assertTrue(error.toString().contains(error.getMessage() + "\n  where `Mode(x) = "));
    }

    private static void assertExpansion(List<AliasExpansion> expansions, String invocation, String expansion) {
        assertEquals(1, expansions.size());
        assertEquals(invocation, expansions.get(0).invocation().replaceAll("[⁰¹²³⁴⁵⁶⁷⁸⁹]", ""));
        assertEquals(expansion, expansions.get(0).expansion().replaceAll("[⁰¹²³⁴⁵⁶⁷⁸⁹]", ""));
    }

    private static RefinementError verify(String test) {
        CommandLineLauncher.launch(TEST_SUITE + test);
        List<LJError> errors = Diagnostics.getInstance().getErrors().stream().toList();
        assertEquals(1, errors.size(), "Expected exactly one error from " + test);
        return assertInstanceOf(RefinementError.class, errors.get(0));
    }
}
