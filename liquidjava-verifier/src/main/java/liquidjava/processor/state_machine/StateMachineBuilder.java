package liquidjava.processor.state_machine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import liquidjava.diagnostics.errors.LJError;
import liquidjava.processor.context.Context;
import liquidjava.processor.context.GhostState;
import liquidjava.processor.context.ObjectState;
import liquidjava.processor.context.RefinedFunction;
import liquidjava.processor.context.Variable;
import liquidjava.processor.refinement_checker.TypeChecker;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.FunctionInvocation;
import liquidjava.rj_language.ast.Ite;
import liquidjava.rj_language.ast.UnaryExpression;
import liquidjava.utils.Utils;
import liquidjava.utils.constants.Keys;
import liquidjava.utils.constants.Types;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtModifiable;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeParameterReference;
import spoon.reflect.reference.CtTypeReference;

/**
 * Builds the {@link StateMachine} of a class from its {@code @StateSet}s and {@code @StateRefinement}s, and those of
 * its supertypes, which it inherits. Every transition is proven by the SMT solver, so the machine never contains a call
 * that would not verify. Methods whose state refinements also mention their parameters or result are kept as
 * {@link CallVariant}s, whose effects the solver works out only when a hint needs them, and boolean methods whose
 * return refinement relates their result to the state are kept as checks of that result. The machine is incomplete if a
 * method has variants or is left out because its refinements mention anything else, as it is when a subclass has state
 * refinements of its own or an inherited refinement may not be the one the verifier applies.
 */
public class StateMachineBuilder {

    /** Classes with more states than this get no machine, as building it would take too many solver calls */
    private static final int MAX_STATES = 16;

    // two objects of the class, before and after a call, that the rest of the program knows nothing about
    private static final String BEFORE = SolverEffects.BEFORE;
    private static final String AFTER = SolverEffects.AFTER;
    // the parameters and result of a call, under names nothing else uses
    private static final String ARGUMENT = "#argument_";
    private static final String RESULT = "#result";

    private final TypeChecker tc;
    private final StateSolver solver;
    private final List<State> states;
    private final List<GhostState> ghostStates;
    private final CtElement element;

    private StateMachineBuilder(TypeChecker tc, StateSolver solver, List<State> states, List<GhostState> ghostStates,
            CtElement element) {
        this.tc = tc;
        this.solver = solver;
        this.states = states;
        this.ghostStates = ghostStates;
        this.element = element;
    }

    /** Whether the transitions of {@code function} can be part of a machine: they only depend on the state of this */
    public static boolean canModel(RefinedFunction function) {
        return function.getAllStates().stream().flatMap(change -> Stream.of(change.getFrom(), change.getTo()))
                .flatMap(p -> p.getVariableNames().stream()).allMatch(Keys.THIS::equals);
    }

    /**
     * Builds the state machine of {@code className}
     *
     * @param element
     *            the element being verified, where the fresh objects are declared
     *
     * @return the machine, or nothing if the class has no state sets or too many states
     */
    public static Optional<StateMachine> build(TypeChecker tc, StateSolver solver, String className, CtElement element)
            throws LJError {
        CtTypeReference<?> type = tc.getFactory().Type().createReference(className);
        List<CtTypeReference<?>> supertypes = supertypesOf(type);
        List<GhostState> ghostStates = ghostStatesOf(tc.getContext(), type, supertypes);
        List<State> states = statesOf(ghostStates, false);
        if (states.isEmpty() || states.size() > MAX_STATES)
            return Optional.empty();
        Context context = tc.getContext();
        context.enterContext();
        try {
            context.addVarToContext(BEFORE, type, new Predicate(), element);
            context.addVarToContext(AFTER, type, new Predicate(), element);
            StateMachineBuilder builder = new StateMachineBuilder(tc, solver, states, ghostStates, element);
            return Optional.of(builder.build(type, supertypes));
        } finally {
            context.exitContext();
        }
    }

