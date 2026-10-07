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

    private int resolveWithRelease(String release) throws IOException {
        Files.writeString(project.resolve("pom.xml"),
                "<project><modelVersion>4.0.0</modelVersion><properties><maven.compiler.release>" + release
                        + "</maven.compiler.release></properties></project>");
        return ComplianceLevel.resolve(Files.createDirectories(project.resolve("src")).toString());
    }

    @Test
    void readsLevelFromPom() throws IOException {
        assertEquals(8, resolveWithRelease("1.8"));
    }

    @Test
    void capsAtMaxSupported() throws IOException {
        assertEquals(ComplianceLevel.MAX_SUPPORTED, resolveWithRelease("30"));
    }
}
