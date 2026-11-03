package testSuite.classes.loop_state_external_error;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@ExternalRefinementsFor("java.io.BufferedReader")
@StateSet({"open", "marked", "closed"})
public interface BufferedReaderRefinements {
    @StateRefinement(to = "open(this)")
    void BufferedReader(java.io.Reader in);

    @StateRefinement(from = "open(this)")
    @StateRefinement(from = "marked(this)")
    String readLine();

    @StateRefinement(from = "open(this)", to = "marked(this)")
    @StateRefinement(from = "marked(this)")
    void mark(int readAheadLimit);

    @StateRefinement(from = "marked(this)", to = "open(this)")
    void reset();

    @StateRefinement(from = "!closed(this)", to = "closed(this)")
    void close();
}
