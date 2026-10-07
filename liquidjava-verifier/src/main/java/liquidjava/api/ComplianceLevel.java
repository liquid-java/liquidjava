package liquidjava.api;

import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Resolves the Java compliance level used by Spoon to parse the verified sources, read from the nearest Maven
 * {@code pom.xml} of each input path.
 */
public final class ComplianceLevel {

    /** Highest level accepted by Spoon 10.4.2 (JDT 3.33); higher levels make the model builder throw */
    public static final int MAX_SUPPORTED = 19;

    /** Level used when no {@code pom.xml} declares one */
    public static final int DEFAULT = MAX_SUPPORTED;

    private static final Pattern PROPERTY_REF = Pattern.compile("\\$\\{([^}]+)\\}");

    private ComplianceLevel() {
    }

    /**
     * Returns the highest level declared by the poms of the given paths, capped at {@link #MAX_SUPPORTED}, or
     * {@link #DEFAULT} if none declares one
     */
    public static int resolve(String... paths) {
        int level = -1;
        for (String path : paths) {
            Optional<Integer> declared = fromMaven(new File(path));
            if (declared.isPresent())
                level = Math.max(level, declared.get());
        }
        if (level < 0)
            return DEFAULT;
        return Math.min(level, MAX_SUPPORTED);
    }

    /**
     * Searches upwards from the path for a {@code pom.xml} that declares a Java version, so a module without one
     * inherits it from the enclosing project
     */
    static Optional<Integer> fromMaven(File path) {
        File dir = path.getAbsoluteFile();
        if (!dir.isDirectory())
            dir = dir.getParentFile();
        for (; dir != null; dir = dir.getParentFile()) {
            File pom = new File(dir, "pom.xml");
            if (pom.isFile()) {
                Optional<Integer> level = readPom(pom);
                if (level.isPresent())
                    return level;
            }
        }
        return Optional.empty();
    }

    /**
     * Reads the Java version from the compiler plugin configuration or the {@code maven.compiler.*} properties,
     * preferring {@code release} over {@code source} as Maven does
     */
    static Optional<Integer> readPom(File pom) {
        Model model;
        try (FileReader reader = new FileReader(pom)) {
            model = new MavenXpp3Reader().read(reader);
        } catch (Exception e) {
            return Optional.empty();
        }
        Properties properties = model.getProperties();
        Xpp3Dom config = compilerConfiguration(model.getBuild());
        String[] candidates = { childValue(config, "release"), properties.getProperty("maven.compiler.release"),
                childValue(config, "source"), properties.getProperty("maven.compiler.source") };
        for (String candidate : candidates) {
            Optional<Integer> level = parseLevel(interpolate(candidate, properties));
            if (level.isPresent())
                return level;
        }
        return Optional.empty();
    }

    private static Xpp3Dom compilerConfiguration(Build build) {
        if (build == null)
            return null;
        Xpp3Dom config = compilerConfiguration(build.getPlugins());
        if (config == null && build.getPluginManagement() != null)
            config = compilerConfiguration(build.getPluginManagement().getPlugins());
        return config;
    }

    private static Xpp3Dom compilerConfiguration(List<Plugin> plugins) {
        for (Plugin plugin : plugins) {
            if ("maven-compiler-plugin".equals(plugin.getArtifactId()) && plugin.getConfiguration() instanceof Xpp3Dom)
                return (Xpp3Dom) plugin.getConfiguration();
        }
        return null;
    }

    private static String childValue(Xpp3Dom config, String name) {
        if (config == null || config.getChild(name) == null)
            return null;
        return config.getChild(name).getValue();
    }

    /**
     * Replaces {@code ${name}} references with the pom properties, leaving unknown ones in place
     */
    private static String interpolate(String value, Properties properties) {
        if (value == null)
            return null;
        for (int i = 0; i < 10; i++) {
            Matcher matcher = PROPERTY_REF.matcher(value);
            if (!matcher.find())
                break;
            String replacement = properties.getProperty(matcher.group(1));
            if (replacement == null)
                break;
            value = matcher.replaceFirst(Matcher.quoteReplacement(replacement));
        }
        return value;
    }

    /**
     * Parses a Java version such as {@code 17} or {@code 1.8}
     */
    static Optional<Integer> parseLevel(String value) {
        if (value == null)
            return Optional.empty();
        String version = value.trim();
        if (version.startsWith("1."))
            version = version.substring(2);
        try {
            return Optional.of(Integer.parseInt(version));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
