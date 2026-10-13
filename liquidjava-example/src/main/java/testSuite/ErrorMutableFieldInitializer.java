package testSuite;

import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"open", "closed"})
class FieldResource {
    @StateRefinement(to = "open(this)")
    public FieldResource() {}

    @StateRefinement(from = "open(this)")
    public void read() {}

    @StateRefinement(from = "open(this)", to = "closed(this)")
    public void close() {}
}

public class ErrorMutableFieldInitializer {
    private float q = 0.75f;
    private static int count = 1;
    private FieldResource resource = new FieldResource();

    public void setQ(float v) {
        this.q = v;
    }

    public static void setCount(int c) {
        count = c;
    }

    static void use(@Refinement("_ >= 0.0 && _ <= 1.0") float quality) {}

    static void positive(@Refinement("_ > 0") int n) {}

    void write() {
        use(q); // Expect: Refinement Error
    }

    void writeThis() {
        use(this.q); // Expect: Refinement Error
    }

    static void report() {
        positive(count); // Expect: Refinement Error
    }

    void closeResource() {
        resource.close(); // Expect: State Refinement Error
    }

    void readResource() {
        resource.read(); // Expect: State Refinement Error
    }
}
