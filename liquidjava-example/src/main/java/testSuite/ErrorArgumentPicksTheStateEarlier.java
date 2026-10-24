package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"tilingOff", "tilingOn"})
@StateSet({"uncompressed", "compressed"})
public class ErrorArgumentPicksTheStateEarlier {

    @StateRefinement(to = "tilingOff(this) && uncompressed(this)")
    public ErrorArgumentPicksTheStateEarlier() {}

    @StateRefinement(to = "mode == 2 ? tilingOn(this) : tilingOff(this)")
    public void setTilingMode(int mode) {}

    @StateRefinement(from = "uncompressed(this)", to = "compressed(this)")
    public void compress() {}

    @StateRefinement(from = "tilingOn(this)")
    public void setTiling() {}

    // compress() changes another state set, so the tiling mode is what decided the state
    public static void use() {
        ErrorArgumentPicksTheStateEarlier param = new ErrorArgumentPicksTheStateEarlier();
        param.setTilingMode(1);
        param.compress();
        param.setTiling(); // Expect: State Refinement Error
    }
}
