package liquidjava.api;

import java.io.FileReader;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "liquidjava", mixinStandardHelpOptions = false, customSynopsis = "./liquidjava <...paths> <options>")
public class CommandLineArgs {
    @Option(names = { "-h", "--help" }, usageHelp = true, description = "Display this help message")
    public boolean help;

    @Option(names = { "-v", "--version" }, versionHelp = true, description = "Display the version of LiquidJava")
    public boolean version;

    @Option(names = { "-d", "--debug" }, description = "Enable debug mode for more detailed output")
    public boolean debugMode;

    @Parameters(arity = "1..*", paramLabel = "<...paths>", description = "Paths to be verified by LiquidJava")
    public List<String> paths;

    public String getVersionString() {
        Path pomPath = Path.of("liquidjava-verifier", "pom.xml");
        try (FileReader fileReader = new FileReader(pomPath.toFile())) {
            Model model = new MavenXpp3Reader().read(fileReader);
            return model.getVersion();
        } catch (Exception ignored) {
            return "unknown";
        }
    }
}
