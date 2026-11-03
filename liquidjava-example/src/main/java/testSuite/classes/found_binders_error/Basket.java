package testSuite.classes.found_binders_error;

import java.util.ArrayList;
import java.util.Random;

import liquidjava.specification.Refinement;

// Found drops the true rows the error does not need and writes fields as in code (#457)
public class Basket {
    static int served;

    @Refinement("_ >= 0")
    int level;

    private final ArrayList<String> items;
    private final Random random = new Random();

    Basket(ArrayList<String> items) {
        this.items = items;
    }

    // the issue's reproducer: the field row only says true
    String pick() {
        ArrayList<String> items = this.items;
        return items.get(random.nextInt(items.size())); // Expect: Refinement Error
    }

    // a static field
    void serve() {
        @Refinement("_ > 0")
        int count = served + 1; // Expect: Refinement Error
    }

    // writes to another object's field are checked as on main, and named after that object
    void setOther(Basket other) {
        other.level = -1; // Expect: Refinement Error
    }

    void incrementOther(Basket other) {
        other.level++;
    }

    void addToOther(Basket other) {
        other.level += 1;
    }

    void decrementOther(Basket other) {
        other.level--; // Expect: Refinement Error
    }

    void writeThroughAlias() {
        level = 7;
        Basket other = this;
        other.level = 3;
        @Refinement("_ == 7")
        int k = level; // Expect: Refinement Error
    }
}
