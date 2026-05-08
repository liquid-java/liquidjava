package testSuite.classes.ts_bufferedreader_error;
import java.io.IOException;
import java.io.Reader;
import java.util.stream.Stream;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.Refinement;
import liquidjava.specification.RefinementAlias;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@RefinementAlias("NonNegative(int v) { v >= 0 }")
@RefinementAlias("Positive(int v) { v > 0 }")
@StateSet({"open", "marked", "closed"})
@ExternalRefinementsFor("java.io.BufferedReader")
public interface BufferedReaderRefinementsExpert {

    @StateRefinement(to="open(this)")
    public void BufferedReader(Reader in);

    @StateRefinement(to="open(this)")
    public void BufferedReader(Reader in, @Refinement("Positive(_)") int sz);

    @StateRefinement(from="open(this)")
    @StateRefinement(from="marked(this)")
    public int read();

    @StateRefinement(from="open(this)")
    @StateRefinement(from="marked(this)")
    @Refinement(" _ >= -1")
    public int read(char[] cbuf, @Refinement("NonNegative(_)") int off, @Refinement("NonNegative(_)") int len);

    @StateRefinement(from="open(this)")
    @StateRefinement(from="marked(this)")
    public String readLine() throws IOException;

    @StateRefinement(from="open(this)")
    @StateRefinement(from="marked(this)")
    public boolean ready() throws IOException;

    @StateRefinement(from="open(this)")
    @StateRefinement(from="marked(this)")
    public boolean markSupported();

    @StateRefinement(from="open(this)", to="marked(this)")
    @StateRefinement(from="marked(this)")
    public void mark(@Refinement("NonNegative(_)") int readAheadLimit);

    @StateRefinement(from="marked(this)", to="open(this)")
    public void reset();

    @StateRefinement(from="!closed(this)", to="closed(this)")
    public void close();

    @StateRefinement(from="open(this)")
    @StateRefinement(from="marked(this)")
    public Stream<String> lines();
}
