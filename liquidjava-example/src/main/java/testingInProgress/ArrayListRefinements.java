package testingInProgress;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.Refinement;
import liquidjava.specification.RefinementPredicate;

@ExternalRefinementsFor("java.util.ArrayList")
@RefinementPredicate("int size(ArrayList l)")
public interface ArrayListRefinements<E> {

    @Refinement("size(this) == 0")
    public void ArrayList();

    @Refinement("size(this) == (size(old(this)) + 1)")
    public void add(E e);

    //	@Refinement("size(this) == (size(old(this)) - 1)")
    //	public void remove(int index);

    //	public E get(@Refinement("index >= 0 && index < size(this)") int index);

    //	@Refinement("size(_) == size(this)")
    //	public Object clone();

}
