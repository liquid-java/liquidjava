package testSuite.classes.state_return_type_scope_correct;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"start", "canEdit"})
@ExternalRefinementsFor("javax.swing.undo.UndoManager")
public interface BUndoManagerRefinements {
    @StateRefinement(to = "start(this)")
    public void UndoManager();

    @StateRefinement(from = "start(this)", to = "!return && start(this)")
    public boolean canUndo();
}
