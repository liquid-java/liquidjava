package liquidjava.processor.refinement_checker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import liquidjava.diagnostics.errors.*;
import liquidjava.diagnostics.TranslationTable;
import liquidjava.processor.VCImplication;
import liquidjava.processor.context.*;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.Ite;
import liquidjava.rj_language.ast.Var;
import liquidjava.smt.Counterexample;
import liquidjava.smt.SMTEvaluator;
import liquidjava.smt.SMTResult;
import liquidjava.utils.constants.Keys;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;

public class VCChecker {
    private final Context context;
    private final List<RefinedVariable> pathVariables;

    public VCChecker() {
        context = Context.getInstance();
        pathVariables = new Stack<>();
    }

    public void processSubtyping(Predicate expectedType, List<GhostState> list, CtElement element, Factory f)
            throws LJError {
        processSubtyping(expectedType, list, element, f, null);
    }

    public void processSubtyping(Predicate expectedType, List<GhostState> list, CtElement element, Factory f,
            String customMessage) throws LJError {
        List<RefinedVariable> lrv = new ArrayList<>(), mainVars = new ArrayList<>();
        gatherVariables(expectedType, lrv, mainVars);
        if (expectedType.isBooleanTrue())
            return;

        TranslationTable map = new TranslationTable();
        String[] s = { Keys.WILDCARD, Keys.THIS };
        Predicate premisesBeforeChange = joinPredicates(expectedType, mainVars, lrv, map).toConjunctions();
        Predicate premises;
        Predicate expected;
        try {
            List<GhostState> filtered = filterGhostStatesForVariables(list, mainVars, lrv);
            premises = premisesBeforeChange.changeStatesToRefinements(filtered, s).changeAliasToRefinement(context, f);
            expected = expectedType.changeStatesToRefinements(filtered, s).changeAliasToRefinement(context, f);
        } catch (LJError e) {
            // add location info to error
            e.setPosition(element.getPosition());
            throw e;
        }
        SMTResult result = verifySMTSubtype(expected, premises, element.getPosition());
        if (result.isError()) {
            throw new RefinementError(element.getPosition(), expectedType.simplify(), premisesBeforeChange.simplify(),
                    map, result.getCounterexample(), customMessage);
        }
    }

    /**
     * Checks if type is a subtype of expectedType
     * 
     * @param type
     * @param expectedType
     * @param list
     * @param element
     * @param f
     * 
     * @throws LJError
     */
    public void processSubtyping(Predicate type, Predicate expectedType, List<GhostState> list, CtElement element,
            Factory f) throws LJError {
        SMTResult result = verifySMTSubtypeStates(type, expectedType, list, element.getPosition(), f);
        if (result.isError())
            throwRefinementError(element.getPosition(), expectedType, type, result.getCounterexample(), null);
    }

    /**
     * Verifies whether the found predicate is a subtype of the expected predicate
     * 
     * @param expected
     * @param found
     * @param position
     * 
     * @return the result of the verification, containing a counterexample if the verification fails
     */
    public SMTResult verifySMTSubtype(Predicate expected, Predicate found, SourcePosition position) throws LJError {
        try {
            return new SMTEvaluator().verifySubtype(found, expected, context);
        } catch (LJError e) {
            e.setPosition(position);
            throw e;
        } catch (Exception e) {
            throw new CustomError(e.getMessage(), position);
        }
    }

    /**
     * Verifies whether the found predicate is a subtype of the expected predicate, taking into account the ghost states
     * 
     * @param type
     * @param expectedType
     * @param states
     * @param position
     * @param factory
     * 
     * @return the result of the verification, containing a counterexample if the verification fails
     */
    public SMTResult verifySMTSubtypeStates(Predicate type, Predicate expectedType, List<GhostState> states,
            SourcePosition position, Factory factory) throws LJError {
        List<RefinedVariable> lrv = new ArrayList<>(), mainVars = new ArrayList<>();
        gatherVariables(expectedType, lrv, mainVars);
        gatherVariables(type, lrv, mainVars);
        if (expectedType.isBooleanTrue() && type.isBooleanTrue())
            return SMTResult.ok();

        TranslationTable map = new TranslationTable();
        String[] s = { Keys.WILDCARD, Keys.THIS };
        Predicate premises = joinPredicates(expectedType, mainVars, lrv, map).toConjunctions();
        List<GhostState> filtered = filterGhostStatesForVariables(states, mainVars, lrv);
        premises = Predicate.createConjunction(premises, type).changeStatesToRefinements(filtered, s)
                .changeAliasToRefinement(context, factory);
        Predicate expected = expectedType.changeStatesToRefinements(filtered, s).changeAliasToRefinement(context,
                factory);

        // check subtyping
        return verifySMTSubtype(expected, premises, position);
    }

