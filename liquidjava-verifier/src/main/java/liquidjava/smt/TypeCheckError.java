package liquidjava.smt;

import liquidjava.diagnostics.Counterexample;

public class TypeCheckError extends Exception {

    private final Counterexample counterexample;

    public TypeCheckError(Counterexample counterexample) {
        super("Refinement was violated");
        this.counterexample = counterexample;
    }

    public Counterexample getCounterexample() {
        return counterexample;
    }
}
