package liquidjava.processor.refinement_checker;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import liquidjava.diagnostics.Diagnostics;
import liquidjava.diagnostics.errors.*;
import liquidjava.diagnostics.categories.ErrorCategory;
import liquidjava.diagnostics.hints.Hint;
import liquidjava.diagnostics.warnings.CustomWarning;
import liquidjava.diagnostics.warnings.UnsatisfiableRefinementWarning;
import liquidjava.processor.context.AliasWrapper;
import liquidjava.processor.context.Context;
import liquidjava.processor.context.GhostFunction;
import liquidjava.processor.context.GhostState;
import liquidjava.processor.context.RefinedVariable;
import liquidjava.processor.facade.AliasDTO;
import liquidjava.processor.facade.GhostDTO;
import liquidjava.processor.state_machine.StateMachine;
import liquidjava.processor.state_machine.StateMachineBuilder;
import liquidjava.processor.state_machine.StateSolver;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.StateValues;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.FunctionInvocation;
import liquidjava.rj_language.ast.Var;
import liquidjava.rj_language.ast.formatter.FieldDisplayNames;
import liquidjava.rj_language.parsing.RefinementsParser;
import liquidjava.diagnostics.DebugLog;
import liquidjava.smt.Counterexample;
import liquidjava.smt.SMTEvaluator;
import liquidjava.smt.SMTResult;
import liquidjava.utils.Utils;
import liquidjava.utils.constants.Formats;
import liquidjava.utils.constants.Keys;
import liquidjava.utils.constants.Types;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtTypedElement;
import spoon.reflect.declaration.CtVariable;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtScanner;

import static liquidjava.processor.refinement_checker.TypeCheckingUtils.*;

public abstract class TypeChecker extends CtScanner {

    protected final Context context;
    protected final Factory factory;
    protected final VCChecker vcChecker;
    private final Diagnostics diagnostics = Diagnostics.getInstance();
    private final Map<String, Optional<StateMachine>> stateMachines = new HashMap<>();

    public TypeChecker(Context context, Factory factory) {
        this.context = context;
        this.factory = factory;
        this.vcChecker = new VCChecker();
    }

    public Context getContext() {
        return context;
    }

    public Factory getFactory() {
        return factory;
    }

    /**
     * The state machine of {@code className}, built once per checker: the specifications it is built from are all known
     * before a checker starts
     */
    public Optional<StateMachine> getStateMachine(String className, CtElement element) throws LJError {
        if (!stateMachines.containsKey(className)) {
            StateSolver solver = new StateSolver(this, element.getPosition());
            stateMachines.put(className, StateMachineBuilder.build(this, solver, className, element));
        }
        return stateMachines.get(className);
    }

    @Override
    public void scan(CtElement element) {
        try {
            super.scan(element);
        } catch (LJError e) {
            throw e;
        } catch (RuntimeException e) {
            SourcePosition position = element.getPosition();
            String location = position.isValidPosition() && position.getFile() != null
                    ? position.getFile().getAbsolutePath() + ":" + position.getLine()
                    : "(no source position: " + element.getClass().getSimpleName() + ")";
            String expression = Utils.getExpressionFromPosition(position);
            String msg = String.format("\nError while checking %s\n  on %s \n  at %s\n  with %s",
                    element.getClass().getSimpleName(), expression, location, e.getMessage());
            throw new RuntimeException(msg, e);
        }
    }

    public Predicate getRefinement(CtElement elem) {
        Predicate c = (Predicate) elem.getMetadata(Keys.REFINEMENT);
        return c == null ? new Predicate() : c;
    }

