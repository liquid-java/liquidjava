package liquidjava.processor.refinement_checker.object_checkers;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

import liquidjava.diagnostics.DebugLog;
import liquidjava.diagnostics.categories.ErrorCategory;
import liquidjava.diagnostics.errors.IllegalConstructorTransitionError;
import liquidjava.diagnostics.errors.InvalidRefinementError;
import liquidjava.diagnostics.errors.LJError;
import liquidjava.diagnostics.errors.SMTUnknownError;
import liquidjava.diagnostics.errors.StateErrorContext;
import liquidjava.diagnostics.hints.Hint;
import liquidjava.diagnostics.hints.StateHints;
import liquidjava.processor.context.*;
import liquidjava.processor.refinement_checker.TypeChecker;
import liquidjava.processor.refinement_checker.TypeCheckingUtils;
import liquidjava.processor.state_machine.DecidingArgument;
import liquidjava.processor.state_machine.State;
import liquidjava.processor.state_machine.StateErrorCategories;
import liquidjava.processor.state_machine.StateMachine;
import liquidjava.processor.state_machine.StateMachineBuilder;
import liquidjava.processor.state_machine.StateSolver;
import liquidjava.rj_language.Predicate;
import liquidjava.utils.Utils;
import liquidjava.utils.constants.Formats;
import liquidjava.utils.constants.Keys;
import liquidjava.utils.constants.Types;
import spoon.reflect.code.*;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtExecutableReference;
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
        List<CtAnnotation<? extends Annotation>> an = getStateAnnotations(c);
        if (!an.isEmpty()) {
            for (CtAnnotation<? extends Annotation> a : an) {
                Map<String, CtExpression> m = a.getAllValues();
                CtLiteral<String> from = (CtLiteral<String>) m.get("from");
                if (from != null) {
                    throw new IllegalConstructorTransitionError(Utils.getAnnotationValuePosition(from));
                }
            }
            setConstructorStates(f, an, c);
        } else if (!inheritSuperConstructorState(c, f, tc)) {
            setDefaultState(f, tc);
        }
    }

    /**
     * An unannotated constructor whose body is only a {@code super(...)} call leaves the object in the state that the
     * called superclass constructor's specification gives it (e.g. {@code MyException(String m, Throwable c) { super(m,
     * c); }} with the {@code Throwable(String, Throwable)} spec: {@code withThrowable(this)}). The super constructor's
     * parameters are renamed to the arguments passed, which must be parameters of this constructor or literals.
     *
     * @return whether a state was inherited (otherwise the caller falls back to the default state)
     */
    private static boolean inheritSuperConstructorState(CtConstructor<?> c, RefinedFunction f, TypeChecker tc) {
        Optional<CtInvocation<?>> call = onlySuperCall(c);
        if (call.isEmpty())
            return false;
        List<Optional<Predicate>> values = call.get().getArguments().stream().map(AuxStateHandler::argumentValue)
                .toList();
        Optional<List<Predicate>> states = specifiedStates(tc, call.get().getExecutable(), values,
                c.getDeclaringType().getQualifiedName());
        states.ifPresent(
                to -> f.setAllStates(to.stream().map(p -> new ObjectState(null, p)).collect(Collectors.toList())));
        return states.isPresent();
    }

    /**
     * The {@code super(...)} call that is all {@code c} runs, if so. Anything after it, or an instance initializer or
     * field initializer of the class (which run right after it), may change the state, as in {@code super(m);
     * initCause(c);}. This is the source counterpart of what {@link ConstructorDelegation} accepts in bytecode.
     */
    private static Optional<CtInvocation<?>> onlySuperCall(CtConstructor<?> c) {
        CtType<?> owner = c.getDeclaringType();
        if (owner == null || c.getBody() == null || c.getBody().getStatements().size() != 1
                || !(c.getBody().getStatement(0)instanceof CtInvocation<?> call)
                || !call.getExecutable().isConstructor()
                || owner.getReference().equals(call.getExecutable().getDeclaringType()))
            return Optional.empty(); // not only a super(...) call (this(...) delegates within the class)
        boolean initializers = owner instanceof CtClass<?> klass
                && klass.getAnonymousExecutables().stream().anyMatch(block -> !block.isStatic());
        boolean fieldInitializers = owner.getFields().stream()
                .anyMatch(field -> !field.isStatic() && field.getDefaultExpression() != null);
        return initializers || fieldInitializers ? Optional.empty() : Optional.of(call);
    }

    /**
     * The state that {@code new C(...)} gives when {@code C}'s constructor has no specification of its own but is a
     * binary constructor that delegates to a specified one of an ancestor (e.g. {@code new RuntimeException(m)} runs
     * {@code Throwable(String)}), with the arguments renamed as in {@link #inheritSuperConstructorState}.
     */
    public static Optional<Predicate> delegatedConstructorState(TypeChecker tc, CtConstructorCall<?> call) {
        if (call instanceof CtNewClass<?> || call.getExecutable().getDeclaringType() == null)
            return Optional.empty(); // an anonymous class body may change the state
        List<Optional<Predicate>> values = call.getArguments().stream().map(a -> callArgumentValue(tc, a)).toList();
        return specifiedStates(tc, call.getExecutable(), values,
                call.getExecutable().getDeclaringType().getQualifiedName()).filter(to -> !to.isEmpty())
                        .map(to -> to.get(0));
    }

    /**
     * The to-states of the specified constructor that {@code exe} ends up running, with each of its parameters replaced
     * by the value passed for it, and the default value for each state set of {@code constructed} that they leave
     * unconstrained. Empty if no specified constructor is known, or if a state mentions a parameter whose value cannot
     * be written.
     *
     * @param argumentValues
     *            how a state can write each argument of the call to {@code exe}
     */
    private static Optional<List<Predicate>> specifiedStates(TypeChecker tc, CtExecutableReference<?> exe,
            List<Optional<Predicate>> argumentValues, String constructed) {
        if (exe.getDeclaringType() == null || argumentValues.size() != exe.getParameters().size())
            return Optional.empty(); // e.g. a varargs call
        Optional<SpecifiedConstructor> spec = specifiedConstructor(tc, exe);
        if (spec.isEmpty() || spec.get().function().getToStates().isEmpty())
            return Optional.empty();

        // replace each spec parameter by the value passed for it; a state may not mention one without a value
        Map<String, Predicate> values = new HashMap<>();
        Set<String> unknown = new HashSet<>();
        List<Variable> params = spec.get().function().getArguments();
        for (int i = 0; i < params.size(); i++) {
            String param = params.get(i).getName();
            ConstructorDelegation.Argument passed = spec.get().arguments().get(i);
            Optional<Predicate> value = passed instanceof ConstructorDelegation.Parameter p
                    ? argumentValues.get(p.index())
                    : ((ConstructorDelegation.Constant) passed).value().flatMap(AuxStateHandler::literal);
            value.ifPresentOrElse(v -> values.put(param, v), () -> unknown.add(param));
        }
        List<Predicate> states = spec.get().function().getToStates();
        if (states.stream().anyMatch(to -> to.getVariableNames().stream().anyMatch(unknown::contains)))
            return Optional.empty();
        // a spec that leaves some state sets unchanged refers to old(this), which a new object does not have
        if (states.stream().anyMatch(to -> to.invokes(Keys.OLD)))
            return Optional.empty();
        return Optional.of(states.stream().map(to -> substituteAll(to, values))
                .map(to -> withDefaultsForOtherSets(tc, constructed, to)).toList());
    }

    /**
     * Replaces each variable of {@code values} at once, so a value that is also a name in {@code values} is not
     * replaced again (e.g. {@code Sub(boolean y, boolean x) { super(y, x); }} for {@code Base(boolean x, boolean y)}).
     */
    private static Predicate substituteAll(Predicate p, Map<String, Predicate> values) {
        Map<String, String> placeholders = new HashMap<>();
        for (String name : values.keySet()) {
            String placeholder = "#param_" + placeholders.size();
            placeholders.put(name, placeholder);
            p = p.substituteVariable(name, placeholder);
        }
        for (Map.Entry<String, String> e : placeholders.entrySet())
            p = p.substituteVariable(e.getValue(), values.get(e.getKey()));
        return p;
    }

    /**
     * {@code to} and the default value of each state set of {@code klass} it does not mention: an ancestor's
     * constructor spec says nothing about the sets a subclass adds (e.g. {@code Stack}'s, after {@code Vector()}).
     */
    private static Predicate withDefaultsForOtherSets(TypeChecker tc, String klass, Predicate to) {
        return Predicate.createConjunction(to,
                defaultValues(unmentionedSets(tc, klass, to), Predicate.createVar(Keys.THIS)));
    }

    /** How a state can write {@code arg}: by the caller's parameter name or a literal. */
    private static Optional<Predicate> argumentValue(CtExpression<?> arg) {
        if (arg instanceof CtVariableRead<?> vr
                && vr.getVariable() instanceof spoon.reflect.reference.CtParameterReference<?>)
            return Optional.of(Predicate.createVar(vr.getVariable().getSimpleName()));
        return literalValue(arg);
    }

    /**
     * How a state can write {@code arg} at a call: by the variable's current instance, which keeps the value it has now
     * even if the variable is assigned later, or a literal. A variable without an instance (e.g. a parameter not
     * assigned yet) has no known value here.
     */
    private static Optional<Predicate> callArgumentValue(TypeChecker tc, CtExpression<?> arg) {
        if (arg instanceof CtVariableRead<?> vr && !(arg instanceof CtFieldRead<?>))
            return tc.getContext().getLastVariableInstance(vr.getVariable().getSimpleName())
                    .map(vi -> Predicate.createVar(vi.getName()));
        return literalValue(arg);
    }

    /** {@code arg} as a literal of the refinement language, if it is a boolean or int literal. */
    private static Optional<Predicate> literalValue(CtExpression<?> arg) {
        return arg instanceof CtLiteral<?> lit ? literal(lit.getValue()) : Optional.empty();
    }

    private static Optional<Predicate> literal(Object value) {
        if (value instanceof Boolean)
            return Optional.of(Predicate.createLit(value.toString(), Types.BOOLEAN));
        if (value instanceof Integer || value instanceof Short || value instanceof Byte)
            return Optional.of(Predicate.createLit(value.toString(), Types.INT));
        return Optional.empty();
    }

    /**
     * A constructor specification, and what a call passes for each of its parameters.
     *
     * @param arguments
     *            for each parameter of {@code function}: an index into the call's arguments, or a constant
     */
    private record SpecifiedConstructor(RefinedFunction function, List<ConstructorDelegation.Argument> arguments) {
    }

    /**
     * The specified constructor that {@code exe} ends up running, if one is known. A binary constructor without a
     * specification is walked through only when its bytecode just passes values on to a superclass constructor (e.g.
     * {@code RuntimeException(String)} to {@code Exception(String)} to {@code Throwable(String)}); assuming any other
     * constructor did so would hide real errors (e.g. {@code ClassNotFoundException(String)} also sets a null cause).
     */
    private static Optional<SpecifiedConstructor> specifiedConstructor(TypeChecker tc, CtExecutableReference<?> exe) {
        List<ConstructorDelegation.Argument> arguments = new ArrayList<>();
        for (int i = 0; i < exe.getParameters().size(); i++)
            arguments.add(new ConstructorDelegation.Parameter(i));
        String klass = exe.getDeclaringType().getQualifiedName();
        RefinedFunction f = tc.getContext().getFunction(exe.getSimpleName(), klass, exe.getParameters());
        Optional<List<String>> types = erasedNames(exe.getParameters());
        while (f == null && types.isPresent()) {
            Optional<ConstructorDelegation> delegation = ConstructorDelegation.of(klass, types.get());
            if (delegation.isEmpty())
                return Optional.empty();
            arguments = passedOn(arguments, delegation.get().arguments());
            klass = delegation.get().superclass();
            types = Optional.of(delegation.get().superParameterTypes());
            // exact types: the arity fallback of getFunction could pick another overload's spec
            f = tc.getContext().getFunctionExact(exe.getSimpleName(), klass, typeReferences(tc, types.get()));
        }
        return f == null ? Optional.empty() : Optional.of(new SpecifiedConstructor(f, arguments));
    }

    /**
     * What reaches the superclass constructor, in terms of the original call: a parameter passed on refers back to the
     * value the call gave for it.
     */
    private static List<ConstructorDelegation.Argument> passedOn(List<ConstructorDelegation.Argument> callArguments,
            List<ConstructorDelegation.Argument> delegated) {
        return delegated.stream()
                .map(a -> a instanceof ConstructorDelegation.Parameter p ? callArguments.get(p.index()) : a).toList();
    }

    /** The erased qualified names of {@code types}, as in bytecode descriptors, if they can be resolved. */
    private static Optional<List<String>> erasedNames(List<CtTypeReference<?>> types) {
        try {
            return Optional.of(types.stream().map(t -> t.getTypeErasure().getQualifiedName()).toList());
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private static List<CtTypeReference<?>> typeReferences(TypeChecker tc, List<String> names) {
        List<CtTypeReference<?>> refs = new ArrayList<>();
        for (String name : names)
            refs.add(tc.getFactory().Type().createReference(name));
        return refs;
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
            String msg = TypeCheckingUtils.getStringFromAnnotation(m.get("msg"));
            ObjectState state = new ObjectState();
            if (msg != null && !msg.isEmpty())
                state.setMessage(msg);

            if (to != null) {
                Predicate p = new Predicate(to, element);
                if (!p.getExpression().isBooleanExpression()) {
                    throw new InvalidRefinementError(Utils.getLJAnnotationPosition(element, to),
                            "State refinement transition must be a boolean expression", to);
                }
                state.setTo(p);
                state.setToPosition(Utils.getLJAnnotationPosition(element, to));
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
        ObjectState os = new ObjectState();
        os.setTo(getDefaultState(tc, f.getTargetClass(), Predicate.createVar(Keys.THIS)));
        List<ObjectState> los = new ArrayList<>();
        los.add(os);
        f.setAllStates(los);
    }

    public static Predicate getDefaultState(TypeChecker tc, String klass, Predicate target) {
        return defaultValues(getDifferentSets(tc, klass), target);
    }

    /** Each of {@code sets} at its default value on {@code target}. */
    private static Predicate defaultValues(List<GhostFunction> sets, Predicate target) {
        Predicate c = new Predicate();
        for (GhostFunction sg : sets) {
            String retType = sg.getReturnType().toString();
            Predicate typePredicate = switch (retType) {
            case "int" -> Predicate.createLit("0", Types.INT);
            case "char" -> Predicate.createLit("\u0000", Types.CHAR);
            case "boolean" -> Predicate.createLit("false", Types.BOOLEAN);
            case "double" -> Predicate.createLit("0.0", Types.DOUBLE);
            default -> throw new RuntimeException("Ghost not implemented for type " + retType);
            };
            Predicate p = Predicate.createEquals(Predicate.createInvocation(sg.getQualifiedName(), target),
                    typePredicate);
            c = Predicate.createConjunction(c, p);
        }
        return c;
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
        List<CtAnnotation<? extends Annotation>> an = getStateAnnotations(method);
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
        String msg = TypeCheckingUtils.getStringFromAnnotation(m.get("msg"));
        ObjectState state = new ObjectState();
        if (msg != null && !msg.isEmpty())
            state.setMessage(msg);

        // has from
        if (from != null) {
            state.setFrom(createStatePredicate(new Predicate(from, e, prefix), from, f.getTargetClass(), tc, e, false));
            state.setFromPosition(Utils.getLJAnnotationPosition(e, from));
        }

        // has to
        if (to != null) {
            state.setTo(createStatePredicate(new Predicate(to, e, prefix), to, f.getTargetClass(), tc, e, true));
            state.setToPosition(Utils.getLJAnnotationPosition(e, to));
        }

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
    private static Predicate createStatePredicate(Predicate p, String value, String targetClass, TypeChecker tc,
            CtElement e, boolean isTo) throws LJError {
        SourcePosition position = Utils.getLJAnnotationPosition(e, value);
        if (!p.getExpression().isBooleanExpression()) {
            throw new InvalidRefinementError(position, "State refinement transition must be a boolean expression",
                    value);
        }
        CtTypeReference<?> returnType = null;
        if (isTo && e instanceof CtMethod<?> method) {
            CtTypeReference<?> mt = method.getType();
            if (mt != null && !"void".equals(mt.toString())) {
                p = p.substituteVariable("return", Keys.WILDCARD);
                returnType = mt;
            }
        }
        CtTypeReference<?> r = tc.getFactory().Type().createReference(targetClass);
        tc.getContext().enterContext();
        try {
            String nameOld = String.format(Formats.INSTANCE, Keys.THIS, tc.getContext().getCounter());
            String name = String.format(Formats.INSTANCE, Keys.THIS, tc.getContext().getCounter());
            tc.getContext().addVarToContext(Keys.THIS, r, new Predicate(), e);
            tc.getContext().addVarToContext(name, r, new Predicate(), e);
            tc.getContext().addVarToContext(nameOld, r, new Predicate(), e);
            // in this scope, so that each method's `return` has its own type
            if (returnType != null)
                tc.getContext().addVarToContext(Keys.WILDCARD, returnType, new Predicate(), e);
            // TODO REVIEW!!
            // what is it for?
            Predicate c1 = isTo ? getMissingStates(targetClass, tc, p) : p;
            Predicate c = c1.substituteVariable(Keys.THIS, name);
            c = c.changeOldMentions(nameOld, name);
            boolean ok = tc.checkStateSMT(new Predicate(), c.negate(), position, true);
            if (ok)
                tc.throwStateConflictError(position, p);
            return c1;
        } finally {
            tc.getContext().exitContext();
        }
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
        return addOldStates(p, Predicate.createVar(Keys.THIS), unmentionedSets(tc, t, p));
    }

    /** The ghost state sets of class {@code t} that {@code p} does not mention. */
    private static List<GhostFunction> unmentionedSets(TypeChecker tc, String t, Predicate p) {
        List<GhostState> gs = p.getStateInvocations(getGhostStatesFor(t, tc));
        List<GhostFunction> sets = getDifferentSets(tc, t);
        for (GhostState g : gs) {
            if (g.getParent() == null && sets.contains(g)) {
                sets.remove(g);
            } else if (g.getParent() != null) {
                sets.remove(g.getParent());
            }
        }
        return sets;
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
            ctConstructorCall.putMetadata(Keys.STATE_CHANGE,
                    new StateChange(f, f.getAllStates().get(0), null, ctConstructorCall.getPosition()));
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
            if (e.getMetadata(Keys.STATE_CHANGE)instanceof StateChange change)
                vi.setStateChange(change);
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
        String parentTargetName = prepareInvocationTarget(tc, target2, invocation);
        VariableInstance target = getTarget(invocation);
        if (target != null) {
            if (f.hasStateChange() && !f.getFromStates().isEmpty()) {
                changeState(tc, target, f, parentTargetName, map, invocation);
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
        String updatedVarName = Utils.qualifyFieldName(fw.getVariable());
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
        Predicate fromPredicate = createStatePredicate(new Predicate(), stateChangeRefinementFrom, targetClass, tc, fw,
                false);
        Predicate toPredicate = Predicate.createEquals(Predicate
                .createInvocation(Utils.qualifyName(prefix, field.getSimpleName()), Predicate.createVar(Keys.THIS)),
                Predicate.createVar(updatedVarName));
        toPredicate = createStatePredicate(toPredicate, stateChangeRefinementTo, targetClass, tc, fw, true);
        stateChange.setFrom(fromPredicate);
        stateChange.setTo(toPredicate);

        // replace "state(this)" to "state(whatever method is called from) and so on"
        Predicate expectState = stateChange.getFrom().substituteVariable(Keys.THIS, instanceName)
                .changeOldMentions(vi.getName(), instanceName);

        boolean validTransition;
        try {
            validTransition = tc.checkStateSMT(prevState, expectState, fw.getPosition());
        } catch (SMTUnknownError error) {
            error.setDeclarationPosition(field.getDeclaringType().getPosition());
            throw error;
        }
        if (!validTransition) { // Invalid field transition
            tc.throwStateRefinementError(fw.getPosition(), field.getDeclaringType().getPosition(), prevState,
                    expectState, stateChange.getMessage());
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
            tc.checkSMT(superC, fw, rv.getPlacementInCode().getAnnotationPosition(), null);
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
    private static void changeState(TypeChecker tc, VariableInstance vi, RefinedFunction function, String name,
            Map<String, String> map, CtElement invocation) throws LJError {
        if (vi.getRefinement() == null) {
            return;
        }
        List<ObjectState> stateChanges = function.getAllStates();
        String instanceName = vi.getName();
        Predicate prevState = vi.getRefinement().substituteVariable(Keys.WILDCARD, instanceName)
                .substituteVariable(name, instanceName);

        boolean found = false;
        boolean preservesState = stateChanges.stream().noneMatch(ObjectState::hasTo);
        if (preservesState) {
            // A state-preserving call accepts any of its entry states, including a disjunction of them.
            Predicate expected = stateChanges.stream().filter(ObjectState::hasFrom).map(ObjectState::getFrom)
                    .reduce(Predicate.createLit("false", Types.BOOLEAN), Predicate::createDisjunction)
                    .substituteVariable(Keys.THIS, instanceName);
            Predicate previous = substituteArguments(prevState, map);
            expected = substituteArguments(expected, map).changeOldMentions(vi.getName(), instanceName);
            try {
                found = tc.checkStateSMT(previous, expected, invocation.getPosition());
            } catch (SMTUnknownError error) {
                error.setDeclarationPosition(stateChanges.stream().map(ObjectState::getFromPosition)
                        .filter(Objects::nonNull).findFirst().orElse(function.getPlacementInCode().getPosition()));
                throw error;
            }
        }
        for (ObjectState stateChange : stateChanges) { // TODO: only working for 1 state annotation
            if (found || preservesState)
                break;
            if (!stateChange.hasFrom())
                continue;

            Predicate expectState = entryState(stateChange, instanceName, map);
            Predicate prevCheck = substituteArguments(prevState, map);

            try {
                found = tc.checkStateSMT(prevCheck, expectState, invocation.getPosition());
            } catch (SMTUnknownError error) {
                error.setDeclarationPosition(stateChange.getFromPosition());
                throw error;
            }
            if (found && stateChange.hasTo()) {
                String newInstanceName = String.format(Formats.INSTANCE, name, tc.getContext().getCounter());
                Predicate transitionedState = transitionedState(stateChange, instanceName, newInstanceName, map,
                        invocation);
                // update of state of new instance of this#n#(whatever it was + 1)
                addInstanceWithState(tc, name, newInstanceName, vi, transitionedState, invocation)
                        .setStateChange(new StateChange(function, stateChange, vi, invocation.getPosition()));
            }
        }
        if (!found && !preservesState)
            found = changeToStateOfAnyEntryState(tc, vi, function, name, map, invocation, prevState);
        if (!found) { // Reaches the end of stateChange no matching states
            Predicate expectedStatesDisjunction = stateChanges.stream().filter(ObjectState::hasFrom)
                    .map(ObjectState::getFrom)
                    .reduce(Predicate.createLit("false", Types.BOOLEAN), Predicate::createDisjunction);
            expectedStatesDisjunction = substituteArguments(
                    expectedStatesDisjunction.substituteVariable(Keys.THIS, instanceName), map)
                            .changeOldMentions(vi.getName(), instanceName);

            // combine messages of all state changes
            String message = stateChanges.stream().map(ObjectState::getMessage)
                    .filter(msg -> msg != null && !msg.isBlank()).distinct().collect(Collectors.joining("\n"));
            SourcePosition declarationPosition = stateChanges.stream().filter(ObjectState::hasFrom)
                    .map(ObjectState::getFromPosition).filter(Objects::nonNull).findFirst()
                    .orElse(function.getPlacementInCode().getPosition());
            List<GhostState> ghostStates = getGhostStatesFor(function.getTargetClass(), tc);
            Optional<DecidingArgument> deciding = decidingArgument(tc, vi, function, ghostStates, invocation);
            Optional<ReceiverStates> states = receiverStates(tc, function, instanceName, prevState, invocation);
            ErrorCategory category = deciding.isPresent() ? ErrorCategory.ARGUMENT_PICKS_THE_STATE
                    : states.flatMap(
                            r -> StateErrorCategories.categorize(r.machine(), r.current(), prevState, ghostStates))
                            .orElse(null);
            List<Hint> hints = deciding.isPresent()
                    ? receiverOf(invocation).map(r -> StateHints.decidingArgument(deciding.get(), r)).stream().toList()
                    : states.map(r -> stateHints(r, function, category, invocation)).orElse(List.of());
            StateErrorContext context = states
                    .map(r -> StateErrorContext.of(r.machine(), function, r.current(), writtenReceiver(invocation)))
                    .orElse(null);
            tc.throwStateRefinementError(invocation.getPosition(), declarationPosition, prevState,
                    expectedStatesDisjunction, message, hints, category, context);
        }
    }

    /** The state machine of the receiver's class and the states the receiver can be in before a call */
    private record ReceiverStates(StateMachine machine, Set<State> current) {
    }

    /**
     * The receiver's states before a call to {@code function} that its state does not allow. Best effort: if they
     * cannot be computed, the error is reported without hints or category
     */
    private static Optional<ReceiverStates> receiverStates(TypeChecker tc, RefinedFunction function,
            String instanceName, Predicate state, CtElement invocation) {
        try {
            Optional<StateMachine> machine = tc.getStateMachine(function.getTargetClass(), invocation);
            if (machine.isEmpty())
                return Optional.empty();
            Set<State> current = new StateSolver(tc, invocation.getPosition()).possibleStates(machine.get().states(),
                    state, instanceName);
            return Optional.of(new ReceiverStates(machine.get(), current));
        } catch (RuntimeException e) {
            // a failure to analyse the states must not replace the error being reported
            DebugLog.warn("Could not compute the receiver's states: " + e.getMessage());
            return Optional.empty();
        }
    }

    /** Best effort, like {@link #receiverStates} */
    private static Optional<DecidingArgument> decidingArgument(TypeChecker tc, VariableInstance receiver,
            RefinedFunction function, List<GhostState> ghostStates, CtElement invocation) {
        try {
            return DecidingArgument.find(tc, receiver, function, ghostStates, invocation);
        } catch (RuntimeException e) {
            DebugLog.warn("Could not find the argument that decided the receiver's state: " + e.getMessage());
            return Optional.empty();
        }
    }

    /** Hints on how to fix a call to {@code function} made in a state that does not allow it */
    private static List<Hint> stateHints(ReceiverStates states, RefinedFunction function, ErrorCategory category,
            CtElement invocation) {
        Optional<String> receiver = receiverOf(invocation);
        if (receiver.isEmpty() || !StateMachineBuilder.canModel(function))
            return List.of();
        return StateHints.forCall(states.machine(), function, states.current(), receiver.get(), category);
    }

    /**
     * The receiver of {@code invocation} as written in the code, if it names an object that calls can be added to: a
     * variable, a field or {@code this}
     */
    private static Optional<String> receiverOf(CtElement invocation) {
        if (!(invocation instanceof CtInvocation<?> call))
            return Optional.empty();
        CtExpression<?> target = call.getTarget();
        if (target == null || target.isImplicit() || target instanceof CtVariableRead<?>
                || target instanceof CtThisAccess<?>)
            return Optional.of(writtenReceiver(invocation));
        return Optional.empty();
    }

    /** The receiver of {@code invocation} as written in the code, {@code this} if it is implicit */
    private static String writtenReceiver(CtElement invocation) {
        CtExpression<?> target = invocation instanceof CtInvocation<?> call ? call.getTarget() : null;
        return target == null || target.isImplicit() ? Keys.THIS : target.toString();
    }

    /** The state that {@code stateChange} gives the object after the call, as the instance {@code newInstanceName} */
    private static Predicate transitionedState(ObjectState stateChange, String instanceName, String newInstanceName,
            Map<String, String> map, CtElement invocation) {
        // Non-void: `_` is the return value; void: legacy alias for the new instance.
        String returnViName = (String) invocation.getMetadata(Keys.RETURN_VAR_NAME);
        String wildcardTarget = returnViName != null ? returnViName : newInstanceName;
        Predicate transitionedState = stateChange.getTo().substituteVariable(Keys.WILDCARD, wildcardTarget)
                .substituteVariable(Keys.THIS, newInstanceName);
        return checkOldMentions(substituteArguments(transitionedState, map), instanceName, newInstanceName);
    }

    /**
     * A call whose entry states are each unprovable but whose union is provable, e.g. on an object that is in one of
     * two states after a loop. The call is allowed, and the object ends up in the state given by any alternative whose
     * entry state it may be in: an alternative without a target state leaves the object as it was.
     *
     * @return whether the call is allowed
     */
    private static boolean changeToStateOfAnyEntryState(TypeChecker tc, VariableInstance vi, RefinedFunction function,
            String name, Map<String, String> map, CtElement invocation, Predicate prevState) throws LJError {
        List<ObjectState> alternatives = function.getAllStates().stream().filter(ObjectState::hasFrom).toList();
        if (alternatives.size() < 2)
            return false;
        String instanceName = vi.getName();
        Predicate previous = substituteArguments(prevState, map);
        Predicate anyEntry = Predicate.createLit("false", Types.BOOLEAN);
        for (ObjectState alternative : alternatives)
            anyEntry = Predicate.createDisjunction(anyEntry, entryState(alternative, instanceName, map));
        try {
            if (!tc.checkStateSMT(previous, anyEntry, invocation.getPosition()))
                return false;
        } catch (SMTUnknownError error) {
            error.setDeclarationPosition(alternatives.get(0).getFromPosition());
            throw error;
        }

        String newInstanceName = String.format(Formats.INSTANCE, name, tc.getContext().getCounter());
        Predicate unchanged = previous.substituteVariable(instanceName, newInstanceName);
        Predicate after = Predicate.createLit("false", Types.BOOLEAN);
        for (ObjectState alternative : alternatives) {
            Predicate entry = entryState(alternative, instanceName, map);
            Predicate exit = alternative.hasTo()
                    ? transitionedState(alternative, instanceName, newInstanceName, map, invocation)
                    : Predicate.createConjunction(entry.substituteVariable(instanceName, newInstanceName), unchanged);
            after = Predicate.createDisjunction(after, Predicate.createConjunction(entry, exit));
        }
        addInstanceWithState(tc, name, newInstanceName, vi, after, invocation);
        return true;
    }

    /** The entry state of {@code stateChange}, for the object {@code instanceName} */
    private static Predicate entryState(ObjectState stateChange, String instanceName, Map<String, String> map) {
        Predicate entry = stateChange.getFrom().substituteVariable(Keys.THIS, instanceName);
        return substituteArguments(entry, map).changeOldMentions(instanceName, instanceName);
    }

    /** Replaces the parameters of the called method with the arguments of the call, as given by {@code map} */
    private static Predicate substituteArguments(Predicate predicate, Map<String, String> map) {
        for (String parameter : map.keySet())
            predicate = predicate.substituteVariable(parameter, map.get(parameter));
        return predicate;
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

            addInstanceWithState(tc, name, newInstanceName, variableInstance, c, invocation)
                    .setStateChange(variableInstance.getStateChange().orElse(null));
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
    private static VariableInstance addInstanceWithState(TypeChecker tc, String superName, String name2,
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
                tc.checkSMT(superC, invocation, rv.getPlacementInCode().getAnnotationPosition(), null);
                tc.getContext().addRefinementInstanceToVariable(superName, name2);
            }
        }
        invocation.putMetadata(Keys.TARGET, vi2);
        return vi2;
    }

    /** Whether the access denotes the receiver of the enclosing type, including unqualified super. */
    public static boolean isCurrentReceiver(CtElement target) {
        CtType<?> owner = target.getParent(CtType.class);
        if (owner == null)
            return false;
        if (target instanceof CtThisAccess<?> self)
            return self.getType() != null && self.getType().getQualifiedName().equals(owner.getQualifiedName());
        if (target instanceof CtSuperAccess<?> parent)
            return parent.getTarget() == null || parent.getTarget().isImplicit();
        return false;
    }

    /**
     * Gets the name of the parent target and adds the closest target to the elem TARGET metadata
     *
     * @param invocation
     *
     * @return the name of the parent target
     */
    public static String prepareInvocationTarget(TypeChecker tc, CtElement target2, CtElement invocation) {
        if (target2 instanceof CtVariableRead<?> || isCurrentReceiver(target2)) {
            // v--------- field read
            // means invocation is in a form of `t.method(args)`
            String name = target2 instanceof CtVariableRead<?> v ? v.getVariable().getSimpleName() : Keys.THIS;
            if (target2 instanceof CtFieldRead<?> fieldRead && fieldRead.getTarget() instanceof CtThisAccess<?>) {
                String fieldName = Utils.qualifyFieldName(fieldRead.getVariable());
                if (tc.getContext().hasVariable(fieldName))
                    name = fieldName;
            }
            Optional<VariableInstance> invocationCallee = tc.getContext().getLastVariableInstance(name);
            if (invocationCallee.isPresent()) {
                invocation.putMetadata(Keys.TARGET, invocationCallee.get());
            } else if (target2.getMetadata(Keys.TARGET) == null) {
                RefinedVariable var = tc.getContext().getVariableByName(name);
                if (var == null)
                    return name;

                String nName = String.format(Formats.INSTANCE, name, tc.getContext().getCounter());
                RefinedVariable rv = tc.getContext().addInstanceToContext(nName, var.getType(),
                        var.getRefinement().substituteVariable(name, nName), target2);
                tc.getContext().addRefinementInstanceToVariable(name, nName);
                invocation.putMetadata(Keys.TARGET, rv);
            }

            return name;
        } else if (target2 instanceof CtConstructorCall<?> newCall
                && newCall.getMetadata(Keys.RECEIVER)instanceof RefinedVariable receiver) {
            // `new T(args).method()` already prepared: reuse its receiver instead of creating another one
            invocation.putMetadata(Keys.TARGET, receiver);
            return receiver.getName();
        } else if (target2 instanceof CtConstructorCall<?> newCall
                && newCall.getMetadata(Keys.REFINEMENT)instanceof Predicate state) {
            // `new T(args).method()`: the receiver is a fresh object in the state the constructor gives. Must come
            // before the TARGET case: a constructor with refined parameters puts its (void) return value in TARGET
            String name = String.format(Formats.FRESH, tc.getContext().getCounter());
            RefinedVariable receiver = tc.getContext().addInstanceToContext(name, newCall.getType(),
                    state.substituteVariable(Keys.THIS, name).substituteVariable(Keys.WILDCARD, name), newCall);
            newCall.putMetadata(Keys.TARGET, receiver);
            newCall.putMetadata(Keys.RECEIVER, receiver);
            invocation.putMetadata(Keys.TARGET, receiver);
            return name;
        } else if (target2 instanceof CtInvocation<?> call
                && call.getMetadata(Keys.RECEIVER)instanceof RefinedVariable receiver) {
            invocation.putMetadata(Keys.TARGET, receiver);
            return receiver.getName();
        } else if (target2 instanceof CtInvocation<?> call && call.getMetadata(Keys.TARGET) != null
                && !isReturnValue(call)) {
            // `a.get().method()` where get() has no return refinement: the call's TARGET is its own receiver (a), not
            // the value it returns, so the receiver is a fresh value of the call's type, in a state not known here
            String name = String.format(Formats.FRESH, tc.getContext().getCounter());
            RefinedVariable receiver = tc.getContext().addInstanceToContext(name, call.getType(), new Predicate(),
                    call);
            call.putMetadata(Keys.RECEIVER, receiver);
            invocation.putMetadata(Keys.TARGET, receiver);
            return name;
        } else if (target2.getMetadata(Keys.TARGET) != null) {
            VariableInstance target2Vi = (VariableInstance) target2.getMetadata(Keys.TARGET);
            Optional<Variable> v = target2Vi.getParent();
            invocation.putMetadata(Keys.TARGET, target2Vi);
            return v.map(Refined::getName).orElse(target2Vi.getName());
        }
        return null;
    }

    /** Whether a call's TARGET metadata is the value it returns (set when its return refinement is applied). */
    private static boolean isReturnValue(CtInvocation<?> call) {
        return call.getMetadata(Keys.TARGET)instanceof VariableInstance vi
                && vi.getName().equals(call.getMetadata(Keys.RETURN_VAR_NAME));
    }

    static VariableInstance getTarget(CtElement invocation) {
        if (invocation.getMetadata(Keys.TARGET) != null) {
            return (VariableInstance) invocation.getMetadata(Keys.TARGET);
        }
        return null;
    }

    private static List<CtAnnotation<? extends Annotation>> getStateAnnotations(CtElement element) {
        return element.getAnnotations().stream().filter(ann -> ann.getAnnotationType().getQualifiedName()
                .contentEquals("liquidjava.specification.StateRefinement")).collect(Collectors.toList());
    }
}
