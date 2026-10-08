package testSuite.classes.throwable_subclass_correct;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@ExternalRefinementsFor("java.lang.Throwable")
@StateSet({ "noThrowable", "withThrowable" })
public interface ThrowableRefinements {
    @StateRefinement(to = "noThrowable(this)")
    public void Throwable();

    @StateRefinement(to = "noThrowable(this)")
    public void Throwable(String message);

    @StateRefinement(to = "withThrowable(this)")
    public void Throwable(String message, Throwable cause);

    @StateRefinement(to = "withThrowable(this)")
    public void Throwable(Throwable cause);

    @StateRefinement(from = "noThrowable(this)", to = "withThrowable(this)")
    public Throwable initCause(Throwable cause);
}
