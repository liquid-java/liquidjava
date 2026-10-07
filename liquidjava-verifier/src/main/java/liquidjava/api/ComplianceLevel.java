package liquidjava.api;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;

import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

/**
 * Java compliance level used by Spoon, read from the {@code maven.compiler.release} or {@code maven.compiler.source}
 * property of the nearest {@code pom.xml} of the verified paths
 */
public final class ComplianceLevel {

    /** Highest level accepted by Spoon 10.4.2 (JDT 3.33), also used when no pom declares one */
    public static final int MAX_SUPPORTED = 19;

    public static int resolve(String... paths) {
        return Arrays.stream(paths).map(path -> fromPom(new File(path).getAbsoluteFile())).flatMap(Optional::stream)
                .max(Integer::compare).map(level -> Math.min(level, MAX_SUPPORTED)).orElse(MAX_SUPPORTED);
    }

    private static Optional<Integer> fromPom(File path) {
        for (File dir = path; dir != null; dir = dir.getParentFile()) {
            File pom = new File(dir, "pom.xml");
            if (!pom.isFile())
                continue;
            try (FileReader reader = new FileReader(pom)) {
                Properties properties = new MavenXpp3Reader().read(reader).getProperties();
                String version = properties.getProperty("maven.compiler.release",
                        properties.getProperty("maven.compiler.source"));
                if (version != null)
                    return Optional.of(Integer.parseInt(version.trim().replaceFirst("^1\\.", "")));
            } catch (Exception ignored) {
                // unreadable pom or non-numeric version: keep searching enclosing poms
            }
        }
        return Optional.empty();
    }
}
