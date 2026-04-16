package liquidjava.processor.refinement_checker;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import liquidjava.diagnostics.Diagnostics;
import liquidjava.diagnostics.errors.*;
import liquidjava.diagnostics.warnings.UnsatisfiableRefinementWarning;
import liquidjava.processor.context.AliasWrapper;
import liquidjava.processor.context.Context;
import liquidjava.processor.context.GhostFunction;
import liquidjava.processor.context.GhostState;
import liquidjava.processor.context.RefinedVariable;
import liquidjava.processor.facade.AliasDTO;
import liquidjava.processor.facade.GhostDTO;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.parsing.RefinementsParser;
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
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
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

            } else if (an.contentEquals("liquidjava.specification.RefinementPredicate")) {
                CtExpression<String> rawValue = ann.getValue("value");
                String value = getStringFromAnnotation(rawValue);
                getGhostFunction(value, element, rawValue.getPosition());

            } else if (an.contentEquals("liquidjava.specification.RefinementAlias")) {
                CtExpression<String> rawValue = ann.getValue("value");
                String value = getStringFromAnnotation(rawValue);
                handleAlias(value, element, rawValue.getPosition());
            }
        }
        if (ref.isPresent()) {
            Predicate p = new Predicate(ref.get(), element);
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

            Predicate refinementPredicate = predicate.changeStatesToRefinements(context.getGhostStates(),
                    new String[] { Keys.WILDCARD, Keys.THIS });
            refinementPredicate = refinementPredicate.changeAliasToRefinement(context, factory);

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

    @SuppressWarnings("unchecked")
    public void handleStateSetsFromAnnotation(CtElement element) throws LJError {
        int set = 0;
        for (CtAnnotation<? extends Annotation> ann : element.getAnnotations()) {
            String an = ann.getAnnotationType().getQualifiedName();
            if (an.contentEquals("liquidjava.specification.StateSet")) {
                set++;
                createStateSet((CtNewArray<String>) ann.getAllValues().get("value"), set, element);
            }
            if (an.contentEquals("liquidjava.specification.Ghost")) {
                CtLiteral<String> s = (CtLiteral<String>) ann.getAllValues().get("value");
                createStateGhost(s.getValue(), element, s.getPosition());
            }
        }
    }

    private void createStateSet(CtNewArray<String> e, int set, CtElement element) throws LJError {

        // if any of the states starts with uppercase, throw error (reserved for alias)
        for (CtExpression<?> ce : e.getElements()) {
            if (ce instanceof CtLiteral<?>) {
                @SuppressWarnings("unchecked")
                CtLiteral<String> s = (CtLiteral<String>) ce;
                String f = s.getValue();
                if (Character.isUpperCase(f.charAt(0))) {
                    throw new CustomError("State names must start with lowercase", s.getPosition());
                }
            }
        }

        Optional<GhostFunction> og = createStateGhost(set, element);
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
        if (element.getParent() instanceof CtClass<?>) {
            return ((CtClass<?>) element.getParent()).getQualifiedName();
        } else if (element instanceof CtClass<?>) {
            return ((CtClass<?>) element).getQualifiedName();
        }
        return null;
    }

    protected String getSimpleClassName(CtElement element) {
        if (element.getParent() instanceof CtClass<?>) {
            return ((CtClass<?>) element.getParent()).getSimpleName();
        } else if (element instanceof CtClass<?>) {
            return ((CtClass<?>) element).getSimpleName();
        }
        return null;
    }

    protected Optional<GhostFunction> createStateGhost(int order, CtElement element) {
        CtClass<?> klass = null;
        if (element.getParent() instanceof CtClass<?>) {
            klass = (CtClass<?>) element.getParent();
        } else if (element instanceof CtClass<?>) {
            klass = (CtClass<?>) element;
        }
        if (klass != null) {
            CtTypeReference<?> ret = factory.Type().INTEGER_PRIMITIVE;
            List<String> params = Collections.singletonList(klass.getSimpleName());
            String name = String.format("state%d", order);
            GhostFunction gh = new GhostFunction(name, params, ret, factory, klass.getQualifiedName());
            return Optional.of(gh);
        }
        return Optional.empty();
    }

    protected void getGhostFunction(String value, CtElement element, SourcePosition position) throws LJError {
        GhostDTO f = getGhostDeclaration(value, position);
        CtType<?> type = element instanceof CtType<?> t ? t : element.getParent()instanceof CtType<?> t ? t : null;
        if (type != null)
            context.addGhostFunction(new GhostFunction(f, factory, type.getQualifiedName()));
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
                if (a.getExpression() != null && !a.getExpression().isBooleanExpression()) {
                    throw new InvalidRefinementError(position, "Refinement alias must return a boolean expression",
                            ref);
                }
                AliasWrapper aw = new AliasWrapper(a, factory, klass, path);
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
        Predicate cEt;
        RefinedVariable mainRV = null;
        if (context.hasVariable(simpleName))
            mainRV = context.getVariableByName(simpleName);

        if (context.hasVariable(simpleName) && !context.getVariableByName(simpleName).getRefinement().isBooleanTrue()) {
            cEt = mainRV.getMainRefinement();
        } else {
            cEt = expectedType.orElseGet(Predicate::new);
        }

        cEt = cEt.substituteVariable(Keys.WILDCARD, simpleName);
        Predicate cet = cEt.substituteVariable(Keys.WILDCARD, simpleName);

        String newName = String.format(Formats.INSTANCE, simpleName, context.getCounter());
        Predicate correctNewRefinement = refinementFound.substituteVariable(Keys.WILDCARD, newName);
        correctNewRefinement = correctNewRefinement.substituteVariable(Keys.THIS, newName);
        cEt = cEt.substituteVariable(simpleName, newName);

        // Substitute variable in verification
        RefinedVariable rv = context.addInstanceToContext(newName, type, correctNewRefinement, usage);
        for (CtTypeReference<?> t : mainRV.getSuperTypes())
            rv.addSuperType(t);
        context.addRefinementInstanceToVariable(simpleName, newName);
        String customMessage = getMessageFromAnnotation(variable).orElse(mainRV != null ? mainRV.getMessage() : null);
        checkSMT(cEt, usage, customMessage); // TODO CHANGE
        context.addRefinementToVariableInContext(simpleName, type, cet, usage);
    }

    public void checkSMT(Predicate expectedType, CtElement element) throws LJError {
        checkSMT(expectedType, element, null);
    }

    public void checkSMT(Predicate expectedType, CtElement element, String customMessage) throws LJError {
        vcChecker.processSubtyping(expectedType, context.getGhostStates(), element, factory, customMessage);
        element.putMetadata(Keys.REFINEMENT, expectedType);
    }

    public void checkStateSMT(Predicate prevState, Predicate expectedState, CtElement target, String moreInfo)
            throws LJError {
        vcChecker.processSubtyping(prevState, expectedState, context.getGhostStates(), target, factory);
    }

    public boolean checkStateSMT(Predicate prevState, Predicate expectedState, SourcePosition p) throws LJError {
        SMTResult result = vcChecker.verifySMTSubtypeStates(prevState, expectedState, context.getGhostStates(), p,
                factory);
        return result.isOk();
    }

    public void throwRefinementError(SourcePosition position, Predicate expectedType, Predicate foundType,
            String customMessage) throws LJError {
        vcChecker.throwRefinementError(position, expectedType, foundType, null, customMessage);
    }

    public void throwStateRefinementError(SourcePosition position, Predicate found, Predicate expected,
            String customMessage) throws LJError {
        vcChecker.throwStateRefinementError(position, found, expected, customMessage);
    }

    public void throwStateConflictError(SourcePosition position, Predicate expectedType) throws LJError {
        vcChecker.throwStateConflictError(position, expectedType);
    }
}
