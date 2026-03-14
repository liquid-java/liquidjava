package testSuite.classes.overload_constructors_error;

import liquidjava.specification.StateSet;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.ExternalRefinementsFor;

@StateSet({"start", "hasMessage", "hasCause"})
@ExternalRefinementsFor("java.lang.Throwable")
public interface ThrowableRefinements {

    // ##### Constructors #######
    @StateRefinement(to="hasMessage(this)")
    public void Throwable(String message);

    @StateRefinement(to="hasCause(this)")
    public void Throwable(Throwable cause);

    @StateRefinement(from="!hasCause(this)", to="hasCause(this)")
    public Throwable initCause(Throwable cause);
}