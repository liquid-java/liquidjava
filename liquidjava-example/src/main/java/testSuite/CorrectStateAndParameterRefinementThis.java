package testSuite;

import liquidjava.specification.Ghost;
import liquidjava.specification.Refinement;
import liquidjava.specification.RefinementPredicate;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@Ghost("int progress")
@StateSet({"downloading", "completed"})
public class CorrectStateAndParameterRefinementThis {

    @StateRefinement(from = "downloading(this)", to = "progress(this) == percentage")
    public void updateProgress(@Refinement("percentage > progress(this)") int percentage) {}
}
