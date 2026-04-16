package testSuite.classes.input_reader_error;

import java.io.InputStream;
import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.RefinementPredicate;
import liquidjava.specification.StateRefinement;

// https://docs.oracle.com/javase/7/docs/api/java/io/InputStreamReader.html
@ExternalRefinementsFor("java.io.InputStreamReader")
@RefinementPredicate("boolean open(InputStreamReader i)")
public interface InputStreamReaderRefinements {
    @StateRefinement(to = "open(this)")
    public void InputStreamReader(InputStream in);

    @StateRefinement(from = "open(this)", to = "open(this)")
    public int read();

    @StateRefinement(from = "open(this)", to = "!open(this)")
    public void close();

    // faster
    //	@StateRefinement(from="open(this)", to="open(this)")
    //	public int read(char[] cbuf, int offset, int length);

}
