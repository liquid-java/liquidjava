package testSuite;

import liquidjava.specification.Refinement;

// Yielding a call to a refined constructor from a switch expression (#346)
@SuppressWarnings("unused")
public class CorrectYieldRefinedConstructor {
    static class Conn {
        public Conn(@Refinement("_ > 0") int port) {
        }
    }

    static Conn open(String scheme) {
        return switch (scheme) {
        case "tcp" -> {
            yield new Conn(80);
        }
        default -> new Conn(1);
        };
    }
}