    /**
     * Reduce the ghost states list to those whose declaring class (prefix) matches any of the involved variable types
     * or their supertypes This prevents ambiguous simple name substitutions across unrelated classes that share state
     * names
     */
    private List<GhostState> filterGhostStatesForVariables(List<GhostState> list, List<RefinedVariable> mainVars,
            List<RefinedVariable> vars) {
        if (list.isEmpty())
            return list;

        // Collect all relevant qualified type names (types + supertypes), keeping order and deduping
        Set<String> allowedPrefixes = new java.util.LinkedHashSet<>();
        Consumer<RefinedVariable> collect = rv -> {
            if (rv.getType() != null) {
                allowedPrefixes.add(rv.getType().getQualifiedName());
            }
            for (CtTypeReference<?> st : rv.getSuperTypes()) {
                if (st != null) {
                    allowedPrefixes.add(st.getQualifiedName());
                }
            }
        };
        mainVars.forEach(collect);
        vars.forEach(collect);

        if (allowedPrefixes.isEmpty())
            return list; // avoid over-filtering when types are unknown

        List<GhostState> filtered = list.stream().filter(g -> {
            String prefix = (g.getParent() != null) ? g.getParent().getPrefix() : g.getPrefix();
            return allowedPrefixes.contains(prefix);
        }).collect(Collectors.toList());

        // If nothing matched, keep original to avoid accidental empties
        return filtered.isEmpty() ? list : filtered;
    }

    private VCImplication joinPredicates(Predicate expectedType, List<RefinedVariable> mainVars,
            List<RefinedVariable> vars, TranslationTable map) {

        VCImplication firstSi = null;
        VCImplication lastSi = null;
        // Check
        for (RefinedVariable var : mainVars) { // join main refinements of mainVars
            addMap(var, map);
            VCImplication si = new VCImplication(var.getName(), var.getType(), var.getMainRefinement());
            if (lastSi != null) {
                lastSi.setNext(si);
                lastSi = si;
            }
            if (firstSi == null) {
                firstSi = si;
                lastSi = si;
            }
        }

        for (RefinedVariable var : vars) { // join refinements of vars
            addMap(var, map);

            // if the last instance is already in vars, it is already in the premises
            // adding "var == lastInstance" would create a contradictory cycle (e.g. x == x + 1 for x = x + 1)
            // so we need to use main refinement to avoid this
            Predicate refinement = var.getRefinement();
            if (var instanceof Variable v) {
                Optional<VariableInstance> lastInst = v.getLastInstance();
                if (lastInst.isPresent() && vars.contains(lastInst.get())
                        && hasDependencyCycle(lastInst.get(), var.getName(), vars, new HashSet<>()))
                    refinement = v.getMainRefinement();
            }
            VCImplication si = new VCImplication(var.getName(), var.getType(), refinement);
            if (lastSi != null) {
                lastSi.setNext(si);
                lastSi = si;
            }
            if (firstSi == null) {
                firstSi = si;
                lastSi = si;
            }
        }
        VCImplication cSMT = new VCImplication(new Predicate());
        if (firstSi != null) {
            cSMT = firstSi.clone();
            lastSi.setNext(new VCImplication(expectedType));
        }
        return cSMT;
    }

    private void addMap(RefinedVariable var, TranslationTable map) {
        map.put(var.getName(), var.getPlacementInCode());
    }

    private void gatherVariables(Predicate expectedType, List<RefinedVariable> lrv, List<RefinedVariable> mainVars) {
        for (String s : expectedType.getVariableNames()) {
            if (context.hasVariable(s)) {
                RefinedVariable rv = context.getVariableByName(s);
                if (!mainVars.contains(rv) && !lrv.contains(rv))
                    mainVars.add(rv);
                List<RefinedVariable> lm = getVariables(rv.getMainRefinement(), rv.getName());
                addAllDifferent(lrv, lm, mainVars);
            }
        }
    }

