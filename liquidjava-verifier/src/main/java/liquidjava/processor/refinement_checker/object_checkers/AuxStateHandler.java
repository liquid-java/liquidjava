package liquidjava.processor.refinement_checker.object_checkers;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

import liquidjava.diagnostics.errors.IllegalConstructorTransitionError;
import liquidjava.diagnostics.errors.InvalidRefinementError;
import liquidjava.diagnostics.errors.LJError;
import liquidjava.processor.context.*;
import liquidjava.processor.refinement_checker.TypeChecker;
import liquidjava.processor.refinement_checker.TypeCheckingUtils;
import liquidjava.rj_language.Predicate;
import liquidjava.utils.Utils;
import liquidjava.utils.constants.Formats;
import liquidjava.utils.constants.Keys;
import liquidjava.utils.constants.Types;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;

public class AuxStateHandler {

    // ########### Get State from StateRefinement declaration #############

    /**
     * Handles the passage of the written state annotations to the context for Constructors
     *
     * @param c
     * @param f
     * @param tc
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void handleConstructorState(CtConstructor<?> c, RefinedFunction f, TypeChecker tc) throws LJError {
        List<CtAnnotation<? extends Annotation>> an = getStateAnnotation(c);
        if (!an.isEmpty()) {
            for (CtAnnotation<? extends Annotation> a : an) {
                Map<String, CtExpression> m = a.getAllValues();
                CtLiteral<String> from = (CtLiteral<String>) m.get("from");
                if (from != null) {
                    throw new IllegalConstructorTransitionError(from);
                }
            }
            setConstructorStates(f, an, c);
        } else {
            setDefaultState(f, tc);
        }
    }

    /**
     * Creates the list of states and adds them to the function
     *
     * @param f
     * @param anns
     * @param element
     */
    @SuppressWarnings({ "rawtypes" })
    private static void setConstructorStates(RefinedFunction f, List<CtAnnotation<? extends Annotation>> anns,
            CtElement element) throws LJError {
        List<ObjectState> l = new ArrayList<>();
        for (CtAnnotation<? extends Annotation> an : anns) {
            Map<String, CtExpression> m = an.getAllValues();
            String to = TypeCheckingUtils.getStringFromAnnotation(m.get("to"));
            ObjectState state = new ObjectState();
            if (to != null) {
                Predicate p = new Predicate(to, element);
                if (!p.getExpression().isBooleanExpression()) {
                    throw new InvalidRefinementError(element.getPosition(),
                            "State refinement transition must be a boolean expression", to);
                }
                state.setTo(p);
            }
            l.add(state);
        }
        f.setAllStates(l);
    }

    /**
     * Sets a default state where all ghost states are initialized to their default values
     * 
     * @param f
     * @param tc
     */
    public static void setDefaultState(RefinedFunction f, TypeChecker tc) {
        String klass = f.getTargetClass();
        Predicate[] s = { Predicate.createVar(Keys.THIS) };
        Predicate c = new Predicate();
        List<GhostFunction> sets = getDifferentSets(tc, klass); // ??
        for (GhostFunction sg : sets) {
            String retType = sg.getReturnType().toString();
            Predicate typePredicate = switch (retType) {
            case "int" -> Predicate.createLit("0", Types.INT);
            case "boolean" -> Predicate.createLit("false", Types.BOOLEAN);
            case "double" -> Predicate.createLit("0.0", Types.DOUBLE);
            default -> throw new RuntimeException("Ghost not implemented for type " + retType);
            };
            Predicate p = Predicate.createEquals(Predicate.createInvocation(sg.getQualifiedName(), s), typePredicate);
            c = Predicate.createConjunction(c, p);
        }
        ObjectState os = new ObjectState();
        os.setTo(c);
        List<ObjectState> los = new ArrayList<>();
        los.add(os);
        f.setAllStates(los);
    }

