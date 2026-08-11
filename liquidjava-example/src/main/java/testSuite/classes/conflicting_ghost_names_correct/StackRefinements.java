package testSuite.classes.conflicting_ghost_names_correct;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.Ghost;
import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;

@ExternalRefinementsFor("java.util.Stack")
@Ghost("int size")
public interface StackRefinements<E> {

	public void Stack();

	@StateRefinement(to="size(this) == size(old(this)) + 1")
	public E push(E elem);

	@StateRefinement(from="size(this) > 0", to="size(this) == size(old(this)) - 1")
	public E pop();

	@StateRefinement(from="size(this) > 0")
	public E peek();

	@Refinement("_ ? size(this) == 0 : size(this) > 0")
	public boolean empty();
}
