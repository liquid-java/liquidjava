package testSuite.classes.downloader_state_refinement_error;

public class SimpleTest {
    public static void main(String[] args) {
        Downloader d = new Downloader();
        d.start();
        d.update(50);
        d.finish(); // State Refinement Error
    }
}
