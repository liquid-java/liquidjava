package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// From any state, go() needs four calls (reset, a, b, c), more than a hint suggests
@StateSet({"s0", "s1", "s2", "s3"})
public class ErrorStateHintUnknownLongPath {

    @StateRefinement(to = "s0(this)")
    public ErrorStateHintUnknownLongPath() {}

    @StateRefinement(to = "s0(this)")
    public void reset() {}

    @StateRefinement(from = "s0(this)", to = "s1(this)")
    public void a() {}

    @StateRefinement(from = "s1(this)", to = "s2(this)")
    public void b() {}

    @StateRefinement(from = "s2(this)", to = "s3(this)")
    public void c() {}

    @StateRefinement(from = "s3(this)")
    public void go() {}

    public static void use(ErrorStateHintUnknownLongPath x) {
        x.go(); // Expect: State Refinement Error
    }
}