    public Optional<Predicate> getRefinementFromAnnotation(CtElement element) throws LJError {
        Optional<Predicate> constr = Optional.empty();
        Optional<String> ref = Optional.empty();
        for (CtAnnotation<? extends Annotation> ann : element.getAnnotations()) {
            String an = ann.getAnnotationType().getQualifiedName();
            if (an.contentEquals("liquidjava.specification.Refinement")) {
                String value = getStringFromAnnotation(ann.getValue("value"));
                ref = Optional.of(value);

            } else if (an.contentEquals("liquidjava.specification.RefinementAlias")) {
                CtExpression<String> rawValue = ann.getValue("value");
                String value = getStringFromAnnotation(rawValue);
                handleAlias(value, element, Utils.getAnnotationValuePosition(rawValue));
            }
        }
        if (ref.isPresent()) {
            String prefix = getQualifiedClassName(element);
            Predicate p = prefix == null ? new Predicate(ref.get(), element)
                    : new Predicate(ref.get(), element, prefix);
            p = readStateValues(p, element, Utils.getLJAnnotationPosition(element, ref.get()));
            if (!p.getExpression().isBooleanExpression()) {
                SourcePosition position = Utils.getLJAnnotationPosition(element, ref.get());
                throw new InvalidRefinementError(position, "Refinement predicate must be a boolean expression",
                        ref.get());
            }
            if (!Boolean.TRUE.equals(element.getMetadata(Keys.REFINEMENT_SAT_CHECK)))
                checkRefinementSatisfiability(ref.get(), p, element);
            constr = Optional.of(p);
        }
        return constr;
    }

    /**
     * Performs a best-effort satisfiability check for a refinement reporting a warning if unsat. Runs an SMT check on a
     * temporary scope and if the refinement mentions other names that are still unavailable at this point, the SMT
     * check fails and that failure is ignored.
     */
    private void checkRefinementSatisfiability(String refinement, Predicate predicate, CtElement element) {
        context.enterContext();
        try {
            Predicate p = new Predicate();
            CtTypeReference<?> annotationType = element instanceof CtTypedElement<?> typedElement
                    ? typedElement.getType() : null;
            if (annotationType != null && !context.hasVariable(Keys.WILDCARD))
                context.addVarToContext(Keys.WILDCARD, annotationType, p, element);

            if (element instanceof CtVariable<?> variable && !context.hasVariable(variable.getSimpleName()))
                context.addVarToContext(variable.getSimpleName(), variable.getType(), p, element);

            if (element instanceof CtMethod<?> method && method.getType() != null && !context.hasVariable("return"))
                context.addVarToContext("return", method.getType(), p, element);

            String qualifiedClassName = getQualifiedClassName(element);
            if (qualifiedClassName != null && !context.hasVariable(Keys.THIS))
                context.addVarToContext(Keys.THIS, factory.Type().createReference(qualifiedClassName), p, element);

            Predicate refinementPredicate = predicate.toSolverPredicate(context.getGhostStates(),
                    new String[] { Keys.WILDCARD, Keys.THIS }, context, factory);

            if (new SMTEvaluator().isUnsatisfiable(refinementPredicate, context)) {
                SourcePosition position = Utils.getLJAnnotationPosition(element, refinement);
                diagnostics.add(new UnsatisfiableRefinementWarning(position, refinement));
            }
            element.putMetadata(Keys.REFINEMENT_SAT_CHECK, true); // for caching the satisfiability check result
        } catch (Exception e) {
            // ignore
        } finally {
            context.exitContext();
        }
    }

    @SuppressWarnings({ "rawtypes" })
    public Optional<String> getMessageFromAnnotation(CtElement element) {
        for (CtAnnotation<? extends Annotation> ann : element.getAnnotations()) {
            String an = ann.getAnnotationType().getQualifiedName();
            if (an.contentEquals("liquidjava.specification.Refinement")) {
                Map<String, CtExpression> values = ann.getAllValues();
                String msg = getStringFromAnnotation((values.get("msg")));
                if (msg != null && !msg.isEmpty()) {
                    return Optional.of(msg);
                }
            }
        }
        return Optional.empty();
    }

