package liquidjava.processor.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import liquidjava.rj_language.Predicate;
import spoon.reflect.reference.CtTypeReference;

public class Variable extends RefinedVariable {
    // Specific Values
    private Stack<List<VariableInstance>> instances;
    private String location;

    // To combine if values
    private static final int ifbeforeIndex = 0;
    private static final int ifthenIndex = 1;
    private static final int ifelseIndex = 2;

    private Stack<Object[]> ifCombiner; // Optional<VariableInstance>

    public Variable(String name, CtTypeReference<?> type, Predicate ref) {
        super(name, type, ref);
        startVariables();
    }

    public Variable(String name, String location, CtTypeReference<?> type, Predicate ref) {
        super(name, type, ref);
        this.location = location;
        startVariables();
    }

    private void startVariables() {
        this.instances = new Stack<>();
        this.instances.push(new ArrayList<>());
        ifCombiner = new Stack<>();
    }

    public Predicate getRefinement() {
        Predicate c = super.getRefinement();
        Optional<VariableInstance> ovi = getLastInstance();
        if (ovi.isPresent()) {
            VariableInstance vi = ovi.get();
            Predicate n = Predicate.createVar(this.getName());
            Predicate n2 = Predicate.createVar(vi.getName());
            c = Predicate.createConjunction(Predicate.createEquals(n, n2), c);
        }
        return c;
    }

    public Predicate getMainRefinement() {
        return super.getRefinement();
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Optional<String> getLocation() {
        return location == null ? Optional.empty() : Optional.of(location);
    }

    // INSTANCES
    public void enterContext() {
        instances.push(new ArrayList<>());
    }

    public void exitContext() {
        instances.pop();
    }

    public void addInstance(VariableInstance vi) {
        instances.peek().add(vi);
    }

    public Optional<VariableInstance> getLastInstance() {
        Stack<List<VariableInstance>> backup = new Stack<>();
        while (!instances.isEmpty()) {
            List<VariableInstance> lvi = instances.peek();
            if (!lvi.isEmpty()) { // last list in stack has a value
                reloadFromBackup(backup);
                return Optional.of(lvi.get(lvi.size() - 1));
            } else {
                backup.add(instances.pop());
            }
        }
        reloadFromBackup(backup);
        return Optional.empty();
    }

    private void reloadFromBackup(Stack<List<VariableInstance>> backup) {
        while (!backup.isEmpty())
            instances.add(backup.pop());
    }

    // IFS
    public void newIfCombination() {
        ifCombiner.push(new Object[ifelseIndex + 1]);
    }

    public void finishIfCombination() {
        ifCombiner.pop();
    }

    void saveInstanceBeforeIf() {
        if (!ifCombiner.isEmpty() && getLastInstance().isPresent())
            ifCombiner.peek()[ifbeforeIndex] = getLastInstance().get();
    }

    void saveInstanceThen() {
        if (!ifCombiner.isEmpty() && getLastInstance().isPresent())
            ifCombiner.peek()[ifthenIndex] = getLastInstance().get();
    }

    void saveInstanceElse() {
        if (!ifCombiner.isEmpty() && getLastInstance().isPresent())
            ifCombiner.peek()[ifelseIndex] = getLastInstance().get();
    }

    /**
     * Creates a combination of the values introduced during the if then else The values depend on the calls to
     * saveInstanceBeforeIf, saveInstanceThen and saveInstanceElse
     *
     * @param counter
     *            A number to create a new variable name
     * @param cond
     *
     * @return A new VariableInfo created by the combination of refinements or an empty Optional
     */
    Optional<VariableInstance> getIfInstanceCombination(int counter, Predicate cond) {
        if (ifCombiner.isEmpty() || (!has(ifthenIndex) && !has(ifelseIndex) && !has(ifbeforeIndex)))
            return Optional.empty();

        String nName = String.format("#%s_%d", super.getName(), counter);
        Predicate ref = new Predicate();

        if (!has(ifelseIndex)) {
            if (has(ifbeforeIndex) && has(ifthenIndex)) // value before if and inside then
                ref = createITEConstraint(nName, cond, get(ifthenIndex), get(ifbeforeIndex));
            else if (!has(ifbeforeIndex)) // only value inside then
                ref = createITEConstraint(nName, cond, get(ifthenIndex));
        } else {
            if (has(ifthenIndex)) // value in then and in else
                ref = createITEConstraint(nName, cond, get(ifthenIndex), get(ifelseIndex));
            else if (has(ifbeforeIndex)) // value before and in else
                ref = createITEConstraint(nName, cond, get(ifbeforeIndex), get(ifelseIndex));
            else
                ref = createITEConstraint(nName, cond.negate(), get(ifelseIndex));
        }
        VariableInstance jointReturn = new VariableInstance(nName, super.getType(), ref, this);
        jointReturn.setPlacementInScope(getPlacementInCode());
        return Optional.of(jointReturn);
    }

    private boolean has(int index) {
        Object o = ifCombiner.peek()[index];
        return (o instanceof VariableInstance);
    }

    private VariableInstance get(int index) {
        return (VariableInstance) ifCombiner.peek()[index];
    }

    /**
     * Creates an ITE where the else is true
     *
     * @param nName
     * @param cond
     * @param then
     *
     * @return
     */
    private Predicate createITEConstraint(String nName, Predicate cond, VariableInstance then) {
        Predicate ref1 = then.getRenamedRefinements(nName);
        return Predicate.createITE(cond, ref1, new Predicate());
    }

    private Predicate createITEConstraint(String nName, Predicate cond, VariableInstance then, VariableInstance els) {
        Predicate ref1 = then.getRenamedRefinements(nName);
        Predicate ref2 = els.getRenamedRefinements(nName);
        return Predicate.createITE(cond, ref1, ref2);
    }

    public String toString() {
        return "Variable [name=" + super.getName() + ", type=" + super.getType() + ", refinement="
                + super.getRefinement() + "]";
    }
}
