package testSuite.classes.arraylist_correct;

import liquidjava.specification.*;

@ExternalRefinementsFor("java.util.ArrayList")
@RefinementPredicate("int size(ArrayList l)")
public interface ArrayListRefinements<E> {

    @StateRefinement(to = "size(this) == 0")
    public void ArrayList();

    @StateRefinement(to = "size(this) == (size(old(this)) + 1)")
    public boolean add(E e);

    //	@Refinement("size(_) == size(this)")
    //	public Object clone();

}
