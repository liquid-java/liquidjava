package liquidjava.api.tests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.Test;

import liquidjava.api.CommandLineLauncher;
import liquidjava.diagnostics.Diagnostics;

public class TestMultiple {

    @Test
    public void testMultipleErrorDirectory() {
        String path = "../liquidjava-example/src/main/java/testMultiple/errors";
        CommandLineLauncher.launch(path);
        Diagnostics diagnostics = Diagnostics.getInstance();
        assertEquals(9, diagnostics.getErrors().size());
    }

    @Test
    public void testMultipleWarningDirectory() {
        String path = "../liquidjava-example/src/main/java/testMultiple/warnings";
        CommandLineLauncher.launch(path);
        Diagnostics diagnostics = Diagnostics.getInstance();
        assertEquals(3, diagnostics.getWarnings().size());
    }

    @Test
    public void testMultipleErrorsSameFile() {
        String path = "../liquidjava-example/src/main/java/testMultiple/MultipleErrorsExample.java";
        CommandLineLauncher.launch(path);
        Diagnostics diagnostics = Diagnostics.getInstance();
        assertEquals(2, diagnostics.getErrors().size());
    }

    @Test
    public void testMultipleDirectory() {
        String path = "../liquidjava-example/src/main/java/testMultiple";
        CommandLineLauncher.launch(path);
        Diagnostics diagnostics = Diagnostics.getInstance();

        assertEquals(11, diagnostics.getErrors().size());
        assertEquals(3, diagnostics.getWarnings().size());
    }
}