    /**
     * Gets the different ghost state sets for the given class
     * 
     * @param tc
     * @param klassQualified
     * 
     * @return list of different ghost function sets
     */
    private static List<GhostFunction> getDifferentSets(TypeChecker tc, String klassQualified) {
        List<GhostFunction> sets = new ArrayList<>();
        List<GhostState> l = getGhostStatesFor(klassQualified, tc);
        for (GhostState g : l) {
            if (g.getParent() == null) {
                sets.add(g);
            } else if (!sets.contains(g.getParent())) {
                sets.add(g.getParent());
            }
        }
        return sets;
    }

    /**
     * Handles the passage of the written state annotations to the context for regular Methods
     *
     * @param method
     * @param f
     * @param tc
     */
    public static void handleMethodState(CtMethod<?> method, RefinedFunction f, TypeChecker tc, String prefix)
            throws LJError {
        List<CtAnnotation<? extends Annotation>> an = getStateAnnotation(method);
        if (!an.isEmpty()) {
            setFunctionStates(f, an, tc, method, prefix);
        }
    }

    /**
     * Creates the list of states and adds them to the function
     *
     * @param f
     * @param anns
     * @param tc
     * @param element
     */
    private static void setFunctionStates(RefinedFunction f, List<CtAnnotation<? extends Annotation>> anns,
            TypeChecker tc, CtElement element, String prefix) throws LJError {
        List<ObjectState> l = new ArrayList<>();
        for (CtAnnotation<? extends Annotation> an : anns) {
            l.add(getStates(an, f, tc, element, prefix));
        }
        f.setAllStates(l);
    }

    @SuppressWarnings({ "rawtypes" })
    private static ObjectState getStates(CtAnnotation<? extends Annotation> ctAnnotation, RefinedFunction f,
            TypeChecker tc, CtElement e, String prefix) throws LJError {
        Map<String, CtExpression> m = ctAnnotation.getAllValues();
        String from = TypeCheckingUtils.getStringFromAnnotation(m.get("from"));
        String to = TypeCheckingUtils.getStringFromAnnotation(m.get("to"));
        ObjectState state = new ObjectState();

        // has from
        if (from != null)
            state.setFrom(createStatePredicate(from, f.getTargetClass(), tc, e, false, prefix));

        // has to
        if (to != null)
            state.setTo(createStatePredicate(to, f.getTargetClass(), tc, e, true, prefix));

        // has from but not to, state remains the same
        if (from != null && to == null)
            state.setTo(createStatePredicate(from, f.getTargetClass(), tc, e, true, prefix));

        // has to but not from, state enters with true
        if (from == null && to != null)
            state.setFrom(new Predicate());

        return state;
    }

    /**
     * Creates the predicate for state transition
     * 
     * @param value
     * @param targetClass
     * @param tc
     * @param e
     * @param isTo
     * @param prefix
     * 
     * @return the created predicate
     */
    private static Predicate createStatePredicate(String value, String targetClass, TypeChecker tc, CtElement e,
            boolean isTo, String prefix) throws LJError {
        Predicate p = new Predicate(value, e, prefix);
        if (!p.getExpression().isBooleanExpression()) {
            throw new InvalidRefinementError(e.getPosition(),
                    "State refinement transition must be a boolean expression", value);
        }
        CtTypeReference<?> r = tc.getFactory().Type().createReference(targetClass);
        String nameOld = String.format(Formats.INSTANCE, Keys.THIS, tc.getContext().getCounter());
        String name = String.format(Formats.INSTANCE, Keys.THIS, tc.getContext().getCounter());
        tc.getContext().addVarToContext(name, r, new Predicate(), e);
        tc.getContext().addVarToContext(nameOld, r, new Predicate(), e);
        // TODO REVIEW!!
        // what is it for?
        Predicate c1 = isTo ? getMissingStates(targetClass, tc, p) : p;
        Predicate c = c1.substituteVariable(Keys.THIS, name);
        c = c.changeOldMentions(nameOld, name);
        boolean ok = tc.checksStateSMT(new Predicate(), c.negate(), e.getPosition());
        if (ok) {
            tc.throwStateConflictError(e.getPosition(), p);
        }
        return c1;
    }

