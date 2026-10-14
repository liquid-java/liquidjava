package testSuite;

import org.example.missing.Config;

import liquidjava.specification.Refinement;

// A violation next to a field whose declaring class cannot be resolved is still reported (#390)
@SuppressWarnings("unused")
public class ErrorUnresolvedFieldDeclaringType {
    static void positive(@Refinement("_ > 0") int x) {
    }

    static void field(Config config) {
        if (config.httpAdminPassword.length() < 8) {
            positive(-2); // Expect: Refinement Error
        }
    }
}
