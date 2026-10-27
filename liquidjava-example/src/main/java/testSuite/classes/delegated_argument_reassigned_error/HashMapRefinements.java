package testSuite.classes.delegated_argument_reassigned_error;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.Ghost;
import liquidjava.specification.StateRefinement;

@ExternalRefinementsFor("java.util.HashMap")
@Ghost("int capacity")
public interface HashMapRefinements<K, V> {
    @StateRefinement(to = "capacity(this) == initialCapacity")
    void HashMap(int initialCapacity);

    @StateRefinement(from = "capacity(this) == 5")
    V get(Object key);
}
