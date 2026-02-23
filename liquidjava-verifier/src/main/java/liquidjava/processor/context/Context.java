package liquidjava.processor.context;

import java.util.*;
import liquidjava.rj_language.Predicate;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtTypeReference;

public class Context {
    private Stack<List<RefinedVariable>> ctxVars;
    private List<RefinedFunction> ctxFunctions;
    private List<RefinedVariable> ctxInstanceVars;
    private final List<RefinedVariable> ctxGlobalVars;

    private List<GhostFunction> ghosts;
    private Map<String, List<GhostState>> classStates;
    private List<AliasWrapper> aliases;

    private int counter;
    private static Context instance;

    private Context() {
        ctxVars = new Stack<>();
        ctxVars.add(new ArrayList<>());
        ctxFunctions = new ArrayList<>();
        ctxInstanceVars = new ArrayList<>();
        ctxGlobalVars = new ArrayList<>();

        aliases = new ArrayList<>();
        ghosts = new ArrayList<>();
        classStates = new HashMap<>();
        counter = 0;
    }

    public static Context getInstance() {
        if (instance == null)
            instance = new Context();
        return instance;
    }

    public void reinitializeContext() {
        ctxVars = new Stack<>();
        ctxVars.add(new ArrayList<>()); // global vars
        ctxInstanceVars = new ArrayList<>();
    }

    public void reinitializeAllContext() {
        reinitializeContext();
        ctxFunctions = new ArrayList<>();
        aliases = new ArrayList<>();
        ghosts = new ArrayList<>();
        classStates = new HashMap<>();
        counter = 0;
    }

    public void enterContext() {
        ctxVars.push(new ArrayList<>());
        // make each variable enter context
        for (RefinedVariable vi : getAllVariables())
            if (vi instanceof Variable)
                ((Variable) vi).enterContext();
    }

    public void exitContext() {
        ctxVars.pop();
        // make each variable exit context
        for (RefinedVariable vi : getAllVariables())
            if (vi instanceof Variable)
                ((Variable) vi).exitContext();
    }

    public int getCounter() {
        return counter++;
    }

    public Map<String, CtTypeReference<?>> getContext() {
        Map<String, CtTypeReference<?>> ret = new HashMap<>();
        for (List<RefinedVariable> l : ctxVars) {
            for (RefinedVariable var : l) {
                ret.put(var.getName(), var.getType());
            }
        }
        for (RefinedVariable var : ctxInstanceVars)
            ret.put(var.getName(), var.getType());
        for (RefinedVariable var : ctxGlobalVars)
            ret.put(var.getName(), var.getType());
        return ret;
    }

    // ---------------------- Global variables ----------------------
    public void addGlobalVariableToContext(String simpleName, String location, CtTypeReference<?> type, Predicate c) {
        RefinedVariable vi = new Variable(simpleName, location, type, c);
        vi.addSuperTypes(type.getSuperclass(), type.getSuperInterfaces());
        ctxGlobalVars.add(vi);
    }

    public List<RefinedVariable> getCtxGlobalVars() {
        return ctxGlobalVars;
    }

    // ---------------------- Add variables and instances ----------------------
    public void addVarToContext(RefinedVariable var) {
        ctxVars.peek().add(var);
        CtTypeReference<?> type = var.getType();
        var.addSuperTypes(type.getSuperclass(), type.getSuperInterfaces());
    }

    public RefinedVariable addVarToContext(String simpleName, CtTypeReference<?> type, Predicate c,
            CtElement placementInCode) {
        RefinedVariable vi = new Variable(simpleName, type, c);
        vi.addPlacementInCode(PlacementInCode.createPlacement(placementInCode));
        vi.addSuperTypes(type.getSuperclass(), type.getSuperInterfaces());
        addVarToContext(vi);
        return vi;
    }

    public RefinedVariable addInstanceToContext(String simpleName, CtTypeReference<?> type, Predicate c,
            CtElement placementInCode) {
        RefinedVariable vi = new VariableInstance(simpleName, type, c);
        vi.addPlacementInCode(PlacementInCode.createPlacement(placementInCode));
        if (!ctxInstanceVars.contains(vi))
            addInstanceVariable(vi);
        return vi;
    }