    /** The ghost states of {@code className} and of its supertypes, which its objects also have */
    public static List<GhostState> ghostStatesOf(TypeChecker tc, String className) {
        CtTypeReference<?> type = tc.getFactory().Type().createReference(className);
        return ghostStatesOf(tc.getContext(), type, supertypesOf(type));
    }

    private static List<GhostState> ghostStatesOf(Context context, CtTypeReference<?> type,
            List<CtTypeReference<?>> supertypes) {
        // a state set can be registered more than once, so states are told apart by name, and states are kept by
        // simple class name, so those of another class with the same simple name are left out
        Map<String, GhostState> ghostStates = new LinkedHashMap<>();
        for (CtTypeReference<?> t : Stream.concat(Stream.of(type), supertypes.stream()).toList()) {
            List<GhostState> declared = context.getGhostState(Utils.getSimpleName(t.getQualifiedName()));
            if (declared != null)
                declared.stream().filter(g -> t.getQualifiedName().equals(g.getPrefix()))
                        .forEach(g -> ghostStates.putIfAbsent(g.getQualifiedName(), g));
        }
        return List.copyOf(ghostStates.values());
    }

    /** Every supertype of {@code type}, nearest first: its superclasses, then the interfaces they implement */
    private static List<CtTypeReference<?>> supertypesOf(CtTypeReference<?> type) {
        List<CtTypeReference<?>> superclasses = new ArrayList<>();
        Deque<CtTypeReference<?>> interfaces = new ArrayDeque<>();
        Set<String> seen = new HashSet<>(Set.of(type.getQualifiedName()));
        for (CtTypeReference<?> t = type; t != null; t = superclassOf(t)) {
            if (t != type && seen.add(t.getQualifiedName()))
                superclasses.add(t);
            interfaces.addAll(superInterfacesOf(t));
        }
        List<CtTypeReference<?>> supertypes = new ArrayList<>(superclasses);
        while (!interfaces.isEmpty()) {
            CtTypeReference<?> t = interfaces.poll();
            if (seen.add(t.getQualifiedName())) {
                supertypes.add(t);
                interfaces.addAll(superInterfacesOf(t));
            }
        }
        return supertypes;
    }

