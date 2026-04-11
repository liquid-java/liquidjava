package testSuite.classes.downloader_correct;

public class SimpleTest {
    public static void main(String[] args) {
        Downloader d = new Downloader();
        d.start();
        d.update(50);
        d.update(100);
        d.finish();
    }
}