    /** Registers the ghosts and state sets a type declares, then checks the names given to its state sets */
    @SuppressWarnings("unchecked")
    public void registerGhostsAndStateSets(CtElement element) throws LJError {
        int set = 0;
        Map<GhostFunction, SourcePosition> stateSets = new LinkedHashMap<>();
        Map<String, GhostFunction> setOfState = new HashMap<>();
        for (CtAnnotation<? extends Annotation> ann : element.getAnnotations()) {
            String an = ann.getAnnotationType().getQualifiedName();
            if (an.contentEquals("liquidjava.specification.StateSet")) {
                set++;
                CtExpression<?> nameValue = ann.getValues().get("name");
                SourcePosition namePosition = nameValue == null ? null : Utils.getAnnotationValuePosition(nameValue);
                String name = stateSetName(nameValue, namePosition);
                CtNewArray<String> states = (CtNewArray<String>) ann.getAllValues().get("value");
                GhostFunction g = createStateSet(states, name != null ? name : String.format(Formats.STATE_SET, set),
                        element);
                stateSets.put(g, name != null ? namePosition : Utils.getAnnotationValuePosition(states));
                checkStatesAreNotInAnotherSet(g, states, setOfState);
            }
            if (an.contentEquals("liquidjava.specification.Ghost")) {
                CtLiteral<String> s = (CtLiteral<String>) ann.getAllValues().get("value");
                createStateGhost(s.getValue(), element, Utils.getAnnotationValuePosition(s));
            }
            if (an.contentEquals("liquidjava.specification.RefinementPredicate")) {
                CtExpression<String> rawValue = ann.getValue("value");
                getGhostFunction(getStringFromAnnotation(rawValue), element,
                        Utils.getAnnotationValuePosition(rawValue));
            }
        }
        // checked once every set and ghost of the class exists, so the order of the annotations does not matter; a
        // generated name such as state1 can clash too
        for (Map.Entry<GhostFunction, SourcePosition> stateSet : stateSets.entrySet()) {
            checkStateSetNameIsFree(stateSet.getKey(), stateSet.getValue());
            warnIfStateSetNameIsAMethod(stateSet.getKey(), stateSet.getValue());
        }
    }

    /**
     * Reports a state of {@code set} that another set of its class also has: {@code open(this)} could not tell which of
     * the two sets it is about
     */
    private static void checkStatesAreNotInAnotherSet(GhostFunction set, CtNewArray<String> states,
            Map<String, GhostFunction> setOfState) throws LJError {
        for (CtExpression<?> state : states.getElements()) {
            if (!(state instanceof CtLiteral<?> literal && literal.getValue()instanceof String name))
                continue;
            GhostFunction other = setOfState.putIfAbsent(name, set);
            if (other != null)
                throw new CustomError(
                        String.format("State '%s' is in two state sets of %s: %s and %s", name,
                                set.getParentClassName(), other.getName(), set.getName()),
                        Utils.getAnnotationValuePosition(literal));
        }
    }

    /** The name given to a state set, or null when it has none */
    private static String stateSetName(CtExpression<?> nameValue, SourcePosition position) throws LJError {
        if (nameValue == null)
            return null;
        if (!(nameValue instanceof CtLiteral<?> literal && literal.getValue()instanceof String name))
            throw new CustomError("State set name must be a string literal", position);
        if (name.isEmpty())
            return null;
        if (!name.matches("[a-z][a-zA-Z0-9_]*") || name.equals(Keys.OLD) || name.equals("true") || name.equals("false"))
            throw new CustomError(String.format(
                    "State set name '%s' must be an identifier that starts with a lowercase letter", name), position);
        if (name.matches(Formats.STATE_SET_NAMES))
            throw new CustomError(String.format("State set name '%s' is kept for state sets without a name", name),
                    position);
        return name;
    }

    /** Reports a named state set whose name is also the name of a state, ghost or other state set of its class */
    private void checkStateSetNameIsFree(GhostFunction set, SourcePosition position) throws LJError {
        String name = set.getName();
        String usedBy = null;
        for (GhostState gs : context.getGhostState(set.getParentClassName()))
            if (gs.getPrefix().equals(set.getPrefix()) && gs.getName().equals(name))
                usedBy = gs.getParent() != null ? "a state" : "a ghost";
        for (GhostFunction g : context.getGhosts())
            if (usedBy == null && g != set && g.getPrefix().equals(set.getPrefix()) && g.getName().equals(name))
                usedBy = context.getGhostStates().stream().anyMatch(gs -> gs.getParent() == g) ? "another state set"
                        : "a ghost";
        if (usedBy != null)
            throw new CustomError(String.format("State set name '%s' is already the name of %s of %s", name, usedBy,
                    set.getParentClassName()), position);
    }

