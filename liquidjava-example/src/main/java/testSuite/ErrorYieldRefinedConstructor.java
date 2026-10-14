package testSuite;

import liquidjava.specification.Refinement;

// The arguments of a refined constructor yielded from a switch expression are checked (#346)
@SuppressWarnings("unused")
public class ErrorYieldRefinedConstructor {
    static class Conn {
        public Conn(@Refinement("_ > 0") int port) {
        }
    }

    static Conn open(String scheme) {
        return switch (scheme) {
        case "tcp" -> {
            yield new Conn(-1); // Expect: Refinement Error
        }
        default -> new Conn(1);
        };
    }
}