    /**
     * Gets the missing states in the predicate and adds equalities to old states
     * 
     * @param t
     * @param tc
     * @param p
     * 
     * @return the updated predicate
     */
    private static Predicate getMissingStates(String t, TypeChecker tc, Predicate p) {
        List<GhostState> gs = p.getStateInvocations(getGhostStatesFor(t, tc));
        List<GhostFunction> sets = getDifferentSets(tc, t);
        for (GhostState g : gs) {
            if (g.getParent() == null && sets.contains(g)) {
                sets.remove(g);
            } else if (g.getParent() != null) {
                sets.remove(g.getParent());
            }
        }
        return addOldStates(p, Predicate.createVar(Keys.THIS), sets);
    }

    /**
     * Collect ghost states for the given qualified class name and its immediate supertypes (superclass and interfaces)
     * 
     * @param qualifiedClass
     * @param tc
     * 
     * @return list of ghost states
     */
    private static List<GhostState> getGhostStatesFor(String qualifiedClass, TypeChecker tc) {
        // Keep order: class, then superclass, then interfaces; avoid duplicates
        java.util.LinkedHashSet<String> typeNames = new java.util.LinkedHashSet<>();
        typeNames.add(Utils.getSimpleName(qualifiedClass));

        CtTypeReference<?> ref = tc.getFactory().Type().createReference(qualifiedClass);
        if (ref != null) {
            CtTypeReference<?> sup = ref.getSuperclass();
            if (sup != null)
                typeNames.add(Utils.getSimpleName(sup.getQualifiedName()));
            for (CtTypeReference<?> itf : ref.getSuperInterfaces()) {
                if (itf != null)
                    typeNames.add(Utils.getSimpleName(itf.getQualifiedName()));
            }
        }

        List<GhostState> res = new ArrayList<>();
        for (String tn : typeNames) {
            List<GhostState> states = tc.getContext().getGhostState(tn);
            if (states != null)
                res.addAll(states);
        }
        return res;
    }

    /**
     * Create predicate with the equalities with previous versions of the object e.g., ghostfunction1(this) ==
     * ghostfunction1(old(this))
     * 
     * @param p
     * @param th
     * @param sets
     * 
     * @return updated predicate
     */
    private static Predicate addOldStates(Predicate p, Predicate th, List<GhostFunction> sets) {
        Predicate c = p;
        for (GhostFunction gf : sets) {
            Predicate eq = Predicate.createEquals( // gf.name == old(gf.name(this))
                    Predicate.createInvocation(gf.getQualifiedName(), th),
                    Predicate.createInvocation(gf.getQualifiedName(), Predicate.createInvocation(Keys.OLD, th)));
            c = Predicate.createConjunction(c, eq);
        }
        return c;
    }

    // ################ Handling State Change effects ################

    /**
     * Sets the new state acquired from the constructor call
     *
     * @param refKey
     * @param f
     * @param map
     * @param ctConstructorCall
     */
    public static void constructorStateMetadata(String refKey, RefinedFunction f, Map<String, String> map,
            CtConstructorCall<?> ctConstructorCall) {
        List<Predicate> oc = f.getToStates();
        if (!oc.isEmpty()) {
            Predicate c = oc.get(0);
            for (String k : map.keySet()) {
                c = c.substituteVariable(k, map.get(k));
            }
            ctConstructorCall.putMetadata(refKey, c);
            // add mapping to oc.get(0)-HERE
        }
    }