    /**
     * Warns about a named state set whose name is also the name of a method of its class, or of the class an external
     * spec refines: in a message such as {@code undo(x) == undoable} the set reads like a call to that method
     */
    private void warnIfStateSetNameIsAMethod(GhostFunction set, SourcePosition position) {
        String name = set.getName();
        if (name.matches(Formats.STATE_SET_NAMES))
            return; // a set without a name, whose name the user did not choose
        CtType<?> type = factory.Type().createReference(set.getPrefix()).getTypeDeclaration();
        // a private method of a superclass is not a method of this class
        if (type == null || type.getAllMethods().stream()
                .noneMatch(m -> m.getSimpleName().equals(name) && (m.getDeclaringType() == type || !m.isPrivate())))
            return;
        String state = context.getGhostState(set.getParentClassName()).stream().filter(gs -> gs.getParent() == set)
                .map(GhostState::getName).findFirst().orElse("...");
        diagnostics.add(new CustomWarning(position,
                String.format(
                        "State set name '%s' is also the name of a method of %s, so %s(x) == %s reads like a "
                                + "call to %s(). Choose another name, such as %sState",
                        name, set.getParentClassName(), name, state, name, name)));
    }

    private GhostFunction createStateSet(CtNewArray<String> e, String name, CtElement element) throws LJError {

        // if any of the states starts with uppercase, throw error (reserved for alias)
        for (CtExpression<?> ce : e.getElements()) {
            if (ce instanceof CtLiteral<?>) {
                @SuppressWarnings("unchecked")
                CtLiteral<String> s = (CtLiteral<String>) ce;
                String f = s.getValue();
                if (Character.isUpperCase(f.charAt(0))) {
                    throw new CustomError("State names must start with lowercase", Utils.getAnnotationValuePosition(s));
                }
            }
        }

        Optional<GhostFunction> og = createStateGhost(name, element);
        if (og.isEmpty()) {
            throw new RuntimeException("Error in creation of GhostFunction");
        }
        GhostFunction g = og.get();
        context.addGhostFunction(g);
        context.addGhostClass(g.getParentClassName());

        List<CtExpression<?>> ls = e.getElements();
        Predicate ip = Predicate.createInvocation(g.getName(), Predicate.createVar(Keys.WILDCARD));
        int order = 0;
        for (CtExpression<?> ce : ls) {
            if (ce instanceof CtLiteral<?>) {
                @SuppressWarnings("unchecked")
                CtLiteral<String> s = (CtLiteral<String>) ce;
                String f = s.getValue();
                GhostState gs = new GhostState(f, g.getParametersTypes(), factory.Type().BOOLEAN_PRIMITIVE,
                        g.getPrefix(), Utils.getFile(element));
                gs.setGhostParent(g);
                gs.setRefinement(Predicate.createEquals(ip, Predicate.createLit(Integer.toString(order), Types.INT)));
                // open(THIS) -> state1(THIS) == 1
                context.addToGhostClass(g.getParentClassName(), gs);
            }
            order++;
        }
        return g;
    }

    protected GhostDTO getGhostDeclaration(String value, SourcePosition position) throws LJError {
        try {
            return RefinementsParser.parseGhostDeclaration(value);
        } catch (LJError e) {
            // add location info to error
            if (e.getPosition() == null)
                e.setPosition(position);
            throw e;
        }
    }

    private void createStateGhost(String string, CtElement element, SourcePosition position) throws LJError {
        GhostDTO gd = getGhostDeclaration(string, position);
        if (!gd.paramTypes().isEmpty()) {
            throw new CustomError(
                    "Ghost States have the class as parameter " + "by default, no other parameters are allowed",
                    position);
        }
        // Set class as parameter of Ghost
        String qn = getQualifiedClassName(element);
        String sn = getSimpleClassName(element);
        if (qn == null || sn == null)
            return; // cannot determine class context - skip processing

        context.addGhostClass(sn);
        List<CtTypeReference<?>> param = Collections.singletonList(factory.Type().createReference(qn));

        CtTypeReference<?> r = factory.Type().createReference(gd.returnType());
        GhostState gs = new GhostState(gd.name(), param, r, qn, Utils.getFile(element));
        context.addToGhostClass(sn, gs);
    }

