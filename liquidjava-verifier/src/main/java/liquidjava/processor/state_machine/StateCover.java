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

    /**
     * Above this many candidate groups, the smallest cover is approximated instead of searched for. With several state
     * sets and the machine's limit of 16 states there are at most 13; with one state set the groups are single states,
     * which the approximation covers exactly.
     */
    private static final int MAX_EXACT = 13;
    private static final int ANY = -1;

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
    static Set<Integer> fixedSets(List<State> states, Set<State> subset) {
        if (subset.isEmpty())
            return Set.of();
        StateCover cover = new StateCover(states);
        return cover.cover(subset, Set.of()).stream()
                .flatMap(group -> IntStream.range(0, group.length).filter(set -> group[set] != ANY).boxed())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private List<int[]> cover(Set<State> subset, Set<Integer> named) {
        return smallestCover(largestGroupsWithin(subset, named), subset);
    }

    /**
     * The groups of states inside {@code subset} that fix the sets {@code named} and that no larger such group
     * contains, in declaration order
     */
    private List<int[]> largestGroupsWithin(Set<State> subset, Set<Integer> named) {
        List<int[]> within = new ArrayList<>();
        groups(new int[values.size()], 0, within, subset, named);
        return within.stream().filter(g -> within.stream().noneMatch(other -> other != g && contains(other, g)))
                .sorted(declarationOrder()).toList();
    }

    private void groups(int[] group, int set, List<int[]> within, Set<State> subset, Set<Integer> named) {
        if (set == values.size()) {
            if (members(group).stream().allMatch(subset::contains))
                within.add(group.clone());
            return;
        }
        if (named.contains(set)) {
            for (int value = 0; value < values.get(set).size(); value++) {
                group[set] = value;
                groups(group, set + 1, within, subset, named);
            }
            return;
        }
        for (int value = ANY; value < values.get(set).size(); value++) {
            group[set] = value;
            groups(group, set + 1, within, subset, named);
        }
    }

    /** The fewest of {@code candidates} that together hold every state of {@code subset} */
    private List<int[]> smallestCover(List<int[]> candidates, Set<State> subset) {
        if (candidates.size() <= MAX_EXACT) {
            for (int size = 1; size <= candidates.size(); size++) {
                List<int[]> chosen = firstCover(candidates, subset, size, 0, new ArrayList<>());
                if (chosen != null)
                    return chosen;
            }
        }
        List<int[]> chosen = new ArrayList<>();
        Set<State> left = new HashSet<>(subset);
        while (!left.isEmpty()) {
            int[] best = candidates.stream()
                    .max(Comparator.comparingLong(g -> members(g).stream().filter(left::contains).count())).get();
            chosen.add(best);
            members(best).forEach(left::remove);
        }
        return chosen.stream().sorted(declarationOrder()).toList();
    }

    /** The first {@code size} candidates from {@code next} on, in order, that cover {@code subset}, or null */
    private List<int[]> firstCover(List<int[]> candidates, Set<State> subset, int size, int next, List<int[]> chosen) {
        if (chosen.size() == size)
            return covers(chosen, subset) ? List.copyOf(chosen) : null;
        for (int i = next; i < candidates.size(); i++) {
            chosen.add(candidates.get(i));
            List<int[]> found = firstCover(candidates, subset, size, i + 1, chosen);
            chosen.remove(chosen.size() - 1);
            if (found != null)
                return found;
        }
        return null;
    }

    private boolean covers(List<int[]> groups, Set<State> subset) {
        Set<State> covered = new HashSet<>();
        groups.forEach(g -> covered.addAll(members(g)));
        return covered.containsAll(subset);
    }

    private List<State> members(int[] group) {
        return states.stream()
                .filter(s -> IntStream.range(0, group.length).allMatch(
                        set -> group[set] == ANY || values.get(set).get(group[set]).equals(s.names().get(set))))
                .toList();
    }

    /** Whether {@code larger} holds every state of {@code smaller} */
    private static boolean contains(int[] larger, int[] smaller) {
        return !Arrays.equals(larger, smaller)
                && IntStream.range(0, larger.length).allMatch(set -> larger[set] == ANY || larger[set] == smaller[set]);
    }

    /** Groups fixing earlier state sets first, then by the order of the states within a set */
    private static Comparator<int[]> declarationOrder() {
        return (a, b) -> {
            for (int set = 0; set < a.length; set++) {
                int x = a[set] == ANY ? Integer.MAX_VALUE : a[set];
                int y = b[set] == ANY ? Integer.MAX_VALUE : b[set];
                if (x != y)
                    return Integer.compare(x, y);
            }
            return 0;
        };
    }

    private List<String> names(int[] group) {
        return IntStream.range(0, group.length).filter(set -> group[set] != ANY)
                .mapToObj(set -> values.get(set).get(group[set])).toList();
    }
}
