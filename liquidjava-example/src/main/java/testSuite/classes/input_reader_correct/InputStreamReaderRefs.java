package testSuite.classes.input_reader_correct;

import java.io.InputStream;
import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.Refinement;
import liquidjava.specification.RefinementPredicate;
import liquidjava.specification.StateRefinement;

// https://docs.oracle.com/javase/7/docs/api/java/io/InputStreamReader.html
@ExternalRefinementsFor("java.io.InputStreamReader")
@RefinementPredicate("boolean open(InputStreamReader)")
public interface InputStreamReaderRefs {

    @StateRefinement(to = "open(this)")
    public void InputStreamReader(InputStream in);

    @StateRefinement(from = "open(this)", to = "open(this)")
    @Refinement("(_ >= -1) && (_ <= 127)")
    public int read();

    @StateRefinement(from = "open(this)", to = "!open(this)")
    public void close();

    @StateRefinement(from = "open(this)", to = "open(this)")
    @Refinement("_ >= -1")
    public int read(
            @Refinement("length(cbuf) > 0") char[] cbuf,
            @Refinement("_ >= 0") int offset,
            @Refinement("(_ >= 0) && (_ + offset) <= length(cbuf)") int length);
    //
    //	@StateRefinement(from="open(this)", to="open(this)")
    //	public int ready();
    //
    //	@StateRefinement(from="open(this)", to="open(this)")
    //	public String getEncoding();

}
