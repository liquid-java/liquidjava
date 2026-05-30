package testSuite.classes.input_stream_reader_no_constructor_correct;

import liquidjava.specification.*;

// https://docs.oracle.com/javase/7/docs/api/java/io/InputStreamReader.html
@ExternalRefinementsFor("java.io.InputStreamReader")
@StateSet({"open", "closed"})
public interface InputStreamReaderRefinements {

    @StateRefinement(from="open(this)")
    public int read();

    @StateRefinement(from="open(this)", to="closed(this)")
    public void close();
}
