package testSuite.classes.atomic_reference_correct;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"empty", "holding"})
@ExternalRefinementsFor("java.util.concurrent.atomic.AtomicReference")
public interface AtomicReferenceRefinements<V> {

    @StateRefinement(to="initialValue == null ? empty(this) : holding(this)")
    public void AtomicReference(V initialValue);

    @StateRefinement(from="holding(this)")
    public V get();
    
    @StateRefinement(to="newValue == null ? empty(this) : holding(this)")
    public void set(V newValue);
}