package testSuite.classes.bytebuf_error;

// SO 48582520 — UnsupportedOperationException at ByteBuffer.array()
// https://stackoverflow.com/questions/48582520
// SO class is already self-contained pure java.nio; only added the package.
import java.nio.ByteBuffer;

public class ByteBufTest {

    public static final int TEST_BUFFER_SIZE = 128;

    private ByteBuffer mDirectBuffer;

    public ByteBufTest() {
        // FIX (from accepted answer): mDirectBuffer = ByteBuffer.wrap(new byte[TEST_BUFFER_SIZE]);
        // or guard with: if (mDirectBuffer.hasArray()) { ... }
        mDirectBuffer = ByteBuffer.allocateDirect(TEST_BUFFER_SIZE);
        // VIOLATION: a direct buffer is not array-backed -> array() throws
        // UnsupportedOperationException.
        byte[] buf = mDirectBuffer.array(); // State Refinement Error
        buf[1] = 100;
    }

    public void test(ByteBuffer mDirectBuffer) {
        printBuffer("nativeInitDirectBuffer", mDirectBuffer.array()); // State Refinement Error
    }

    private void printBuffer(String tag, byte[] buffer) {
        StringBuffer sBuffer = new StringBuffer();
        for (int i = 0; i < buffer.length; i++) {
            sBuffer.append(buffer[i]);
            sBuffer.append(" ");
        }
    }

    public static void main(String[] args) throws Exception {
        ByteBufTest item = new ByteBufTest();
        ByteBuffer mDirectBuffer = ByteBuffer.allocateDirect(128);
        item.test(mDirectBuffer);
    }
}
