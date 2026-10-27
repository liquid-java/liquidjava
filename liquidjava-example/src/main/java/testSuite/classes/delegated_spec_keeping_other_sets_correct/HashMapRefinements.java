package testSuite.classes.delegated_spec_keeping_other_sets_correct;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@ExternalRefinementsFor("java.util.HashMap")
@StateSet({ "a1", "a2" })
@StateSet({ "b1", "b2" })
public interface HashMapRefinements<K, V> {
    @StateRefinement(to = "a2(this)")
    void HashMap(int initialCapacity);

    @StateRefinement(from = "b1(this)")
    V get(Object key);
}
