package liquidjava.api.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import liquidjava.specification.Refinement;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Validates that every soundness-hole test program is a genuine soundness hole and not a mislabeled safe program.
 *
 * <p>
 * The companion {@link TestExamples} cases named {@code Error*Unsound} document programs the verifier currently accepts
 * but which are actually unsafe. For each such program this test compiles it and runs it in-process under a child class
 * loader with assertions enabled (no forked JVM); each embeds a runtime {@code assert} that mirrors its
 * {@code @Refinement} — an independent oracle that does not trust the verifier — so a valid hole MUST abort with an
 * {@link AssertionError} at runtime. If a program runs to completion (its refinement actually holds at runtime), it is
 * not a real hole and this test fails — keeping the soundness-hole suite honest.
 */
public class TestSoundnessHoleRuntime {

    private static final Path SUITE = Paths.get("../liquidjava-example/src/main/java/testSuite/");

    /**
     * Per-case compile output; JUnit creates a fresh dir per invocation and deletes it (with the .class files) after.
     */
    @TempDir
    private Path out;

    /**
     * Cases for the runtime validator: a generated self-check fixture (so the harness is exercised even before any
     * soundness-hole test exists, e.g. in the framework commit) followed by the testSuite {@code Error*Unsound.java}
     * programs.
     */
    private static Stream<Path> holePrograms() throws Exception {
        Stream<Path> holes = Files.list(SUITE).filter(Files::isRegularFile).filter(p -> {
            String n = p.getFileName().toString();
            return n.startsWith("Error") && n.contains("Unsound") && n.endsWith(".java");
        }).sorted();
        return Stream.concat(Stream.of(selfCheckFixture()), holes);
    }

    /**
     * Generates a tiny program whose assertion always fails, used to confirm the harness actually observes a
     * {@code -ea} abort. It is generated (not a testSuite file), so it is a passing self-check, not a soundness hole.
     */
    private static Path selfCheckFixture() throws Exception {
        Path dir = Files.createTempDirectory("ljselfcheck");
        Path src = dir.resolve("HarnessSelfCheck.java");
        Files.writeString(src, "package testSuite;\n" + "public class HarnessSelfCheck {\n"
                + "    public static void main(String[] args) { assert false : \"harness self-check\"; }\n" + "}\n");
        dir.toFile().deleteOnExit();
        src.toFile().deleteOnExit();
        return src;
    }

    @ParameterizedTest
    @MethodSource("holePrograms")
    public void runtimeViolatesRefinement(final Path source) throws Exception {
        String fileName = source.getFileName().toString();
        String className = fileName.substring(0, fileName.length() - ".java".length());
        String fqcn = "testSuite." + className;

        // Classpath only needs liquidjava-api (where @Refinement / @StateRefinement live). Locate it from
        // the already-loaded annotation class so we do not depend on the surefire classpath representation.
        String apiPath = new File(Refinement.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                .getAbsolutePath();

        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        assertNotNull(javac, "system Java compiler unavailable (tests must run on a JDK)");
        int compileRc = javac.run(null, null, null, "-cp", apiPath, "-d", out.toString(), source.toString());
        assertEquals(0, compileRc, fileName + " must compile as valid Java");

        // Run in-process (no forked JVM): load the freshly compiled class through a child class loader with
        // assertions enabled, then invoke main on a daemon thread bounded by a timeout. liquidjava-api is already
        // on the parent loader (Refinement is loaded), so the child only needs the compiled program.
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "soundness-hole-" + className);
            t.setDaemon(true);
            return t;
        });
        try (URLClassLoader loader = new URLClassLoader(new URL[] { out.toUri().toURL() },
                getClass().getClassLoader())) {
            loader.setDefaultAssertionStatus(true); // equivalent to -ea, without a subprocess
            Method main = Class.forName(fqcn, true, loader).getMethod("main", String[].class);
            try {
                executor.submit(() -> main.invoke(null, (Object) new String[0])).get(60, TimeUnit.SECONDS);
                fail(fileName + " is NOT a valid soundness hole: it ran to completion without violating its "
                        + "refinement at runtime.");
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof InvocationTargetException invocation) {
                    cause = invocation.getCause();
                }
                assertInstanceOf(AssertionError.class, cause,
                        fileName + " aborted, but not via the refinement assertion: " + cause);
            } catch (TimeoutException e) {
                fail(fileName + " timed out while running");
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
