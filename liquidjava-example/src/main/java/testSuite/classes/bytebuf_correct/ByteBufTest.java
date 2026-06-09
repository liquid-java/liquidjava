package testSuite.classes.bytebuf_correct;

// SO 48582520 — UnsupportedOperationException at ByteBuffer.array()
// https://stackoverflow.com/questions/48582520
// SO class is already self-contained pure java.nio; only added the package.
import java.nio.ByteBuffer;

public class ByteBufTest {

    public static final int TEST_BUFFER_SIZE = 128;

    // private ByteBuffer mDirectBuffer;

    public ByteBufTest() {
        // FIX (from accepted answer): mDirectBuffer = ByteBuffer.wrap(new byte[TEST_BUFFER_SIZE]);
        // or guard with: if (mDirectBuffer.hasArray()) { ... }
        ByteBuffer mDirectBuffer = ByteBuffer.wrap(new byte[TEST_BUFFER_SIZE]);
        // VIOLATION: a direct buffer is not array-backed -> array() throws
        // UnsupportedOperationException.
        byte[] buf = mDirectBuffer.array();
        buf[1] = 100;
    }
}
