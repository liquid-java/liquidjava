package testSuite.classes.found_true_binders_error;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.Ghost;
import liquidjava.specification.Refinement;

@ExternalRefinementsFor("java.util.ArrayList")
@Ghost("int size")
public interface ArrayListRefinements<E> {

    public void ArrayList();

    @Refinement("_ == size(this) && _ >= 0")
    public int size();
}