    public void addRefinementToVariableInContext(String name, CtTypeReference<?> type, Predicate et,
            CtElement placementInCode) {
        if (hasVariable(name)) {
            RefinedVariable vi = getVariableByName(name);
            vi.setRefinement(et);
            vi.addPlacementInCode(PlacementInCode.createPlacement(placementInCode));
        } else {
            addVarToContext(name, type, et, placementInCode);
        }
    }

    /**
     * The variable with name variableName will have a new refinement
     *
     * @param variableName
     * @param expectedType
     */
    public void newRefinementToVariableInContext(String variableName, Predicate expectedType) {
        if (hasVariable(variableName)) {
            RefinedVariable vi = getVariableByName(variableName);
            vi.setRefinement(expectedType);
        }
    }

    public Predicate getVariableRefinements(String varName) {
        return hasVariable(varName) ? getVariableByName(varName).getRefinement() : null;
    }

    public RefinedVariable getVariableByName(String name) {
        for (List<RefinedVariable> l : ctxVars) {
            for (RefinedVariable var : l) {
                if (var.getName().equals(name))
                    return var;
            }
        }
        for (RefinedVariable var : ctxInstanceVars) {
            if (var.getName().equals(name))
                return var;
        }
        for (RefinedVariable var : ctxGlobalVars) {
            if (var.getName().equals(name))
                return var;
        }
        return null;
    }

    public boolean hasVariable(String name) {
        return getVariableByName(name) != null;
    }

    /**
     * Lists all variables inside the stack
     *
     * @return list of all variables
     */
    public List<RefinedVariable> getAllVariables() {
        List<RefinedVariable> lvi = new ArrayList<>();
        for (List<RefinedVariable> l : ctxVars) {
            lvi.addAll(l);
        }
        return lvi;
    }

    public List<RefinedVariable> getAllVariablesWithSupertypes() {
        List<RefinedVariable> lvi = new ArrayList<>();
        for (RefinedVariable rv : getAllVariables()) {
            if (!rv.getSuperTypes().isEmpty())
                lvi.add(rv);
        }
        for (RefinedVariable rv : ctxInstanceVars) {
            if (!rv.getSuperTypes().isEmpty())
                lvi.add(rv);
        }
        return lvi;
    }

    public void addRefinementInstanceToVariable(String name, String instanceName) {
        RefinedVariable vi1 = getVariableByName(name);
        RefinedVariable vi2 = getVariableByName(instanceName);
        if (!hasVariable(name) || !hasVariable(instanceName)
                || !(vi1 instanceof Variable && vi2 instanceof VariableInstance))
            return;

        ((Variable) vi1).addInstance((VariableInstance) vi2);
        ((VariableInstance) vi2).setParent((Variable) vi1);
        addInstanceVariable(vi2);
    }

    public Optional<VariableInstance> getLastVariableInstance(String name) {
        RefinedVariable rv = getVariableByName(name);
        if (!hasVariable(name) || !(rv instanceof Variable))
            return Optional.empty();
        return ((Variable) rv).getLastInstance();
    }

    public void addInstanceVariable(RefinedVariable vi) {
        if (!ctxInstanceVars.contains(vi)) {
            ctxInstanceVars.add(vi);
            CtTypeReference<?> type = vi.getType();
            vi.addSuperTypes(type.getSuperclass(), type.getSuperInterfaces());
        }
    }

    public Variable getVariableFromInstance(VariableInstance vi) {
        return vi.getParent().orElse(null);
    }

    public List<RefinedVariable> getCtxVars() {
        List<RefinedVariable> lvi = new ArrayList<>();
        for (List<RefinedVariable> l : ctxVars) {
            lvi.addAll(l);
        }
        return lvi;
    }

    public List<RefinedVariable> getCtxInstanceVars() {
        return ctxInstanceVars;
    }

