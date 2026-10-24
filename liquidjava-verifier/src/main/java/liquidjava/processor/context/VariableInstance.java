package liquidjava.processor.context;

import java.util.Optional;
import liquidjava.rj_language.Predicate;
import spoon.reflect.reference.CtTypeReference;

public class VariableInstance extends RefinedVariable {

    private Variable parent;
    private StateChange stateChange;

    public VariableInstance(String name, CtTypeReference<?> type, Predicate c) {
        super(name, type, c);
        this.parent = null;
    }

    public VariableInstance(String name, CtTypeReference<?> type, Predicate c, Variable parent) {
        super(name, type, c);
        this.parent = parent;
    }

    @Override
    public Predicate getMainRefinement() {
        return super.getRefinement();
    }

    @Override
    public String toString() {
        return "VariableInstance [name=" + super.getName() + ", type=" + super.getType() + ", refinement="
                + super.getRefinement() + "]";
    }

    public void setParent(Variable p) {
        parent = p;
    }

    /** The last call that changed the state of this object, if it is known */
    public Optional<StateChange> getStateChange() {
        return Optional.ofNullable(stateChange);
    }

    public void setStateChange(StateChange stateChange) {
        this.stateChange = stateChange;
    }

    public Optional<Variable> getParent() {
        return parent == null ? Optional.empty() : Optional.of(parent);
    }
}
