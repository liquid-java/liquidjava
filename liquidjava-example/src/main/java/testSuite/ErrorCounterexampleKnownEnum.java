package testSuite;

import liquidjava.specification.Refinement;

public class ErrorCounterexampleKnownEnum {

    enum Format {
        JPG, PNG
    }

    static void onlyPng(@Refinement("f == Format.PNG") Format f) {}

    static void probe(Format format) {
        if (format == Format.JPG)
            onlyPng(format); // Expect: Refinement Error
    }
}
