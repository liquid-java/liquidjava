package testSuite.classes.loop_list_index_correct;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.Ghost;
import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;

@ExternalRefinementsFor("java.util.ArrayList")
@Ghost("int size")
public interface ArrayListRefinements<E> {
    @StateRefinement(to = "size(this) == 0")
    void ArrayList();

    @StateRefinement(to = "size(this) == size(old(this)) + 1")
    boolean add(E e);

    E get(@Refinement("_ >= 0 && _ < size(this)") int index);

    @Refinement("_ == size(this) && _ >= 0")
    int size();
}
