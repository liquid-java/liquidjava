package testSuite.classes.refs_from_interface_error;

import java.io.IOException;

public class SimpleTest {

    public static void main(String[] args) throws IOException {
        Bus b = new Bus();
        b.setYear(1500); // Refinement Error
    }
}