    protected String getQualifiedClassName(CtElement element) {
        return getEnclosingType(element).map(CtType::getQualifiedName).orElse(null);
    }

    protected String getSimpleClassName(CtElement element) {
        return getEnclosingType(element).map(CtType::getSimpleName).orElse(null);
    }

    protected Optional<GhostFunction> createStateGhost(String name, CtElement element) {
        Optional<CtType<?>> enclosingType = getEnclosingType(element);
        if (enclosingType.isPresent()) {
            CtType<?> type = enclosingType.get();
            CtTypeReference<?> ret = factory.Type().INTEGER_PRIMITIVE;
            List<String> params = Collections.singletonList(type.getSimpleName());
            GhostFunction gh = new GhostFunction(name, params, ret, factory, type.getQualifiedName());
            return Optional.of(gh);
        }
        return Optional.empty();
    }

    private Optional<CtType<?>> getEnclosingType(CtElement element) {
        if (element instanceof CtType<?> type)
            return Optional.of(type);
        return Optional.ofNullable(element.getParent(CtType.class));
    }

    protected void getGhostFunction(String value, CtElement element, SourcePosition position) throws LJError {
        GhostDTO f = getGhostDeclaration(value, position);
        CtType<?> type = element instanceof CtType<?> t ? t : element.getParent()instanceof CtType<?> t ? t : null;
        if (type != null)
            context.addGhostFunction(new GhostFunction(f, factory, type.getQualifiedName()));
    }

    /**
     * {@code p} with each state written as a value of its set, {@code phase(x) == open}, read as the shorthand
     * {@code open(x)}, in the scope of {@code element}, whose variables keep their meaning
     */
    public Predicate readStateValues(Predicate p, CtElement element, SourcePosition position) throws LJError {
        StateValues values = new StateValues(context.getGhostStates(), name -> isVariableIn(element, name),
                argument -> typeIn(element, argument).map(type -> set -> isRelatedTo(type, set.getPrefix())));
        try {
            return new Predicate(values.toShorthand(p.getExpression()));
        } catch (LJError e) {
            if (e.getPosition() == null)
                e.setPosition(position);
            throw e;
        }
    }

    /**
     * Whether {@code name} is a variable where {@code element} is: a parameter, _, this, return, or one in the context
     */
    private boolean isVariableIn(CtElement element, String name) {
        if (name.equals(Keys.WILDCARD) || name.equals(Keys.THIS) || name.equals("return") || context.hasVariable(name))
            return true;
        CtExecutable<?> executable = element instanceof CtExecutable<?> e ? e : element.getParent(CtExecutable.class);
        return executable != null && executable.getParameters().stream().anyMatch(p -> p.getSimpleName().equals(name));
    }

    /** The type of a state's argument where {@code element} is: this, _, a parameter, a variable, or old of one */
    private Optional<CtTypeReference<?>> typeIn(CtElement element, Expression argument) {
        if (argument instanceof FunctionInvocation old && old.getName().equals(Keys.OLD) && old.getArgs().size() == 1)
            return typeIn(element, old.getArgs().get(0));
        if (!(argument instanceof Var var))
            return Optional.empty();
        String name = var.getName();
        if (name.equals(Keys.THIS))
            return Optional.ofNullable(getQualifiedClassName(element)).map(c -> factory.Type().createReference(c));
        if (name.equals(Keys.WILDCARD))
            return element instanceof CtTypedElement<?> typed ? Optional.ofNullable(typed.getType()) : Optional.empty();
        CtExecutable<?> executable = element instanceof CtExecutable<?> e ? e : element.getParent(CtExecutable.class);
        if (executable != null)
            for (CtParameter<?> parameter : executable.getParameters())
                if (parameter.getSimpleName().equals(name))
                    return Optional.ofNullable(parameter.getType());
        return context.hasVariable(name) ? Optional.ofNullable(context.getVariableByName(name).getType())
                : Optional.empty();
    }

