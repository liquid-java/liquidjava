package liquidjava.smt;

public class SMTResult {
    private final Counterexample counterexample;

    private SMTResult(Counterexample counterexample) {
        this.counterexample = counterexample;
    }

    public static SMTResult ok() {
        return new SMTResult(null);
    }

    public static SMTResult error(Counterexample counterexample) {
        return new SMTResult(counterexample);
    }

    public boolean isOk() {
        return counterexample == null;
    }

    public boolean isError() {
        return !isOk();
    }

    public Counterexample getCounterexample() {
        return counterexample;
    }
}
