package liquidjava.processor.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import liquidjava.rj_language.Predicate;
import liquidjava.utils.Utils;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtTypeReference;

public abstract class RefinedVariable extends Refined {
    private final List<CtTypeReference<?>> supertypes;
    private PlacementInCode placementInCode;
    private boolean isParameter;
    private SourcePosition annPosition;
    private Predicate failingRefinement;

    public RefinedVariable(String name, CtTypeReference<?> type, Predicate c) {
        super(name, type, c);
        supertypes = new ArrayList<>();
        isParameter = false;
    }

    public abstract Predicate getMainRefinement();

    public void addSuperType(CtTypeReference<?> t) {
        if (!supertypes.contains(t))
            supertypes.add(t);
    }

    public List<CtTypeReference<?>> getSuperTypes() {
        return supertypes;
    }

    public void addSuperTypes(CtTypeReference<?> ts, Set<CtTypeReference<?>> sts) {
        if (ts != null && !supertypes.contains(ts))
            supertypes.add(ts);
        for (CtTypeReference<?> ct : sts)
            if (ct != null && !supertypes.contains(ct))
                supertypes.add(ct);
    }

    public void addPlacementInCode(CtElement element) {
        placementInCode = PlacementInCode.createPlacement(element);
        annPosition = Utils.getFirstLJAnnotationPosition(element);
    }

    public void addPlacementInCode(PlacementInCode placement) {
        placementInCode = placement;
    }

    public PlacementInCode getPlacementInCode() {
        return placementInCode;
    }

    public SourcePosition getAnnPosition() {
        return annPosition;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((supertypes == null) ? 0 : supertypes.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        RefinedVariable other = (RefinedVariable) obj;
        if (supertypes == null) {
            return other.supertypes == null;
        } else {
            return supertypes.equals(other.supertypes);
        }
    }

    public void setIsParameter(boolean b) {
        isParameter = b;
    }

    public boolean isParameter() {
        return isParameter;
    }

    public void setFailingRefinement(Predicate failingRefinement) {
        this.failingRefinement = failingRefinement;
    }

    public Predicate getFailingRefinement() {
        return failingRefinement;
    }
}
