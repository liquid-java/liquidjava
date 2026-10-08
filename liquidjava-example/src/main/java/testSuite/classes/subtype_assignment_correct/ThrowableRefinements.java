package testSuite.classes.subtype_assignment_correct;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@ExternalRefinementsFor("java.lang.Throwable")
@StateSet({"withThrowable", "noThrowable"})
public interface ThrowableRefinements {
    @StateRefinement(to = "noThrowable(this)")
    void Throwable(String message);

    @StateRefinement(to = "withThrowable(this)")
    void Throwable(String message, Throwable cause);

    @StateRefinement(from = "noThrowable(this)", to = "withThrowable(this)")
    Throwable initCause(Throwable cause);
}
