package liquidjava.processor;

import java.util.Objects;

import liquidjava.rj_language.Predicate;

/**
 * Represents a VC implication node whose refinement was simplified from another quantified VC shape.
 */
public class SimplifiedVCImplication extends VCImplication {

    private final VCImplication origin;

    public SimplifiedVCImplication(VCImplication implication, Predicate refinement, VCImplication origin) {
        super(implication, refinement);
        this.origin = origin.clone();
    }

    @Override
    public VCImplication getOrigin() {
        return origin;
    }

    @Override
    public Predicate getOriginRefinement() {
        return origin.getRefinement().clone();
    }

    @Override
    public VCImplication copyWithRefinement(Predicate refinement) {
        return new SimplifiedVCImplication(this, refinement, origin);
    }

    @Override
    public SimplifiedVCImplication clone() {
        SimplifiedVCImplication vc = new SimplifiedVCImplication(this, this.refinement.clone(), origin);
        if (this.next != null)
            vc.next = this.next.clone();
        return vc;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), origin);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof SimplifiedVCImplication))
            return false;
        if (!super.equals(obj))
            return false;
        SimplifiedVCImplication other = (SimplifiedVCImplication) obj;
        return origin.equals(other.origin);
    }
}