    /**
     * Reads the states written as values of their sets in the body of {@code alias}, declared on class {@code klass} at
     * {@code path}: only its parameters are variables, typed as they are declared
     */
    private StateValues aliasStateValues(AliasDTO alias, String klass, String path) {
        return new StateValues(context.getGhostStates(), alias.getVarNames()::contains, argument -> {
            int parameter = argument instanceof Var var ? alias.getVarNames().indexOf(var.getName()) : -1;
            if (parameter < 0)
                return Optional.empty();
            CtTypeReference<?> type = AliasWrapper.parameterType(alias.getVarTypes().get(parameter), factory, klass,
                    path);
            return Optional.of(set -> isRelatedTo(type, set.getPrefix()));
        });
    }

    /**
     * Whether a value of {@code type} can be of the class {@code className}: it is that class, one of its subtypes or
     * one of its supertypes
     */
    private boolean isRelatedTo(CtTypeReference<?> type, String className) {
        CtTypeReference<?> klass = factory.Type().createReference(className);
        return type.getQualifiedName().equals(className) || type.isSubtypeOf(klass) || klass.isSubtypeOf(type);
    }

    protected void handleAlias(String ref, CtElement element, SourcePosition position) throws LJError {
        try {
            AliasDTO a = RefinementsParser.parseAliasDefinition(ref);
            String klass = null;
            String path = null;
            if (element instanceof CtClass) {
                klass = ((CtClass<?>) element).getSimpleName();
                path = ((CtClass<?>) element).getQualifiedName();
            } else if (element instanceof CtInterface<?>) {
                klass = ((CtInterface<?>) element).getSimpleName();
                path = ((CtInterface<?>) element).getQualifiedName();
            }
            if (klass != null && path != null) {
                a.parse(path);
                a.resolveStaticFinalConstants(element, position);
                a.readStateValues(aliasStateValues(a, klass, path));
                if (a.getExpression() != null && !a.getExpression().isBooleanExpression()) {
                    throw new InvalidRefinementError(position, "Refinement alias must return a boolean expression",
                            ref);
                }
                AliasWrapper aw = new AliasWrapper(a, factory, klass, path, position);
                context.addAlias(aw);
            }
        } catch (LJError e) {
            // add location info to error
            if (e.getPosition() == null)
                e.setPosition(position);
            throw e;
        }
    }

    Optional<CtAnnotation<?>> getExternalRefinement(CtInterface<?> intrface) {
        for (CtAnnotation<? extends Annotation> ann : intrface.getAnnotations())
            if (ann.getAnnotationType().getQualifiedName()
                    .contentEquals("liquidjava.specification.ExternalRefinementsFor")) {
                return Optional.of(ann);
            }
        return Optional.empty();
    }

    public void checkVariableRefinements(Predicate refinementFound, String simpleName, CtTypeReference<?> type,
            CtElement usage, CtElement variable) throws LJError {
        Optional<Predicate> expectedType = getRefinementFromAnnotation(variable);
        SourcePosition declarationPosition = Utils.getRefinementPosition(variable);
        Predicate cEt;
        RefinedVariable mainRV = null;
        if (context.hasVariable(simpleName)) {
            mainRV = context.getVariableByName(simpleName);
        } else {
            // A field declaration may occur after its first write in the source.
            // Register its declared contract before attaching the assigned instance.
            mainRV = context.addVarToContext(simpleName, type, expectedType.orElseGet(Predicate::new), variable);
        }

        if (context.hasVariable(simpleName) && !context.getVariableByName(simpleName).getRefinement().isBooleanTrue()) {
            cEt = mainRV.getMainRefinement();
        } else {
            cEt = expectedType.orElseGet(Predicate::new);
        }

        cEt = cEt.substituteVariable(Keys.WILDCARD, simpleName);
        Predicate cet = cEt.substituteVariable(Keys.WILDCARD, simpleName);

        String newName = String.format(Formats.INSTANCE, simpleName, context.getCounter());
        Utils.untrackedWriteReceiver(usage)
                .ifPresent(receiver -> FieldDisplayNames.recordWriteReceiver(newName, receiver));
        Predicate correctNewRefinement = refinementFound.substituteVariable(Keys.WILDCARD, newName);
        correctNewRefinement = correctNewRefinement.substituteVariable(Keys.THIS, newName);
        cEt = cEt.substituteVariable(simpleName, newName);

        // Substitute variable in verification
        RefinedVariable rv = context.addInstanceToContext(newName, type, correctNewRefinement, usage);
        for (CtTypeReference<?> t : mainRV.getSuperTypes())
            rv.addSuperType(t);
        context.addRefinementInstanceToVariable(simpleName, newName);
        String customMessage = getMessageFromAnnotation(variable).orElse(mainRV != null ? mainRV.getMessage() : null);
        checkSMT(cEt, usage, declarationPosition, customMessage);
        context.addRefinementToVariableInContext(simpleName, type, cet, usage);
    }