    /**
     * If an expression has a state in metadata, then its state is passed to the last instance of the variable with
     * varName
     *
     * @param tc
     * @param varName
     * @param e
     */
    public static void addStateRefinements(TypeChecker tc, String varName, CtExpression<?> e) {
        Optional<VariableInstance> ovi = tc.getContext().getLastVariableInstance(varName);
        if (ovi.isPresent() && e.getMetadata(Keys.REFINEMENT) != null) {
            VariableInstance vi = ovi.get();
            Predicate c = (Predicate) e.getMetadata(Keys.REFINEMENT);
            c = c.substituteVariable(Keys.THIS, vi.getName()).substituteVariable(Keys.WILDCARD, vi.getName());
            vi.setRefinement(c);
        }
    }

    /**
     * Checks the changes in the state of the target
     *
     * @param tc
     * @param f
     * @param target2
     * @param map
     * @param invocation
     */
    public static void checkTargetChanges(TypeChecker tc, RefinedFunction f, CtExpression<?> target2,
            Map<String, String> map, CtElement invocation) throws LJError {
        String parentTargetName = searchFistVariableTarget(tc, target2, invocation);
        VariableInstance target = getTarget(invocation);
        if (target != null) {
            if (f.hasStateChange() && !f.getFromStates().isEmpty()) {
                changeState(tc, target, f.getAllStates(), parentTargetName, map, invocation);
            }
            if (!f.hasStateChange()) {
                sameState(tc, target, parentTargetName, invocation);
            }
        }
    }

    /**
     * Updates the ghost field after a write
     * 
     * @param fw
     * @param tc
     */
    public static void updateGhostField(CtFieldWrite<?> fw, TypeChecker tc) throws LJError {
        CtField<?> field = fw.getVariable().getDeclaration();
        String updatedVarName = String.format(Formats.THIS, fw.getVariable().getSimpleName());
        String targetClass = field.getDeclaringType().getQualifiedName();

        // state transition annotation construction
        String stateChangeRefinementTo = field.getSimpleName() + "(this) == " + updatedVarName;
        String stateChangeRefinementFrom = "true";

        // extracting target from assignment

        // only works for things in form of this.field_name = 1
        // does not work thor thins like `void method(){otherMethod();}`
        if (!(fw.getTarget() instanceof CtVariableRead<?>)) {
            return;
        }

        String parentTargetName = ((CtVariableRead<?>) fw.getTarget()).getVariable().getSimpleName();
        Optional<VariableInstance> invocationCallee = tc.getContext().getLastVariableInstance(parentTargetName);

        if (invocationCallee.isEmpty()) {
            return;
        }

        VariableInstance vi = invocationCallee.get();
        String instanceName = vi.getName();
        Predicate prevState = vi.getRefinement().substituteVariable(Keys.WILDCARD, instanceName)
                .substituteVariable(parentTargetName, instanceName);

        ObjectState stateChange = new ObjectState();
        String prefix = field.getDeclaringType().getQualifiedName();
        Predicate fromPredicate = createStatePredicate(stateChangeRefinementFrom, targetClass, tc, fw, false, prefix);
        Predicate toPredicate = createStatePredicate(stateChangeRefinementTo, targetClass, tc, fw, true, prefix);
        stateChange.setFrom(fromPredicate);
        stateChange.setTo(toPredicate);

        // replace "state(this)" to "state(whatever method is called from) and so on"
        Predicate expectState = stateChange.getFrom().substituteVariable(Keys.THIS, instanceName)
                .changeOldMentions(vi.getName(), instanceName);

        if (!tc.checksStateSMT(prevState, expectState, fw.getPosition())) { // Invalid field transition
            tc.throwStateRefinementError(fw.getPosition(), prevState, stateChange.getFrom());
            return;
        }

        String newInstanceName = String.format(Formats.INSTANCE, parentTargetName, tc.getContext().getCounter());
        Predicate transitionedState = stateChange.getTo().substituteVariable(Keys.WILDCARD, newInstanceName)
                .substituteVariable(Keys.THIS, newInstanceName);

        transitionedState = checkOldMentions(transitionedState, instanceName, newInstanceName);
        // update of stata of new instance of this#n#(whatever it was + 1)

        VariableInstance vi2 = (VariableInstance) tc.getContext().addInstanceToContext(newInstanceName, vi.getType(),
                vi.getRefinement(), fw);
        vi2.setRefinement(transitionedState);

        RefinedVariable rv = tc.getContext().getVariableByName(parentTargetName);
        rv.getSuperTypes().forEach(vi2::addSuperType);

        // if the variable is a parent (not a VariableInstance) we need to check that
        // this refinement
        // is a subtype of the variable's main refinement
        if (rv instanceof Variable) {
            Predicate superC = rv.getMainRefinement().substituteVariable(rv.getName(), vi2.getName());
            tc.checkSMT(superC, fw);
            tc.getContext().addRefinementInstanceToVariable(parentTargetName, newInstanceName);
        }
    }

