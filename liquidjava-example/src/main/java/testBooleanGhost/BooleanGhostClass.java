package testBooleanGhost;

import liquidjava.specification.Ghost;
import liquidjava.specification.StateRefinement;

@Ghost("boolean opened")
@Ghost("boolean closed")
public class BooleanGhostClass {

	boolean opened;
	boolean closed;

	@StateRefinement(from = "!opened(this) && !closed(this)", to = "opened(this) && !closed(this)")
	void open() {
		opened = true;
	}

	@StateRefinement(from = "opened(this) && !closed(this)")
	void execute() {
		// System.out.println("opened:" + open + "\nclosed::" + closed); // lança
		// exceção

		System.out.println("opened: ");
		System.out.println(opened);
		System.out.println("\nclosed: ");
		System.out.println(closed);

	}

	@StateRefinement(from = "opened(this) && !closed(this)", to = "opened(this) && closed(this)")
	void close() {
		closed = true;
	}

	@StateRefinement(from = "opened(this) && closed(this)")
	void terminate() {
		System.out.println("Terminating\n");
	}
}
