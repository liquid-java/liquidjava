package testSuite.classes.downloader_refinement_error;

public class SimpleTest {
    public static void main(String[] args) {
        Downloader d = new Downloader();
        d.start();
        d.update(50);
        d.update(40); // Refinement Error
    }
}
