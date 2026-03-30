package testSuite.classes.input_reader_error2;

import java.io.IOException;
import java.io.InputStreamReader;

public class Test {
    public static void main(String[] args) throws IOException {
        InputStreamReader isr = new InputStreamReader(System.in);
        isr.read();
        isr.close();
        isr.getEncoding();
        isr.read(); // State Refinement Error
    }
}
