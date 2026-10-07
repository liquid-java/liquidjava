package testSuite.classes.try_with_resources_correct;

public class ResTest {
    static void useInside() {
        try (Res r = new Res()) {
            r.read();
            r.read();
        }
    }

    static void multipleResources() {
        try (Res a = new Res(); Res b = new Res()) {
            a.read();
            b.read();
        }
    }

    static void resourceReference() {
        Res r = new Res();
        r.read();
        try (r) {
            r.read();
        }
    }

    static void withCatchAndFinally() {
        try (Res r = new Res()) {
            r.read();
        } catch (RuntimeException e) {
            e.getMessage();
        } finally {
            Res s = new Res();
            s.read();
        }
    }
}
