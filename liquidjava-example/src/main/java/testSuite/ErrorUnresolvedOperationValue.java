package testSuite;

import org.example.missing.Item;

import liquidjava.specification.Refinement;

// An operation whose operand types cannot be resolved has an unknown value (#388)
@SuppressWarnings("unused")
public class ErrorUnresolvedOperationValue {
    static void unknownValue(Item x) {
        @Refinement("_ > 0")
        int y = x.a() + 1; // Expect: Refinement Error
    }
}
