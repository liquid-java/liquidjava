package testSuite.classes.atomic_reference_error;

import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unused")
public class AtomicReferenceTest {
    
    public void testError() {
        AtomicReference<String> ref = new AtomicReference<>(null);
        String s = ref.get(); // error
    }
}