    public void checkSMT(Predicate expectedType, CtElement element, SourcePosition declarationPosition,
            String customMessage) throws LJError {
        vcChecker.processSubtyping(expectedType, context.getGhostStates(), element, factory, declarationPosition,
                customMessage);
        element.putMetadata(Keys.REFINEMENT, expectedType);
    }

    public void checkStateSMT(Predicate prevState, Predicate expectedState, CtElement target,
            SourcePosition declarationPosition, String moreInfo) throws LJError {
        vcChecker.processSubtyping(prevState, expectedState, context.getGhostStates(), target, declarationPosition,
                factory);
    }

    public boolean checkStateSMT(Predicate prevState, Predicate expectedState, SourcePosition p) throws LJError {
        return checkStateSMT(prevState, expectedState, p, false);
    }

    public boolean checkStateSMT(Predicate prevState, Predicate expectedState, SourcePosition p, boolean silent)
            throws LJError {
        return verifyStateSMT(prevState, expectedState, p, silent, false).isOk();
    }

    /** Checks that {@code prevState} implies {@code expectedState}, with a counterexample when it does not */
    public SMTResult verifyStateSMT(Predicate prevState, Predicate expectedState, SourcePosition p, boolean silent)
            throws LJError {
        return verifyStateSMT(prevState, expectedState, p, silent, true);
    }

    private SMTResult verifyStateSMT(Predicate prevState, Predicate expectedState, SourcePosition p, boolean silent,
            boolean needsCounterexample) throws LJError {
        return vcChecker.verifySMTSubtypeStates(prevState, expectedState, context.getGhostStates(), p, factory, silent,
                needsCounterexample);
    }

    /** What a state refinement error from {@code found} to {@code expected} shows as found */
    public Predicate shownFound(Predicate found, Predicate expected) {
        return vcChecker.shownFound(found, expected);
    }

    public void throwRefinementError(SourcePosition position, SourcePosition declarationPosition,
            Predicate expectedType, Predicate foundType, String customMessage) throws LJError {
        vcChecker.throwRefinementError(position, declarationPosition, expectedType, foundType, null, customMessage);
    }

    public void throwStateRefinementError(SourcePosition position, SourcePosition declarationPosition, Predicate found,
            Predicate expected, String customMessage) throws LJError {
        throwStateRefinementError(position, declarationPosition, found, expected, customMessage, List.of(), null, null,
                List.of());
    }

    public void throwStateRefinementError(SourcePosition position, SourcePosition declarationPosition, Predicate found,
            Predicate expected, String customMessage, List<Hint> hints, ErrorCategory category,
            StateErrorContext context, List<String> declarationNotes) throws LJError {
        vcChecker.throwStateRefinementError(position, declarationPosition, found, expected, customMessage, hints,
                category, context, declarationNotes, counterexample(found, expected, position));
    }

    /** A model of what is known where {@code expected} fails, or null if the solver gives none */
    private Counterexample counterexample(Predicate found, Predicate expected, SourcePosition position) {
        try {
            SMTResult result = verifyStateSMT(found, expected, position, true);
            return result.isError() ? result.getCounterexample() : null;
        } catch (RuntimeException e) {
            // a failure to find a model must not replace the error being reported
            DebugLog.warn("Could not find a counterexample for the state error: " + e.getMessage());
            return null;
        }
    }

    public void throwStateConflictError(SourcePosition position, Predicate expectedType) throws LJError {
        vcChecker.throwStateConflictError(position, expectedType);
    }
}
