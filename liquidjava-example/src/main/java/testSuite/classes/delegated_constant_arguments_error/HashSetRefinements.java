package testSuite.classes.delegated_constant_arguments_error;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.Ghost;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@ExternalRefinementsFor("java.util.HashSet")
@Ghost("int capacity")
@StateSet({ "plain", "linked" })
public interface HashSetRefinements<E> {
    @StateRefinement(to = "capacity(this) == initialCapacity && (dummy --> linked(this)) && (!dummy --> plain(this))")
    void HashSet(int initialCapacity, float loadFactor, boolean dummy);

    @StateRefinement(from = "capacity(this) == 16 && linked(this)")
    boolean add(E e);

    @StateRefinement(from = "plain(this)")
    void clear();
}
