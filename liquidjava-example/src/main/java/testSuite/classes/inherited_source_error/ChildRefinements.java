package testSuite.classes.inherited_source_error;

import liquidjava.specification.*;

@ExternalRefinementsFor("testSuite.classes.inherited_source_error.Child")
@StateSet({"ready", "changed"})
public interface ChildRefinements {
    @StateRefinement(to = "ready(this)") void Child();
    @StateRefinement(to = "changed(this)") void change(int value);
    @StateRefinement(from = "ready(this)") void use();
}
