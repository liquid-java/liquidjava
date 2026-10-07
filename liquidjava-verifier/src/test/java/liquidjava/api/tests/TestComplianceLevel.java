package liquidjava.api.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import liquidjava.api.ComplianceLevel;

class TestComplianceLevel {

    @TempDir
    Path project;

    private Path source() throws IOException {
        Path src = Files.createDirectories(project.resolve("src/main/java"));
        return Files.writeString(src.resolve("A.java"), "class A {}");
    }

    private void writePom(Path dir, String properties, String compilerConfig) throws IOException {
        String plugin = compilerConfig == null ? ""
                : "<build><plugins><plugin><artifactId>maven-compiler-plugin</artifactId><configuration>"
                        + compilerConfig + "</configuration></plugin></plugins></build>";
        Files.writeString(dir.resolve("pom.xml"),
                "<project><modelVersion>4.0.0</modelVersion><groupId>g</groupId><artifactId>a</artifactId>"
                        + "<version>1</version><properties>" + properties + "</properties>" + plugin + "</project>");
    }

    @Test
    void defaultsWithoutPom() throws IOException {
        assertEquals(ComplianceLevel.DEFAULT, ComplianceLevel.resolve(source().toString()));
    }

    @Test
    void readsCompilerProperties() throws IOException {
        writePom(project, "<maven.compiler.source>11</maven.compiler.source>", null);
        assertEquals(11, ComplianceLevel.resolve(source().toString()));
    }

    @Test
    void prefersReleaseOverSource() throws IOException {
        writePom(project,
                "<maven.compiler.source>11</maven.compiler.source><maven.compiler.release>17</maven.compiler.release>",
                null);
        assertEquals(17, ComplianceLevel.resolve(source().toString()));
    }

    @Test
    void readsPluginConfigurationWithPropertyReference() throws IOException {
        writePom(project, "<java.version>1.8</java.version>", "<source>${java.version}</source>");
        assertEquals(8, ComplianceLevel.resolve(source().toString()));
    }

    @Test
    void inheritsFromEnclosingPom() throws IOException {
        writePom(project, "<maven.compiler.release>16</maven.compiler.release>", null);
        Path module = Files.createDirectories(project.resolve("module"));
        writePom(module, "", null);
        Path src = Files.createDirectories(module.resolve("src"));
        assertEquals(16, ComplianceLevel.resolve(src.toString()));
    }

    @Test
    void capsAtMaxSupported() throws IOException {
        writePom(project, "<maven.compiler.release>25</maven.compiler.release>", null);
        assertEquals(ComplianceLevel.MAX_SUPPORTED, ComplianceLevel.resolve(source().toString()));
    }
}
