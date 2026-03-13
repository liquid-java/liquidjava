package liquidjava.smt;

import java.util.List;

import liquidjava.utils.Pair;

public record Counterexample(List<Pair<String, String>> assignments) {
}
