package testSuite.classes.atomic_reference_correct;

import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unused")
public class AtomicReferenceTest {
    
    public void testOk() {
        AtomicReference<String> ref = new AtomicReference<>("hello");
        String s = ref.get();

        ref.set("world");
        String s2 = ref.get();
    }
}
