package liquidjava.rj_language.opt;

import java.util.ArrayList;
import java.util.List;

import liquidjava.processor.VCImplication;

/**
 * Result of simplifying VC implication chain
 */
public final class VCSimplificationResult {

    private final VCImplication implication;
    private final VCSimplificationResult origin;
    private final String simplification;

    public VCSimplificationResult(VCImplication implication) {
        this.implication = implication.clone();
        this.origin = null;
        this.simplification = null;
    }

    public VCSimplificationResult(VCImplication implication, VCSimplificationResult origin, String simplification) {
        this.implication = implication.clone();
        this.origin = origin;
        this.simplification = simplification;
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

    /**
     * Returns the name of the simplification pass that produced this result or null for the original VC chain
     */
    public String getSimplification() {
        return simplification;
    }

    /**
     * Returns the list of binder names in the simplified VC chain in order of appearance
     */
    public List<String> getBinders() {
        ArrayList<String> binderNames = new ArrayList<>();
        for (VCImplication current = getImplication(); current != null; current = current.getNext())
            if (current.hasBinder())
                binderNames.add(current.getName());
        return binderNames;
    }

    @Override
    public String toString() {
        if (origin == null)
            return "\n" + implication;
        return "\n" + implication + "\n" + origin.toString().indent(2).stripTrailing();
    }
}
