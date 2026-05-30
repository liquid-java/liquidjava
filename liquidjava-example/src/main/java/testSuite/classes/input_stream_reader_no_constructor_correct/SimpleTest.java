package testSuite.classes.input_stream_reader_no_constructor_correct;

import java.io.IOException;
import java.io.InputStreamReader;

public class SimpleTest {

    public static void main(String[] args) throws IOException {
        InputStreamReader isr = new InputStreamReader(System.in);
        isr.read();
        isr.close();
    }
}
