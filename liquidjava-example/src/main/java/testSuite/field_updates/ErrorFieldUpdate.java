// State Refinement Error
package testSuite.field_updates;

import liquidjava.specification.StateRefinement;

public class ErrorFieldUpdate {
    public int n;

    @StateRefinement(from = "n(this) > 0", to = "n(this) == n(old(this))")
    public void shouldFailIfFieldIsNegative() {}

    public static void main(String[] args) {

        ErrorFieldUpdate t = new ErrorFieldUpdate();
        t.n = -1;
        t.shouldFailIfFieldIsNegative();
    }
}