    private void addAllDifferent(List<RefinedVariable> toExpand, List<RefinedVariable> from,
            List<RefinedVariable> remove) {
        from.stream().filter(rv -> !toExpand.contains(rv) && !remove.contains(rv)).forEach(toExpand::add);
    }

    private List<RefinedVariable> getVariables(Predicate c, String varName) {
        List<RefinedVariable> allVars = new ArrayList<>();
        getVariablesFromContext(c.getVariableNames(), allVars, varName);
        List<String> pathNames = pathVariables.stream().map(Refined::getName).collect(Collectors.toList());
        getVariablesFromContext(pathNames, allVars, "");
        return allVars;
    }

    private void getVariablesFromContext(List<String> lvars, List<RefinedVariable> allVars, String notAdd) {
        lvars.stream().filter(name -> !name.equals(notAdd) && context.hasVariable(name)).map(context::getVariableByName)
                .filter(rv -> !allVars.contains(rv)).forEach(rv -> {
                    allVars.add(rv);
                    recAuxGetVars(rv, allVars);
                });
    }

    private void recAuxGetVars(RefinedVariable var, List<RefinedVariable> newVars) {
        if (!context.hasVariable(var.getName()))
            return;
        Predicate c = var.getRefinement();
        String varName = var.getName();
        List<String> l = c.getVariableNames();
        getVariablesFromContext(l, newVars, varName);
    }

    public void addPathVariable(RefinedVariable rv) {
        pathVariables.add(rv);
    }

    public void removePathVariable(RefinedVariable rv) {
        pathVariables.remove(rv);
    }

    void removePathVariableThatIncludes(String otherVar) {
        pathVariables.stream().filter(rv -> rv.getRefinement().getVariableNames().contains(otherVar)).toList()
                .forEach(pathVariables::remove);
    }

    private boolean hasDependencyCycle(RefinedVariable rv, String var, List<RefinedVariable> vars, Set<String> seen) {
        if (!seen.add(rv.getName()))
            return false;
        Expression e = rv.getRefinement().getExpression();
        return hasVariable(e, var) || vars.stream().filter(o -> hasVariable(e, o.getName()))
                .anyMatch(o -> hasDependencyCycle(o, var, vars, seen));
    }

    private boolean hasVariable(Expression exp, String var) {
        if (exp instanceof Var v)
            return v.getName().equals(var);
        if (exp instanceof Ite ite)
            return hasVariable(ite.getThen(), var) || hasVariable(ite.getElse(), var);
        return exp.getChildren().stream().anyMatch(c -> hasVariable(c, var));
    }

    // Errors---------------------------------------------------------------------------------------------------

    protected void throwRefinementError(SourcePosition position, Predicate expected, Predicate found,
            Counterexample counterexample, String customMessage) throws RefinementError {
        List<RefinedVariable> lrv = new ArrayList<>(), mainVars = new ArrayList<>();
        gatherVariables(expected, lrv, mainVars);
        gatherVariables(found, lrv, mainVars);
        TranslationTable map = new TranslationTable();
        Predicate premises = joinPredicates(expected, mainVars, lrv, map).toConjunctions();
        throw new RefinementError(position, expected.simplify(), premises.simplify(), map, counterexample,
                customMessage);
    }

    protected void throwStateRefinementError(SourcePosition position, Predicate found, Predicate expected,
            String customMessage) throws StateRefinementError {
        List<RefinedVariable> lrv = new ArrayList<>(), mainVars = new ArrayList<>();
        gatherVariables(found, lrv, mainVars);
        TranslationTable map = new TranslationTable();
        VCImplication foundState = joinPredicates(found, mainVars, lrv, map);
        throw new StateRefinementError(position, expected.getExpression(),
                foundState.toConjunctions().simplify().getValue(), map, customMessage);
    }

    protected void throwStateConflictError(SourcePosition position, Predicate expected) throws StateConflictError {
        TranslationTable map = createMap(expected);
        throw new StateConflictError(position, expected.getExpression(), map);
    }

    private TranslationTable createMap(Predicate expectedType) {
        List<RefinedVariable> lrv = new ArrayList<>(), mainVars = new ArrayList<>();
        gatherVariables(expectedType, lrv, mainVars);
        TranslationTable map = new TranslationTable();
        joinPredicates(expectedType, mainVars, lrv, map);
        return map;
    }
}
