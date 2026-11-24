package liquidjava.api.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import liquidjava.api.CommandLineLauncher;
import liquidjava.diagnostics.Diagnostics;

class TestFloatGhostDefault {

    @TempDir
    Path dir;

    @Test
    void floatGhostUsesZeroAsDefaultValue() throws IOException {
        String source = """
                import liquidjava.specification.Ghost;
                import liquidjava.specification.StateRefinement;

                @Ghost("float level")
                class Tank {
                    @StateRefinement(to = "true")
                    public void fill() {}
                }

                public class Main {
                    public static void main(String[] args) {
                        Tank t = new Tank();
                        t.fill();
                    }
                }
                """;

        Files.writeString(dir.resolve("Main.java"), source);

        CommandLineLauncher.launch(dir.toString());

        assertTrue(Diagnostics.getInstance().getErrors().isEmpty(),
                () -> "Float ghost should not produce errors: " + Diagnostics.getInstance().getErrors());
    }
}