package testSuite.classes.state_multiple_error;

import java.io.InputStream;
import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// https://docs.oracle.com/javase/7/docs/api/java/io/InputStreamReader.html
@ExternalRefinementsFor("java.io.InputStreamReader")
@StateSet({"open", "close"})
@StateSet({"alreadyRead", "nothingRead"})
public interface InputStreamReaderRefinements {

    @StateRefinement(to = "open(this) && close(this)") // State Conflict Error
    public void InputStreamReader(InputStream in);

    @StateRefinement(from = "open(this)", to = "open(this) && alreadyRead(this)")
    @Refinement("(_ >= -1) && (_ <= 127)")
    public int read();

    @StateRefinement(from = "close(this)", to = "close(this)")
    @StateRefinement(from = "open(this)", to = "close(this)")
    public void close();
}
