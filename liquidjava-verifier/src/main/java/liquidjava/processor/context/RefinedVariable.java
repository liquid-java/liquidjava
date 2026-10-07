package liquidjava.processor.context;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import liquidjava.diagnostics.DebugLog;
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

    /**
     * Records the given superclass and interfaces and, transitively, all of theirs: a spec written for an indirect
     * supertype must also apply to this variable (e.g. the {@code Throwable} spec on an {@code IOException}, whose
     * direct superclass is {@code Exception}).
     */
    public void addSuperTypes(CtTypeReference<?> ts, Set<CtTypeReference<?>> sts) {
        // LinkedList, unlike ArrayDeque, accepts the nulls of a missing superclass, skipped when polled
        Deque<CtTypeReference<?>> todo = new LinkedList<>(sts);
        todo.addFirst(ts);
        while (!todo.isEmpty()) {
            CtTypeReference<?> t = todo.poll();
            if (t == null || supertypes.contains(t))
                continue;
            supertypes.add(t);
            try {
                todo.add(t.getSuperclass());
                todo.addAll(t.getSuperInterfaces());
            } catch (RuntimeException | LinkageError e) {
                // a supertype that cannot be resolved (no source, not on the classpath) ends the walk on that branch
                DebugLog.warn("Could not resolve the supertypes of " + t.getQualifiedName()
                        + "; specs declared above it will not apply to " + getName() + " (" + e + ")");
            }
        }
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
