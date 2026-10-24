package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"open", "closed"})
class CorrectCallResultDoor {
    @StateRefinement(to = "open(this)")
    public CorrectCallResultDoor() {}

    @StateRefinement(from = "open(this)", to = "closed(this)")
    public void close() {}

    @StateRefinement(to = "closed(this)")
    public void slam() {}
}

class CorrectCallResultHolder {
    private final CorrectCallResultDoor door = new CorrectCallResultDoor();

    CorrectCallResultDoor get() {
        return door;
    }
}

public class CorrectCallResultReceiver {
    static void use(CorrectCallResultHolder holder) {
        holder.get().slam();
    }
}
