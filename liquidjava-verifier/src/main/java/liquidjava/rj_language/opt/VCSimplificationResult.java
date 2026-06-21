package liquidjava.rj_language.opt;

import java.util.Objects;

import liquidjava.processor.VCImplication;

/**
 * Result of simplifying VC implication chain
 */
public final class VCSimplificationResult {

    private final VCImplication implication;
    private final VCSimplificationResult origin;

    public VCSimplificationResult(VCImplication implication) {
        this(implication, null);
    }

    public VCSimplificationResult(VCImplication implication, VCSimplificationResult origin) {
        this.implication = Objects.requireNonNull(implication).clone();
        this.origin = origin;
    }

    /**
     * Returns the simplified VC chain represented by this result
     */
    public VCImplication getImplication() {
        return implication;
    }

    /**
     * Returns the origin of this simplification result or null if this result is the original VC chain
     */
    public VCSimplificationResult getOrigin() {
        return origin;
    }

    @Override
    public String toString() {
        if (origin == null)
            return "\n" + implication;
        return "\n" + implication + "\n" + origin.toString().indent(2).stripTrailing();
    }
}
