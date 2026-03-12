package liquidjava.processor.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import liquidjava.rj_language.Predicate;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtTypeReference;

public abstract class RefinedVariable extends Refined {
    private final List<CtTypeReference<?>> supertypes;
    private PlacementInCode placementInCode;

    public RefinedVariable(String name, CtTypeReference<?> type, Predicate c) {
        super(name, type, c);
        supertypes = new ArrayList<>();
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

    public void setPlacementInCode(CtElement element) {
        placementInCode = PlacementInCode.createPlacement(element);
    }

    public void setPlacementInScope(PlacementInCode placement) {
        placementInCode = placement;
    }

    public PlacementInCode getPlacementInCode() {
        return placementInCode;
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
}
