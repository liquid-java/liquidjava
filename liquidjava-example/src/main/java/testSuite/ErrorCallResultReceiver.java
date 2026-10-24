package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"open", "closed"})
class ErrorCallResultDoor {
    @StateRefinement(to = "open(this)")
    public ErrorCallResultDoor() {}

    @StateRefinement(from = "open(this)", to = "closed(this)")
    public void close() {}
}

class ErrorCallResultHolder {
    private final ErrorCallResultDoor door = new ErrorCallResultDoor();

    ErrorCallResultDoor get() {
        return door;
    }
}

public class ErrorCallResultReceiver {
    static void use(ErrorCallResultHolder holder) {
        holder.get().close(); // Expect: State Refinement Error
    }
}
