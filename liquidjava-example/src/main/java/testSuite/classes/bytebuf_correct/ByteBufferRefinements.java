package testSuite.classes.bytebuf_correct;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.Ghost;
import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;

import java.nio.ByteBuffer;


@Ghost("boolean arrayBacked")
@ExternalRefinementsFor("java.nio.ByteBuffer")
public interface ByteBufferRefinements {

    // ---- Backing array access ----

    @StateRefinement(to="_ ? arrayBacked() : !arrayBacked()")
    boolean hasArray();

    @StateRefinement(from="arrayBacked()")
    byte[] array();

    @StateRefinement(from="arrayBacked()")
    int arrayOffset();
    
    @Refinement("arrayBacked(_)")
    ByteBuffer wrap(byte[] array, int offset, int length);

    @Refinement("arrayBacked(_)")
    ByteBuffer wrap(byte[] array);

  }