    /**
     * Changes the state
     *
     * @param tc
     * @param vi
     * @param stateChanges
     * @param name
     * @param map
     * @param invocation
     */
    private static void changeState(TypeChecker tc, VariableInstance vi, List<ObjectState> stateChanges, String name,
            Map<String, String> map, CtElement invocation) throws LJError {
        if (vi.getRefinement() == null) {
            return;
        }
        String instanceName = vi.getName();
        Predicate prevState = vi.getRefinement().substituteVariable(Keys.WILDCARD, instanceName)
                .substituteVariable(name, instanceName);

        boolean found = false;
        for (ObjectState stateChange : stateChanges) { // TODO: only working for 1 state annotation
            if (found) {
                break;
            }
            if (!stateChange.hasFrom()) {
                continue;
            }
            // replace "state(this)" to "state(whatever method is called from) and so on"
            Predicate expectState = stateChange.getFrom().substituteVariable(Keys.THIS, instanceName);
            Predicate prevCheck = prevState;
            for (String s : map.keySet()) { // substituting function variables into annotation if there are any
                prevCheck = prevCheck.substituteVariable(s, map.get(s));
                expectState = expectState.substituteVariable(s, map.get(s));
            }
            expectState = expectState.changeOldMentions(vi.getName(), instanceName);

            found = tc.checksStateSMT(prevCheck, expectState, invocation.getPosition());
            if (found && stateChange.hasTo()) {
                String newInstanceName = String.format(Formats.INSTANCE, name, tc.getContext().getCounter());
                Predicate transitionedState = stateChange.getTo().substituteVariable(Keys.WILDCARD, newInstanceName)
                        .substituteVariable(Keys.THIS, newInstanceName);
                for (String s : map.keySet()) {
                    transitionedState = transitionedState.substituteVariable(s, map.get(s));
                }
                transitionedState = checkOldMentions(transitionedState, instanceName, newInstanceName);
                // update of stata of new instance of this#n#(whatever it was + 1)
                addInstanceWithState(tc, name, newInstanceName, vi, transitionedState, invocation);
                return;
            }
        }
        if (!found) { // Reaches the end of stateChange no matching states
            Predicate expectedStatesDisjunction = stateChanges.stream().filter(ObjectState::hasFrom)
                    .map(ObjectState::getFrom)
                    .reduce(Predicate.createLit("false", Types.BOOLEAN), Predicate::createDisjunction);
            String simpleInvocation = invocation.toString();
            tc.throwStateRefinementError(invocation.getPosition(), prevState, expectedStatesDisjunction);
        }
    }

    private static Predicate checkOldMentions(Predicate transitionedState, String instanceName,
            String newInstanceName) {
        return transitionedState.changeOldMentions(instanceName, newInstanceName);
    }

