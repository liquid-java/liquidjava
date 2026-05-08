package testSuite.classes.ts_bufferedreader_error;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

// (A) The model is co-located: BufferedReaderRefinementsExpert (de-packaged
// copy) declares states open/marked/closed on java.io.BufferedReader and
// requires open or marked for read methods.
//
// (B) The client: load the first non-comment setting from a config file.
// The author handles three branches — empty file, leading-comment header,
// plain header — and tries to close the reader as soon as it is no longer
// needed. In the leading-comment branch the close happens too early; the
// follow-up readLine runs against a closed reader.

class ConfigLoader {

    String loadFirstSetting(String configPath, String defaultValue) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(configPath));

        String header = reader.readLine();
        if (header.isEmpty()) {
            reader.close();
            return defaultValue;
        }

        if (header.startsWith("#")) {
            // Header was a comment — the real value is on the next line.
            reader.close();
            return reader.readLine(); // State Refinement Error
        }

        reader.close();
        return header;
    }

    String loadFirstSetting2(String configPath, String defaultValue) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(configPath));

        String header = reader.readLine();
        if (header.isEmpty()) {
            reader.close();
            // no return here
        }
        reader.close(); // State Refinement Error
        return header;
    }
}
