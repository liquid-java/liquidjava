package testSuite.classes.input_reader_error;

import java.io.IOException;
import java.io.InputStreamReader;

public class Test {

    public static void main(String[] args) throws IOException {
        // java.io.InputStreamReader.InputStreamReader
        InputStreamReader is = new InputStreamReader(System.in);
        is.read();
        is.read();
        is.close();
        is.read(); // State Refinement Error
    }
}
