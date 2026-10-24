package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"readOnly", "writable"})
public class ErrorConstructorArgumentPicksTheState {

    @StateRefinement(to = "write ? writable(this) : readOnly(this)")
    public ErrorConstructorArgumentPicksTheState(boolean write) {}

    @StateRefinement(from = "writable(this)")
    public void append() {}

    public static void use() {
        ErrorConstructorArgumentPicksTheState file = new ErrorConstructorArgumentPicksTheState(false);
        file.append(); // Expect: State Refinement Error
    }
}
