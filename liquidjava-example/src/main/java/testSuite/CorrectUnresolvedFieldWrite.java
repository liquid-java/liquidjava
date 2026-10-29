package testSuite;

import java.awt.Point;

import org.example.missing.Options;

// Writes to fields whose declaration is not in the analyzed sources (#426)
@SuppressWarnings("unused")
public class CorrectUnresolvedFieldWrite {
    void configure() {
        Options opts = new Options();
        opts.sampleSize = 1;
    }

    void update(Options opts) {
        opts.sampleSize += 1;
        opts.error = true;
    }

    void move(Point p) {
        p.x = 3;
    }
}
