package liquidjava.processor.state_machine;

import java.util.List;
import java.util.stream.Collectors;

import liquidjava.rj_language.Predicate;

/**
 * A state of a class's typestate: one state from each of its state sets
 *
 * @param names
 *            the name of the state in each state set, in the order the sets are declared
 * @param predicate
 *            the predicate that holds for {@code this} in this state
 */
public record State(List<String> names, Predicate predicate) {

    public State {
        names = List.copyOf(names);
    }

    /** The state names, e.g. {@code open} or {@code open and locked} for a class with two state sets */
    public String name() {
        return String.join(" and ", names);
    }

    /** The state names in the given state sets only, as indexes into {@link #names()} */
    public String name(List<Integer> stateSets) {
        return stateSets.stream().map(names::get).collect(Collectors.joining(" and "));
    }
}
