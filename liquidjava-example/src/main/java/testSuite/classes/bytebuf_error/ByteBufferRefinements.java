package testSuite.classes.bytebuf_error;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.Ghost;
import liquidjava.specification.Refinement;
import liquidjava.specification.RefinementAlias;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.ShortBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.FloatBuffer;
import java.nio.DoubleBuffer;


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
