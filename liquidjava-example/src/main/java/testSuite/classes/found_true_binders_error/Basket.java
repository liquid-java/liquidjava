package testSuite.classes.found_true_binders_error;

import java.util.ArrayList;
import java.util.Random;

import liquidjava.specification.Refinement;

// Found keeps a binder refined only by true when the error needs it (#457)
public class Basket {
    static int served;

    private final ArrayList<String> items;
    private final ArrayList<String> extras;
    private final Random random = new Random();
    int amount;

    Basket(ArrayList<String> items, ArrayList<String> extras) {
        this.items = items;
        this.extras = extras;
    }

    // the issue's reproducer: the field row only says true
    String pick() {
        ArrayList<String> items = this.items;
        return items.get(random.nextInt(items.size())); // Expect: Refinement Error
    }

    // a parameter refined only by true
    String pickFrom(ArrayList<String> list) {
        return list.get(random.nextInt(list.size())); // Expect: Refinement Error
    }

    // a call result refined only by true
    String pickCopy() {
        ArrayList<String> copy = copyItems();
        return copy.get(random.nextInt(copy.size())); // Expect: Refinement Error
    }

    ArrayList<String> copyItems() {
        return new ArrayList<>(items);
    }

    // a local refined only by true: the loop may have reassigned it
    String pickLocal(ArrayList<String> list) {
        ArrayList<String> local = list;
        for (String name : list) {
            local = list;
        }
        return local.get(random.nextInt(local.size())); // Expect: Refinement Error
    }

    // several fields, read with and without this
    int pickAny() {
        int first = this.items.size();
        int second = extras.size();
        return random.nextInt(first + second); // Expect: Refinement Error
    }

    // a later row uses the parameter and the counterexample has a value for it
    void next(int x) {
        @Refinement("_ > 0")
        int y = x + 1; // Expect: Refinement Error
    }

    // the expected refinement is about the parameter
    void forward(int x, int unrelated) {
        positive(x); // Expect: Refinement Error
    }

    void positive(@Refinement("_ > 0") int v) {
    }

    // a static field
    void serve() {
        @Refinement("_ > 0")
        int count = served + 1; // Expect: Refinement Error
    }

    // an instance field read with this
    void withThis() {
        @Refinement("_ > 0")
        int a = this.amount; // Expect: Refinement Error
    }

    // the same field read without this
    void withoutThis() {
        @Refinement("_ > 0")
        int a = amount; // Expect: Refinement Error
    }

    class Shelf {
        ArrayList<String> names;

        // a field of an inner class
        String pick() {
            return names.get(new Random().nextInt(names.size())); // Expect: Refinement Error
        }
    }

    // a guard on a parameter unrelated to the expected one
    void guarded(int port, int t) {
        if (port < 0)
            return;
        positive(t); // Expect: Refinement Error
    }

    // one guard on two flags unrelated to the expected parameter
    void flags(boolean p, boolean q, int t) {
        if (p || q)
            return;
        positive(t); // Expect: Refinement Error
    }

    // the guard of a loop uses its variable before the variable's row
    void count(int start) {
        for (int i = start; i < served; i++) {
            positive(i); // Expect: Refinement Error
        }
    }
}