    /**
     * Copies the previous state to the new variable instance
     *
     * @param tc
     * @param variableInstance
     * @param name
     * @param invocation
     */
    private static void sameState(TypeChecker tc, VariableInstance variableInstance, String name, CtElement invocation)
            throws LJError {
        if (variableInstance.getRefinement() != null) {
            String newInstanceName = String.format(Formats.INSTANCE, name, tc.getContext().getCounter());
            Predicate c = variableInstance.getRefinement().substituteVariable(Keys.WILDCARD, newInstanceName)
                    .substituteVariable(variableInstance.getName(), newInstanceName);

            addInstanceWithState(tc, name, newInstanceName, variableInstance, c, invocation);
        }
    }

    /**
     * Adds a new instance with the given state to the parent variable
     *
     * @param tc
     * @param superName
     * @param name2
     * @param prevInstance
     * @param transitionedState
     * @param invocation
     */
    private static void addInstanceWithState(TypeChecker tc, String superName, String name2,
            VariableInstance prevInstance, Predicate transitionedState, CtElement invocation) throws LJError {
        VariableInstance vi2 = (VariableInstance) tc.getContext().addInstanceToContext(name2, prevInstance.getType(),
                prevInstance.getRefinement(), invocation);
        vi2.setRefinement(transitionedState);
        Context ctx = tc.getContext();
        if (ctx.hasVariable(superName)) {
            RefinedVariable rv = ctx.getVariableByName(superName);
            for (CtTypeReference<?> t : rv.getSuperTypes()) {
                vi2.addSuperType(t);
            }

            // if the variable is a parent (not a VariableInstance) we need to check that
            // this refinement
            // is a subtype of the variable's main refinement
            if (rv instanceof Variable) {
                Predicate superC = rv.getMainRefinement().substituteVariable(rv.getName(), vi2.getName());
                tc.checkSMT(superC, invocation);
                tc.getContext().addRefinementInstanceToVariable(superName, name2);
            }
        }
        invocation.putMetadata(Keys.TARGET, vi2);
    }

    /**
     * Gets the name of the parent target and adds the closest target to the elem TARGET metadata
     *
     * @param invocation
     *
     * @return the name of the parent target
     */
    static String searchFistVariableTarget(TypeChecker tc, CtElement target2, CtElement invocation) {
        if (target2 instanceof CtVariableRead<?> v) {
            // v--------- field read
            // means invocation is in a form of `t.method(args)`
            String name = v.getVariable().getSimpleName();
            Optional<VariableInstance> invocationCallee = tc.getContext().getLastVariableInstance(name);
            if (invocationCallee.isPresent()) {
                invocation.putMetadata(Keys.TARGET, invocationCallee.get());
            } else if (target2.getMetadata(Keys.TARGET) == null) {
                RefinedVariable var = tc.getContext().getVariableByName(name);
                String nName = String.format(Formats.INSTANCE, name, tc.getContext().getCounter());
                RefinedVariable rv = tc.getContext().addInstanceToContext(nName, var.getType(),
                        var.getRefinement().substituteVariable(name, nName), target2);
                tc.getContext().addRefinementInstanceToVariable(name, nName);
                invocation.putMetadata(Keys.TARGET, rv);
            }

            return name;
        } else if (target2.getMetadata(Keys.TARGET) != null) {
            // invocation is in
            // who did put the metadata here then?
            VariableInstance target2Vi = (VariableInstance) target2.getMetadata(Keys.TARGET);
            Optional<Variable> v = target2Vi.getParent();
            invocation.putMetadata(Keys.TARGET, target2Vi);
            return v.map(Refined::getName).orElse(target2Vi.getName());
        }
        return null;
    }

    static VariableInstance getTarget(CtElement invocation) {
        if (invocation.getMetadata(Keys.TARGET) != null) {
            return (VariableInstance) invocation.getMetadata(Keys.TARGET);
        }
        return null;
    }

    private static List<CtAnnotation<? extends Annotation>> getStateAnnotation(CtElement element) {
        return element.getAnnotations().stream().filter(ann -> ann.getActualAnnotation().annotationType()
                .getCanonicalName().contentEquals("liquidjava.specification.StateRefinement"))
                .collect(Collectors.toList());
    }
}