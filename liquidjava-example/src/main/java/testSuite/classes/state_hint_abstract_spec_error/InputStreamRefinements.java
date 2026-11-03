package testSuite.classes.state_hint_abstract_spec_error;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// A spec for an abstract JDK class: its constructor creates the needed state, but no client can call new InputStream()
@ExternalRefinementsFor("java.io.InputStream")
@StateSet({"open", "closed"})
public interface InputStreamRefinements {

    @StateRefinement(to = "open(this)")
    void InputStream();

    @StateRefinement(from = "open(this)")
    int read();

    @StateRefinement(from = "open(this)", to = "closed(this)")
    void close();
}
