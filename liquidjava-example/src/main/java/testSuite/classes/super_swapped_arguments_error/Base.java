package testSuite.classes.super_swapped_arguments_error;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"open", "closed"})
public class Base {
    @StateRefinement(to = "(x && !y && open(this)) || (!(x && !y) && closed(this))")
    public Base(boolean x, boolean y) {}

    @StateRefinement(from = "open(this)", to = "closed(this)")
    public void close() {}
}
