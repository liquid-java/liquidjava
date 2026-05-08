package testSuite.classes.ts_bufferedreader_correct;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

// Regression test for unrefined conditions: each branch reads (open/marked) before
// closing. The verifier must not assume the if-condition is unconditionally true,
// otherwise it would conclude the reader is always closed by line 24 and flag the
// readLine() call on line 31 as a state-refinement error.

class ConfigLoader {

    String loadFirstSetting(String configPath, String defaultValue) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(configPath));

        String header = reader.readLine();
        if (header.isEmpty()) {
            reader.close();
            return defaultValue;
        }

        if (header.startsWith("#")) {
            String value = reader.readLine();
            reader.close();
            return value;
        }

        reader.close();
        return header;
    }
}
