package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// Found folds the branches the path condition rules out: under next == BORDERLESS only the second if can have run
@StateSet(name = "display", value = {"hidden", "shown"})
class FoldFrame {
    @StateRefinement(to = "hidden(this)")
    FoldFrame() {}

    @StateRefinement(from = "hidden(this)", to = "shown(this)")
    void show() {}

    @StateRefinement(from = "shown(this)", to = "hidden(this)")
    void hide() {}

    @StateRefinement(from = "hidden(this)")
    void decorate() {}
}

enum FoldMode {
    WINDOWED, BORDERLESS
}

public class ErrorStatePathFolding {
    static void apply(FoldMode previous, FoldMode next) {
        FoldFrame frame = new FoldFrame();
        frame.show();
        if (previous == FoldMode.BORDERLESS && next != FoldMode.BORDERLESS)
            frame.hide();
        else if (previous != FoldMode.BORDERLESS && next == FoldMode.BORDERLESS)
            frame.hide();
        switch (next) {
        case BORDERLESS:
            frame.decorate(); // Expect: State Refinement Error
            break;
        default:
            break;
        }
    }
}