    // a supertype that cannot be resolved (no source, not on the classpath) ends the walk on that branch
    private static CtTypeReference<?> superclassOf(CtTypeReference<?> type) {
        try {
            return type.getSuperclass();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Set<CtTypeReference<?>> superInterfacesOf(CtTypeReference<?> type) {
        try {
            return type.getSuperInterfaces();
        } catch (RuntimeException e) {
            return Set.of();
        }
    }

    /**
     * Every combination of one state from each state set of the class
     *
     * @param withNone
     *            whether an object may also be in none of the states of a set, named {@code none} (e.g. after
     *            {@code @StateRefinement(to = "!open(this) && !closed(this)")})
     */
    public static List<State> statesOf(Context context, String className, boolean withNone) {
        List<GhostState> ghostStates = context.getGhostState(Utils.getSimpleName(className));
        return ghostStates == null ? List.of() : statesOf(ghostStates, withNone);
    }

    private static List<State> statesOf(List<GhostState> ghostStates, boolean withNone) {
        Map<String, List<GhostState>> stateSets = ghostStates.stream().filter(g -> g.getParent() != null).collect(
                Collectors.groupingBy(g -> g.getParent().getQualifiedName(), LinkedHashMap::new, Collectors.toList()));
        if (stateSets.isEmpty())
            return List.of();

        List<State> combinations = List.of(new State(List.of(), new Predicate()));
        for (List<GhostState> stateSet : stateSets.values()) {
            List<State> inSet = Stream.concat(stateSet.stream().map(StateMachineBuilder::inState),
                    withNone ? Stream.of(inNone(stateSet)) : Stream.empty()).toList();
            combinations = combinations.stream().flatMap(s -> inSet.stream().map(a -> combine(s, a))).toList();
        }
        return combinations;
    }

    /** The states of each state set of the class on its own, in the order the sets are declared */
    private List<List<State>> stateSets() {
        return ghostStates.stream().filter(g -> g.getParent() != null)
                .collect(Collectors.groupingBy(g -> g.getParent().getQualifiedName(), LinkedHashMap::new,
                        Collectors.mapping(StateMachineBuilder::inState, Collectors.toList())))
                .values().stream().toList();
    }

    private static State inState(GhostState ghostState) {
        return new State(List.of(ghostState.getName()),
                Predicate.createInvocation(ghostState.getQualifiedName(), Predicate.createVar(Keys.THIS)));
    }

    private static State inNone(List<GhostState> stateSet) {
        Predicate none = stateSet.stream().map(g -> inState(g).predicate().negate())
                .reduce(Predicate::createConjunction).orElseThrow();
        return new State(List.of("none"), none);
    }

    private static State combine(State state, State inSet) {
        List<String> names = new ArrayList<>(state.names());
        names.addAll(inSet.names());
        return new State(names, Predicate.createConjunction(state.predicate(), inSet.predicate()));
    }

    private StateMachine build(CtTypeReference<?> type, List<CtTypeReference<?>> supertypes) throws LJError {
        String className = type.getQualifiedName();
        Set<String> inherited = supertypes.stream().map(CtTypeReference::getQualifiedName).collect(Collectors.toSet());
        List<RefinedFunction> methods = new ArrayList<>();
        List<Transition> transitions = new ArrayList<>();
        Map<RefinedFunction, Set<State>> constructors = new LinkedHashMap<>();
        List<CallVariant> variants = new ArrayList<>();
        boolean complete = !subclassChangesState(className, type, inherited);
        for (RefinedFunction function : tc.getContext().getCtxFunctions()) {
            boolean query = !function.hasStateChange() && isQuery(function);
            if (!function.hasStateChange() && !query)
                continue;
            boolean constructor = CtExecutableReference.CONSTRUCTOR_NAME.equals(function.getName());
            boolean own = className.equals(function.getTargetClass());
            if (own && constructor) {
                if (abstractType(type) || privateConstructor(function, type))
                    continue; // cannot be suggested as a new object
                Optional<Set<State>> created = createdStates(function);
                created.ifPresent(s -> constructors.put(function, s));
                continue;
            }
            if (!own && (constructor || !inherited.contains(function.getTargetClass())))
                continue;
            if (!own && overridden(function, className))
                continue; // the class's own refinement for it is the one in the machine
            boolean applies = own ? appliesToOwnType(function, type, supertypes)
                    : resolvesTo(function, type, supertypes) && publiclyInherited(function);
            if (!applies) {
                if (!query)
                    complete = false;
                continue;
            }
            if (query) {
                variants.addAll(queryVariants(function, type));
                continue;
            }
            if (!canModel(function)) {
                complete = false;
                variants.addAll(variantsOf(function, type));
                continue;
            }
            Optional<List<Transition>> calls = transitionsOf(function);
            if (calls.isPresent()) {
                methods.add(function);
                transitions.addAll(calls.get());
            } else {
                complete = false;
            }
        }
        return new StateMachine(states, methods, transitions, constructors, complete, variants);
    }

    /**
     * Whether {@code function} is a boolean method without state refinements whose return refinement relates its result
     * to the states of this, e.g. {@code _ == true --> hasMore(this)}
     */
    private boolean isQuery(RefinedFunction function) {
        Predicate ret = function.getRefReturn();
        if (ret == null || !returnsBoolean(function)
                || CtExecutableReference.CONSTRUCTOR_NAME.equals(function.getName()))
            return false;
        List<String> names = ret.getVariableNames();
        return names.contains(Keys.WILDCARD) && !ret.getStateInvocations(ghostStates).isEmpty()
                && names.stream().allMatch(n -> n.equals(Keys.THIS) || n.equals(Keys.WILDCARD));
    }

    /** The checks of the result of a query: true and false */
    private List<CallVariant> queryVariants(RefinedFunction function, CtTypeReference<?> type) {
        SolverEffects.Scope scope = new SolverEffects.Scope(tc, solver, states, stateSets(), type,
                Map.of(RESULT, function.getType()), element);
        Predicate ret = function.getRefReturn().substituteVariable(Keys.WILDCARD, RESULT);
        return resultConditions().stream().map(c -> new CallVariant(function, CallVariant.Kind.RESULT, c,
                SolverEffects.ofQuery(scope, ret, c.substituteVariable(Keys.WILDCARD, RESULT)))).toList();
    }

    /**
     * The ways to call {@code function}, whose state refinements also mention its parameters or result: with any
     * arguments; with arguments that meet each condition on them that its refinements test, and with arguments that do
     * not; and, for a boolean result its refinements mention, checking that the result is true or false. Nothing if the
     * refinements mention anything else.
     */
    private List<CallVariant> variantsOf(RefinedFunction function, CtTypeReference<?> type) {
        Map<String, String> renamed = new LinkedHashMap<>();
        Map<String, CtTypeReference<?>> free = new LinkedHashMap<>();
        for (Variable parameter : function.getArguments()) {
            renamed.put(parameter.getName(), ARGUMENT + parameter.getName());
            free.put(ARGUMENT + parameter.getName(), parameter.getType());
        }
        if (returnsValue(function)) {
            renamed.put(Keys.WILDCARD, RESULT);
            free.put(RESULT, function.getType());
        }
        List<ObjectState> changes = function.getAllStates().stream().filter(ObjectState::hasFrom).toList();
        List<Predicate> predicates = changes.stream().flatMap(c -> Stream.of(c.getFrom(), c.getTo()))
                .filter(Objects::nonNull).toList();
        Set<String> known = new HashSet<>(renamed.keySet());
        known.add(Keys.THIS);
        if (!predicates.stream().flatMap(p -> p.getVariableNames().stream()).allMatch(known::contains))
            return List.of();

        List<ObjectState> renamedChanges = changes.stream()
                .map(c -> new ObjectState(rename(c.getFrom(), renamed), c.hasTo() ? rename(c.getTo(), renamed) : null))
                .toList();
        SolverEffects.Scope scope = new SolverEffects.Scope(tc, solver, states, stateSets(), type, free, element);
        SolverEffects any = new SolverEffects(scope, renamedChanges, new Predicate(), false, null);
        List<CallVariant> variants = new ArrayList<>();
        variants.add(new CallVariant(function, CallVariant.Kind.ANY, new Predicate(), any));
        Set<String> parameters = function.getArguments().stream().map(Variable::getName).collect(Collectors.toSet());
        for (Predicate condition : argumentConditions(predicates, parameters))
            variants.add(new CallVariant(function, CallVariant.Kind.ARGUMENTS, condition,
                    new SolverEffects(scope, renamedChanges, rename(condition, renamed), false, null)));
        boolean resultMentioned = predicates.stream().anyMatch(p -> p.getVariableNames().contains(Keys.WILDCARD));
        if (returnsBoolean(function) && resultMentioned)
            for (Predicate condition : resultConditions())
                variants.add(new CallVariant(function, CallVariant.Kind.RESULT, condition,
                        new SolverEffects(scope, renamedChanges, rename(condition, renamed), true, any)));
        return variants;
    }

    private static List<Predicate> resultConditions() {
        Predicate result = Predicate.createVar(Keys.WILDCARD);
        return List.of(result, result.negate());
    }

    /**
     * The conditions on the parameters that {@code predicates} test, and their negations: the conditions of
     * {@code c ? a : b}, the left sides of {@code -->} and the operands of {@code &&} that mention parameters and
     * nothing else
     */
    private static List<Predicate> argumentConditions(List<Predicate> predicates, Set<String> parameters) {
        List<Expression> tested = new ArrayList<>();
        predicates.forEach(p -> collectTested(p.getExpression(), tested));
        Map<String, Predicate> conditions = new LinkedHashMap<>();
        for (Expression condition : tested) {
            List<String> mentioned = new ArrayList<>();
            condition.getVariableNames(mentioned);
            if (mentioned.isEmpty() || !parameters.containsAll(mentioned) || invokesFunction(condition))
                continue;
            for (Expression e : List.of(condition.clone(), negation(condition)))
                conditions.putIfAbsent(e.toString(), new Predicate(e));
        }
        return List.copyOf(conditions.values());
    }

    private static void collectTested(Expression e, List<Expression> tested) {
        if (e instanceof Ite ite)
            tested.add(ite.getCondition());
        else if (e instanceof BinaryExpression b && (b.getOperator().equals("-->") || b.getOperator().equals("&&")))
            tested.add(b.getFirstOperand());
        if (e instanceof BinaryExpression b && b.getOperator().equals("&&"))
            tested.add(b.getSecondOperand());
        e.getChildren().forEach(child -> collectTested(child, tested));
    }

    private static boolean invokesFunction(Expression e) {
        return e instanceof FunctionInvocation
                || e.getChildren().stream().anyMatch(StateMachineBuilder::invokesFunction);
    }

    private static final Map<String, String> NEGATED_COMPARISONS = Map.of("==", "!=", "!=", "==", "<", ">=", ">=", "<",
            ">", "<=", "<=", ">");

    /** The negation of {@code e}, written without {@code !} when it is a comparison or a negation */
    private static Expression negation(Expression e) {
        if (e instanceof BinaryExpression b && NEGATED_COMPARISONS.containsKey(b.getOperator()))
            return new BinaryExpression(b.getFirstOperand().clone(), NEGATED_COMPARISONS.get(b.getOperator()),
                    b.getSecondOperand().clone());
        if (e instanceof UnaryExpression u && u.getOp().equals("!"))
            return u.getExpression().clone();
        return new UnaryExpression("!", e.clone());
    }

    private static Predicate rename(Predicate p, Map<String, String> renamed) {
        for (Map.Entry<String, String> e : renamed.entrySet())
            p = p.substituteVariable(e.getKey(), e.getValue());
        return p;
    }

    private static boolean returnsBoolean(RefinedFunction function) {
        return function.getType() != null && Types.BOOLEAN.equals(function.getType().toString());
    }

    static boolean returnsValue(RefinedFunction function) {
        return function.getType() != null && !"void".equals(function.getType().toString());
    }

    /** Whether a subclass has state refinements of its own, which the machine does not include */
    private boolean subclassChangesState(String className, CtTypeReference<?> type, Set<String> inherited) {
        return tc.getContext().getCtxFunctions().stream().filter(RefinedFunction::hasStateChange)
                .map(RefinedFunction::getTargetClass)
                .filter(c -> c != null && !c.equals(className) && !inherited.contains(c)).distinct()
                .map(c -> tc.getFactory().Type().createReference(c)).anyMatch(other -> other.isSubtypeOf(type));
    }

    /**
     * Whether the verifier checks a call to {@code method}, specified for {@code type}, on an object of that type. It
     * looks the refinement up by the receiver's type, which needs the parameter types to match those of the method it
     * resolves to, and otherwise by the type that declares that method. A parameter whose type is a type variable may
     * not match, so then {@code type} must declare the method.
     */
    private boolean appliesToOwnType(RefinedFunction method, CtTypeReference<?> type,
            List<CtTypeReference<?>> supertypes) {
        boolean concreteParameters = method.getArguments().stream()
                .noneMatch(a -> a.getType() instanceof CtTypeParameterReference);
        return concreteParameters || resolvesTo(method, type, supertypes);
    }

    /** Whether {@code className} has its own refinement for {@code method}, with the same parameter types */
    private boolean overridden(RefinedFunction method, String className) {
        String name = Utils.getSimpleName(method.getName());
        List<String> parameters = parameterTypes(method);
        return tc.getContext().getCtxFunctions().stream().anyMatch(f -> className.equals(f.getTargetClass())
                && name.equals(Utils.getSimpleName(f.getName())) && parameters.equals(parameterTypes(f)));
    }

    private static List<String> parameterTypes(RefinedFunction function) {
        return function.getArguments().stream().map(a -> typeName(a.getType())).toList();
    }

    /**
     * Whether the nearest type in the hierarchy of {@code type} that declares {@code method} is the one it is specified
     * for, which is where the verifier looks up the refinement of a call. Methods are matched by name and number of
     * parameters, so an overload can hide an inherited method, which only leaves the machine incomplete.
     */
    private boolean resolvesTo(RefinedFunction method, CtTypeReference<?> type, List<CtTypeReference<?>> supertypes) {
        for (CtTypeReference<?> t : Stream.concat(Stream.of(type), supertypes.stream()).toList()) {
            CtType<?> declaration = t.getTypeDeclaration();
            if (declaration == null)
                return false; // cannot tell which type declares it
            if (declarationsOf(method, declaration).findAny().isPresent())
                return t.getQualifiedName().equals(method.getTargetClass());
        }
        return false;
    }

    /**
     * Whether any caller can call {@code method} on an object of a subtype of the type it is specified for. The machine
     * is shared by every caller, so a method that only some callers can reach (private, package-private or protected)
     * is left out, which only leaves the machine incomplete.
     */
    private boolean publiclyInherited(RefinedFunction method) {
        CtType<?> declaring = tc.getFactory().Type().createReference(method.getTargetClass()).getTypeDeclaration();
        if (declaring == null)
            return false; // cannot tell whether it is public
        // interface methods are public unless declared private
        return declarationsOf(method, declaring).allMatch(m -> declaring.isInterface() ? !m.isPrivate() : m.isPublic());
    }

    /** The methods of {@code type} that {@code method} may refer to: those with its name and number of parameters */
    private static Stream<CtMethod<?>> declarationsOf(RefinedFunction method, CtType<?> type) {
        String name = Utils.getSimpleName(method.getName());
        int arity = method.getArguments().size();
        return type.getMethods().stream()
                .filter(m -> m.getSimpleName().equals(name) && m.getParameters().size() == arity);
    }

    private static boolean abstractType(CtTypeReference<?> type) {
        CtType<?> declaration = type.getTypeDeclaration();
        return declaration == null || declaration.isInterface() || declaration.isAbstract();
    }

    /**
     * Whether {@code constructor} cannot be called from outside its class: it is private, or the class is an enum. A
     * constructor that is not found in the class's declaration is assumed callable. The machine is shared by every
     * caller, so a private constructor is left out even for calls inside its class.
     */
    private static boolean privateConstructor(RefinedFunction constructor, CtTypeReference<?> type) {
        try {
            CtType<?> declaration = type.getTypeDeclaration();
            if (declaration.isEnum())
                return true;
            if (!(declaration instanceof CtClass<?> declared))
                return false;
            List<String> parameters = parameterTypes(constructor);
            return declared.getConstructors().stream()
                    .filter(c -> c.getParameters().stream().map(p -> typeName(p.getType())).toList().equals(parameters))
                    .anyMatch(CtModifiable::isPrivate);
        } catch (RuntimeException e) {
            return false; // a declaration that cannot be built is not evidence that the constructor is private
        }
    }

    private static String typeName(CtTypeReference<?> type) {
        return type == null ? "" : type.getQualifiedName();
    }

    /** The transitions of {@code method}, or nothing if they depend on more than the state of {@code this} */
    private Optional<List<Transition>> transitionsOf(RefinedFunction method) throws LJError {
        if (!canModel(method))
            return Optional.empty();
        List<Transition> transitions = new ArrayList<>();
        List<List<State>> sets = stateSets();
        List<ObjectState> changes = method.getAllStates();
        // whether a from holds depends only on the state sets it names: the others are other ghost functions
        List<Map<List<String>, CallVariant.Entry>> entries = changes.stream()
                .map(c -> new HashMap<List<String>, CallVariant.Entry>()).collect(Collectors.toList());
        List<Map<List<String>, Set<List<String>>>> afters = changes.stream()
                .map(c -> new HashMap<List<String>, Set<List<String>>>()).collect(Collectors.toList());
        for (State state : states) {
            Predicate current = state.predicate().substituteVariable(Keys.THIS, BEFORE);
            for (int i = 0; i < changes.size(); i++) {
                ObjectState change = changes.get(i);
                if (!change.hasFrom())
                    continue;
                List<String> named = StateMachine.namedSets(change.getFrom(), sets).stream().map(state.names()::get)
                        .toList();
                CallVariant.Entry entry = entries.get(i).get(named);
                if (entry == null) {
                    entry = entry(current, change.getFrom().substituteVariable(Keys.THIS, BEFORE));
                    entries.get(i).put(named, entry);
                }
                if (entry == CallVariant.Entry.HOLDS) {
                    // like the verifier, the first state change whose precondition holds is the one applied
                    Set<State> next = statesAfter(change, state, current, sets, afters.get(i));
                    if (next.isEmpty())
                        return Optional.empty();
                    transitions.add(new Transition(state, method, change, next));
                    break;
                }
                if (entry == CallVariant.Entry.UNDECIDED)
                    return Optional.empty(); // allowed only sometimes in this state
            }
        }
        return Optional.of(transitions);
    }

    private CallVariant.Entry entry(Predicate current, Predicate from) throws LJError {
        if (solver.implies(current, from))
            return CallVariant.Entry.HOLDS;
        return solver.compatible(current, from) ? CallVariant.Entry.UNDECIDED : CallVariant.Entry.FAILS;
    }

    /**
     * The states {@code change} can lead to from {@code state}. The verifier keeps the state of the sets its {@code to}
     * does not name, and the states it can lead to in the others depend only on the state before in the sets it names
     * for {@code old(this)}, so they are asked once for each and kept in {@code known}
     */
    private Set<State> statesAfter(ObjectState change, State state, Predicate current, List<List<State>> sets,
            Map<List<String>, Set<List<String>>> known) throws LJError {
        if (!change.hasTo())
            return Set.of(state);
        List<Integer> named = StateMachine.namedSets(change.getTo(), sets).stream().sorted().toList();
        List<State> candidates = StateMachine.keeping(states, state, Set.copyOf(named));
        List<String> before = StateMachine.namedSetsBefore(change.getTo(), sets).stream().sorted()
                .map(state.names()::get).toList();
        if (!known.containsKey(before)) {
            Predicate to = change.getTo().substituteVariable(Keys.THIS, AFTER).changeOldMentions(BEFORE, AFTER);
            known.put(before, solver.possibleStates(candidates, Predicate.createConjunction(current, to), AFTER)
                    .stream().map(s -> named.stream().map(s.names()::get).toList()).collect(Collectors.toSet()));
        }
        Set<List<String>> after = known.get(before);
        return candidates.stream().filter(c -> after.contains(named.stream().map(c.names()::get).toList()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** The states {@code constructor} can create the object in, or nothing if they depend on its arguments */
    private Optional<Set<State>> createdStates(RefinedFunction constructor) throws LJError {
        List<Predicate> toStates = constructor.getToStates();
        if (!canModel(constructor) || toStates.isEmpty())
            return Optional.empty();
        Predicate to = toStates.get(0).substituteVariable(Keys.THIS, AFTER);
        Set<State> created = solver.possibleStates(states, to, AFTER);
        return created.isEmpty() ? Optional.empty() : Optional.of(created);
    }
}
