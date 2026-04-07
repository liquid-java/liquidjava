package testingInProgress;

import liquidjava.specification.RefinementPredicate;
import liquidjava.specification.StateRefinement;

@RefinementPredicate("int lengthS(StringBuilder s)")
public interface StringBuilderRefinements {
    @StateRefinement(to = "lengthS() == 0")
    public void StringBuilder();

    @StateRefinement(from = "#i == lengthS()", to = "lengthS() == (#i + 1)")
    public StringBuilder append(char c);
}
