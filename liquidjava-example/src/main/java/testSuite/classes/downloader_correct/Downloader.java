package testSuite.classes.downloader_correct;

import liquidjava.specification.Ghost;
import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@Ghost("int progress")
@StateSet({"created", "downloading", "completed"})
public class Downloader {

    @StateRefinement(to="created(this) && progress(this) == 0")
    public Downloader() {}

    @StateRefinement(from="created(this) && progress(this) == 0", to="downloading(this) && progress(this) == 0")
    public void start() {}

    @StateRefinement(from="downloading(this)", to="downloading(this) && progress(this) == percentage")
    public void update(@Refinement("percentage > progress(this)") int percentage) {}

    @StateRefinement(from="downloading(this) && progress(this) == 100", to="completed(this)")
    public void finish() {}
}
