package liquidjava.rj_language.opt.derivation_node;

/**
 * Origin of a developer-facing state predicate that was derived from a ghost-state equality.
 *
 * <p>
 * When a found-state conjunction holds a known developer state for one object ({@code knownState(a)}) together with an
 * internal ghost-state equality ({@code state1(a) == state1(b)}), the same state must hold for the other object. The
 * derivation pass rewrites this into {@code knownState(b)} and attaches a {@code StateDerivationNode} so the step is
 * explainable: {@code knownState(b)} was derived through {@link #getSourceEquality()} starting from
 * {@link #getSourceState()}.
 */
public class StateDerivationNode extends DerivationNode {

    private final ValDerivationNode sourceEquality;
    private final ValDerivationNode sourceState;

    public StateDerivationNode(ValDerivationNode sourceEquality, ValDerivationNode sourceState) {
        this.sourceEquality = sourceEquality;
        this.sourceState = sourceState;
    }

    /** The ghost-state equality ({@code stateN(x) == stateN(y)}) the derivation went through. */
    public ValDerivationNode getSourceEquality() {
        return sourceEquality;
    }

    /** The known developer state the derivation started from. */
    public ValDerivationNode getSourceState() {
        return sourceState;
    }
}
