package testSuite.classes.arraylist_correct;

import liquidjava.specification.*;

@ExternalRefinementsFor("java.util.ArrayList")
public interface ArrayListRefinements<E> {

    @RefinementPredicate("int size(ArrayList l)")
    @StateRefinement(to = "size(this) == 0")
    public void ArrayList();

    @StateRefinement(to = "size(this) == (size(old(this)) + 1)")
    public boolean add(E e);

    //	@Refinement("size(_) == size(this)")
    //	public Object clone();

}
