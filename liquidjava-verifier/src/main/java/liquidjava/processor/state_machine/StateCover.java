package liquidjava.processor.state_machine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Names a set of states as briefly as possible: as the fewest groups that each fix the state in some state sets and
 * allow any state in the others. For example, the states where {@code open(this) || loud(this)} holds are named
 * {@code open} or {@code loud}, not as every combination with the other state sets.
 */
final class StateCover {

    private static final int ANY = -1;

    /**
     * States that fix the state of some state sets and allow any state in the others
     *
     * @param values
     *            the index of the fixed state in each state set, or {@link #ANY}
     * @param members
     *            the states in the group
     */
    private record Group(int[] values, Set<State> members) {
    }

    private final List<State> states;
    private final List<List<String>> values;

    private StateCover(List<State> states) {
        this.states = states;
        int stateSets = states.get(0).names().size();
        this.values = IntStream.range(0, stateSets)
                .mapToObj(set -> states.stream().map(s -> s.names().get(set)).distinct().toList()).toList();
    }

    /**
     * @param states
     *            every state of the class, one for each combination of the states of its state sets
     * @param named
     *            the state sets every group must fix, as indexes into {@link State#names()}
     *
     * @return the groups, each given by the states it fixes, in the order the state sets are declared; if
     *         {@code subset} has every state and no set must be named, each state on its own
     */
    static List<List<String>> of(List<State> states, Set<State> subset, Set<Integer> named) {
        if (subset.isEmpty())
            return List.of();
        if (subset.size() == states.size() && named.isEmpty())
            return states.stream().map(State::names).toList();
        StateCover cover = new StateCover(states);
        return cover.cover(subset, named).stream().map(cover::names).toList();
    }

    /** The state sets that the groups naming {@code subset} fix, as indexes into {@link State#names()} */
    static Set<Integer> namedSets(List<State> states, Set<State> subset) {
        if (subset.isEmpty())
            return Set.of();
        return new StateCover(states).cover(subset, Set.of()).stream().flatMap(
                group -> IntStream.range(0, group.values().length).filter(set -> group.values()[set] != ANY).boxed())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /** The fewest of the largest groups inside {@code subset} that fix the sets {@code named} and hold all of it */
    private List<Group> cover(Set<State> subset, Set<Integer> named) {
        List<Group> candidates = largestGroupsWithin(subset, named);
        for (int size = 1;; size++) {
            List<Group> chosen = firstCover(candidates, subset, size, 0, new ArrayList<>());
            if (chosen != null)
                return chosen;
        }
    }

    /** The groups inside {@code subset} that fix the sets {@code named} and no larger such group holds */
    private List<Group> largestGroupsWithin(Set<State> subset, Set<Integer> named) {
        List<Group> within = new ArrayList<>();
        groups(new int[values.size()], 0, within, subset, named);
        return within.stream().filter(g -> within.stream().noneMatch(other -> other != g && contains(other, g)))
                .sorted(declarationOrder()).toList();
    }

    /** Whether {@code larger} fixes fewer state sets than {@code smaller}, to the same states */
    private static boolean contains(Group larger, Group smaller) {
        return !Arrays.equals(larger.values(), smaller.values()) && IntStream.range(0, larger.values().length)
                .allMatch(set -> larger.values()[set] == ANY || larger.values()[set] == smaller.values()[set]);
    }

    private void groups(int[] group, int set, List<Group> within, Set<State> subset, Set<Integer> named) {
        if (set == values.size()) {
            Set<State> members = members(group);
            if (subset.containsAll(members))
                within.add(new Group(group.clone(), members));
            return;
        }
        for (int value = named.contains(set) ? 0 : ANY; value < values.get(set).size(); value++) {
            group[set] = value;
            groups(group, set + 1, within, subset, named);
        }
    }

    /** The first {@code size} candidates from {@code next} on, in order, that hold all of {@code subset}, or null */
    private static List<Group> firstCover(List<Group> candidates, Set<State> subset, int size, int next,
            List<Group> chosen) {
        if (chosen.size() == size)
            return covers(chosen, subset) ? List.copyOf(chosen) : null;
        for (int i = next; i < candidates.size(); i++) {
            chosen.add(candidates.get(i));
            List<Group> found = firstCover(candidates, subset, size, i + 1, chosen);
            chosen.remove(chosen.size() - 1);
            if (found != null)
                return found;
        }
        return null;
    }

    private static boolean covers(List<Group> groups, Set<State> subset) {
        Set<State> covered = new HashSet<>();
        groups.forEach(g -> covered.addAll(g.members()));
        return covered.containsAll(subset);
    }

    private Set<State> members(int[] group) {
        return states.stream()
                .filter(s -> IntStream.range(0, group.length).allMatch(
                        set -> group[set] == ANY || values.get(set).get(group[set]).equals(s.names().get(set))))
                .collect(Collectors.toSet());
    }

    /** Groups fixing earlier state sets first, then by the order of the states within a set */
    private static Comparator<Group> declarationOrder() {
        return (a, b) -> {
            for (int set = 0; set < a.values().length; set++) {
                int x = a.values()[set] == ANY ? Integer.MAX_VALUE : a.values()[set];
                int y = b.values()[set] == ANY ? Integer.MAX_VALUE : b.values()[set];
                if (x != y)
                    return Integer.compare(x, y);
            }
            return 0;
        };
    }

    private List<String> names(Group group) {
        return IntStream.range(0, group.values().length).filter(set -> group.values()[set] != ANY)
                .mapToObj(set -> values.get(set).get(group.values()[set])).toList();
    }
}
