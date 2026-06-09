package testSuite.classes.iterator_queue_error;

// SO 22361194 — "Iterator.remove() IllegalStateException"
// https://stackoverflow.com/questions/22361194
// Faithful to the question body. The asker's own (unshown) types are given
// minimal stand-ins so the original structure compiles:
//   - the queues qev1/qev2/qcv1/qcv2 -> a small Queue exposing enqueue()/iterator()
//   - CInteger -> the nested class below
// The asker catches UnsupportedOperationException, but iterator().remove()
// before next() actually throws IllegalStateException — so the catch does NOT
// fire. That mismatch is reproduced verbatim from the post.
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

public class IteratorRemoveBeforeNext {

    static class Queue<T> implements Iterable<T> {
        private final ArrayList<T> items = new ArrayList<>();
        void enqueue(T item) { items.add(item); }
        public Iterator<T> iterator() { return items.iterator(); }
    }

    static class CInteger {
        final int value;
        CInteger(int value) { this.value = value; }
    }

    public static void main(String[] args) {
        Queue<Object> qev1 = new Queue<>();
        Queue<Object> qev2 = new Queue<>();
        Queue<Object> qcv1 = new Queue<>();
        Queue<Object> qcv2 = new Queue<>();
        Object ci = new CInteger(0);

        try {
            // VIOLATION: remove() before next() -> IllegalStateException
            // (NOT UnsupportedOperationException, so this catch does not fire).
            Iterator<Object> it = qev1.iterator();
            it.remove(); // State Refinement Error
        } catch (UnsupportedOperationException e) {
            System.out.println("Calling Iterator.remove() and throwing exception.");
        }

        qev1.enqueue(ci);
        qev2.enqueue(ci);
        qcv1.enqueue(ci);
        qcv2.enqueue(ci);

        for (int i = 1; i < 5; i++) {
            if (i % 2 == 0) {
                qev1.enqueue(new CInteger(i + 1));
                qev2.enqueue(new CInteger(i + 1));
                qcv1.enqueue(new CInteger(i + 1));
                qcv2.enqueue(new CInteger(i + 1));
            } else {
                qev1.enqueue(new Date(i * i));
                qev2.enqueue(new Date(i * i));
                qcv1.enqueue(new Date(i * i));
                qcv2.enqueue(new Date(i * i));
            }
        }
    }
}
