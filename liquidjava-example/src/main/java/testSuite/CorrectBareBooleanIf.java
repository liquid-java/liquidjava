package testSuite;

import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// Regression for issue #371: preserve both outcomes of a boolean condition.
@StateSet({"open", "closed"})
public class CorrectBareBooleanIf {
    @StateRefinement(to = "open(this)")
    public CorrectBareBooleanIf() {}
    @StateRefinement(to = "open(this)")
    public void reopen() {}
    @StateRefinement(to = "closed(this)")
    public void close() {}
    @StateRefinement(from = "open(this)")
    public void use() {}

    @Refinement("_ == value")
    public static boolean identity(boolean value) { return value; }

    public static void bothPathsSafe(boolean again) {
        CorrectBareBooleanIf r = new CorrectBareBooleanIf();
        if (again) {
            r.reopen();
        }
        r.use();
    }

    public static void knownTrue() {
        CorrectBareBooleanIf r = new CorrectBareBooleanIf();
        r.close();
        boolean again = true;
        if (again) {
            r.reopen();
        }
        r.use();
    }

    public static void knownFalseCall() {
        CorrectBareBooleanIf r = new CorrectBareBooleanIf();
        if (identity(false)) {
            r.close();
        }
        r.use();
    }

    public static void negatedKnownFalse() {
        CorrectBareBooleanIf r = new CorrectBareBooleanIf();
        r.close();
        boolean again = false;
        if (!again) {
            r.reopen();
        }
        r.use();
    }

    public static void guardedUse(boolean again) {
        CorrectBareBooleanIf r = new CorrectBareBooleanIf();
        r.close();
        if (again) {
            r.reopen();
        }
        if (again) {
            r.use();
        }
    }

    public static void explicitElse(boolean again) {
        CorrectBareBooleanIf r = new CorrectBareBooleanIf();
        r.close();
        if (again) {
            r.reopen();
        } else {
            r.reopen();
        }
        r.use();
    }
}