    // ---------------------- Variables - if information storing ----------------------
    public void variablesSetBeforeIf() {
        for (RefinedVariable vi : getAllVariables())
            if (vi instanceof Variable)
                ((Variable) vi).saveInstanceBeforeIf();
    }

    public void variablesSetThenIf() {
        for (RefinedVariable vi : getAllVariables())
            if (vi instanceof Variable)
                ((Variable) vi).saveInstanceThen();
    }

    public void variablesSetElseIf() {
        for (RefinedVariable vi : getAllVariables())
            if (vi instanceof Variable)
                ((Variable) vi).saveInstanceElse();
    }

    public void variablesNewIfCombination() {
        for (RefinedVariable vi : getAllVariables())
            if (vi instanceof Variable)
                ((Variable) vi).newIfCombination();
    }

    public void variablesFinishIfCombination() {
        for (RefinedVariable vi : getAllVariables())
            if (vi instanceof Variable)
                ((Variable) vi).finishIfCombination();
    }

    public void variablesCombineFromIf(Predicate cond) {
        for (RefinedVariable vi : getAllVariables()) {
            if (vi instanceof Variable) {
                Optional<VariableInstance> ovi = ((Variable) vi).getIfInstanceCombination(getCounter(), cond);
                if (ovi.isPresent()) {
                    RefinedVariable vii = ovi.get();
                    addVarToContext(vii);
                    addRefinementInstanceToVariable(vi.getName(), vii.getName());
                }
            }
        }
    }

    // ---------------------- Functions ----------------------
    public void addFunctionToContext(RefinedFunction f) {
        if (!ctxFunctions.contains(f))
            ctxFunctions.add(f);
    }

    public RefinedFunction getFunction(String name, String target, int size) {
        for (RefinedFunction fi : ctxFunctions) {
            if (fi.getTargetClass() != null && fi.getName().equals(name) && fi.getTargetClass().equals(target)
                    && fi.getArguments().size() == size)
                return fi;
        }
        return null;
    }

    public List<RefinedFunction> getAllMethodsWithNameSize(String name, int size) {
        List<RefinedFunction> l = new ArrayList<>();
        for (RefinedFunction fi : ctxFunctions) {
            if (fi.getName().equals(name) && fi.getArguments().size() == size)
                l.add(fi);
        }
        return l;
    }

    public List<RefinedFunction> getCtxFunctions() {
        return ctxFunctions;
    }

    // ---------------------- Ghost Predicates ----------------------
    public void addGhostFunction(GhostFunction gh) {
        ghosts.add(gh);
    }

    public List<GhostFunction> getGhosts() {
        return ghosts;
    }

    public void addGhostClass(String klass) {
        if (!classStates.containsKey(klass))
            classStates.put(klass, new ArrayList<>());
    }

    public void addToGhostClass(String klass, GhostState gs) {
        List<GhostState> l = classStates.get(klass);
        if (!l.contains(gs))
            l.add(gs);
    }

    public List<GhostState> getGhostState(String s) {
        return classStates.get(s);
    }

    public List<GhostState> getGhostStates() {
        List<GhostState> lgs = new ArrayList<>();
        for (List<GhostState> l : classStates.values())
            lgs.addAll(l);
        return lgs;
    }

    // ---------------------- Alias ----------------------
    public void addAlias(AliasWrapper aw) {
        if (!aliases.contains(aw))
            aliases.add(aw);
    }

    public List<AliasWrapper> getAliases() {
        return aliases;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n############Global Variables:############\n");
        for (RefinedVariable f : ctxGlobalVars)
            sb.append(f.toString());
        sb.append("\n###########Variables############");
        for (List<RefinedVariable> l : ctxVars) {
            sb.append("{");
            for (RefinedVariable var : l) {
                sb.append(var.toString()).append("; ");
            }
            sb.append("}\n");
        }
        sb.append("\n############Functions:############\n");
        for (RefinedFunction f : ctxFunctions)
            sb.append(f.toString());

        sb.append("\n############Ghost Functions:############\n");
        for (GhostFunction f : ghosts)
            sb.append(f.toString());
        return sb.toString();
    }
}
