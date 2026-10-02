package liquidjava.smt;

import liquidjava.rj_language.Predicate;

public class SMTResult {
    private final Counterexample counterexample;
    private final Predicate finalExpected;

    private SMTResult(Counterexample counterexample, Predicate finalExpected) {
        this.counterexample = counterexample;
        this.finalExpected = finalExpected;
    }

    public static SMTResult ok() {
        return new SMTResult(null, null);
    }

    public static SMTResult error(Counterexample counterexample) {
        return new SMTResult(counterexample, null);
    }

    public SMTResult withFinalExpected(Predicate expected) {
        return new SMTResult(counterexample, expected);
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

    public Predicate getFinalExpected() {
        return finalExpected;
    }
}
