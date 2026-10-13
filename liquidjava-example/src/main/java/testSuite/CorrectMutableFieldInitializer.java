package testSuite;

import liquidjava.specification.Refinement;

public class CorrectMutableFieldInitializer {
    private final float fixed = 0.75f;

    @Refinement("_ >= 0.0 && _ <= 1.0")
    private float refined = 0.75f;

    private float local = 0.75f;

    public CorrectMutableFieldInitializer() {
        use(local);
    }

    public void setRefined(@Refinement("_ >= 0.0 && _ <= 1.0") float v) {
        this.refined = v;
    }

    public void setLocal(float v) {
        this.local = v;
    }

    static void use(@Refinement("_ >= 0.0 && _ <= 1.0") float quality) {}

    void writeFixed() {
        use(fixed);
    }

    void writeRefined() {
        use(refined);
    }

    void writeAfterAssignment() {
        local = 0.5f;
        use(local);
    }
}
