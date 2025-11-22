package liquidjava.processor.refinement_checker;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import liquidjava.diagnostics.errors.*;
import liquidjava.processor.context.AliasWrapper;
import liquidjava.processor.context.Context;
import liquidjava.processor.context.GhostFunction;
import liquidjava.processor.context.GhostState;
import liquidjava.processor.context.RefinedVariable;
import liquidjava.processor.facade.AliasDTO;
import liquidjava.processor.facade.GhostDTO;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.parsing.RefinementsParser;
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
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtScanner;

import static liquidjava.processor.refinement_checker.TypeCheckingUtils.*;

public abstract class TypeChecker extends CtScanner {

    Context context;
    Factory factory;
    VCChecker vcChecker;

    public TypeChecker(Context context, Factory factory) {
        this.context = context;
        this.factory = factory;
        vcChecker = new VCChecker();
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
            String an = ann.getActualAnnotation().annotationType().getCanonicalName();
            if (an.contentEquals("liquidjava.specification.Refinement")) {
                String st = getStringFromAnnotation(ann.getValue("value"));
                ref = Optional.of(st);

            } else if (an.contentEquals("liquidjava.specification.RefinementPredicate")) {
                String st = getStringFromAnnotation(ann.getValue("value"));
                getGhostFunction(st, element);

            } else if (an.contentEquals("liquidjava.specification.RefinementAlias")) {
                String st = getStringFromAnnotation(ann.getValue("value"));
                handleAlias(st, element);
            }
        }
        if (ref.isPresent()) {
            Predicate p = new Predicate(ref.get(), element);

            // check if refinement is valid
            if (!p.getExpression().isBooleanExpression()) {
                throw new InvalidRefinementError(element.getPosition(),
                        "Refinement predicate must be a boolean expression", ref.get());
            }
            constr = Optional.of(p);
        }
        return constr;
    }

    @SuppressWarnings("unchecked")
    public void handleStateSetsFromAnnotation(CtElement element) throws LJError {
        int set = 0;
        for (CtAnnotation<? extends Annotation> ann : element.getAnnotations()) {
            String an = ann.getActualAnnotation().annotationType().getCanonicalName();
            if (an.contentEquals("liquidjava.specification.StateSet")) {
                set++;
                createStateSet((CtNewArray<String>) ann.getAllValues().get("value"), set, element);
            }
            if (an.contentEquals("liquidjava.specification.Ghost")) {
                CtLiteral<String> s = (CtLiteral<String>) ann.getAllValues().get("value");
                createStateGhost(s.getValue(), ann, element);
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
                        g.getPrefix());
                gs.setGhostParent(g);
                gs.setRefinement(Predicate.createEquals(ip, Predicate.createLit(Integer.toString(order), Types.INT)));
                // open(THIS) -> state1(THIS) == 1
                context.addToGhostClass(g.getParentClassName(), gs);
            }
            order++;
        }
    }

    private void createStateGhost(String string, CtAnnotation<? extends Annotation> ann, CtElement element)
            throws LJError {
        GhostDTO gd = RefinementsParser.getGhostDeclaration(string);
        if (!gd.paramTypes().isEmpty()) {
            throw new CustomError(
                    "Ghost States have the class as parameter " + "by default, no other parameters are allowed",
                    ann.getPosition());
        }
        // Set class as parameter of Ghost
        String qn = getQualifiedClassName(element);
        String sn = getSimpleClassName(element);
        context.addGhostClass(sn);
        List<CtTypeReference<?>> param = Collections.singletonList(factory.Type().createReference(qn));

        CtTypeReference<?> r = factory.Type().createReference(gd.returnType());
        GhostState gs = new GhostState(gd.name(), param, r, qn);
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

    protected void getGhostFunction(String value, CtElement element) throws LJError {
        GhostDTO f = RefinementsParser.getGhostDeclaration(value);
        if (element.getParent()instanceof CtClass<?> klass) {
            GhostFunction gh = new GhostFunction(f, factory, klass.getQualifiedName());
            context.addGhostFunction(gh);
        }
    }

    protected void handleAlias(String ref, CtElement element) throws LJError {
        try {
            AliasDTO a = RefinementsParser.getAliasDeclaration(ref);
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
                // refinement alias must return a boolean expression
                if (a.getExpression() != null && !a.getExpression().isBooleanExpression()) {
                    throw new InvalidRefinementError(element.getPosition(),
                            "Refinement alias must return a boolean expression", ref);
                }
                AliasWrapper aw = new AliasWrapper(a, factory, klass, path);
                context.addAlias(aw);
            }
        } catch (LJError e) {
            // add location info to error
            SourcePosition pos = Utils.getRefinementAnnotationPosition(element, ref);
            e.setPosition(pos);
            throw e;
        }
    }

    Optional<String> getExternalRefinement(CtInterface<?> intrface) {
        Optional<String> ref = Optional.empty();
        for (CtAnnotation<? extends Annotation> ann : intrface.getAnnotations())
            if (ann.getActualAnnotation().annotationType().getCanonicalName()
                    .contentEquals("liquidjava.specification.ExternalRefinementsFor")) {
                @SuppressWarnings("unchecked")
                CtLiteral<String> s = (CtLiteral<String>) ann.getAllValues().get("value");
                ref = Optional.of(s.getValue());
            }
        return ref;
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
        // smt check
        checkSMT(cEt, usage); // TODO CHANGE
        context.addRefinementToVariableInContext(simpleName, type, cet, usage);
    }

    public void checkSMT(Predicate expectedType, CtElement element) throws LJError {
        vcChecker.processSubtyping(expectedType, context.getGhostState(), element, factory);
        element.putMetadata(Keys.REFINEMENT, expectedType);
    }

    public void checkStateSMT(Predicate prevState, Predicate expectedState, CtElement target, String moreInfo)
            throws LJError {
        vcChecker.processSubtyping(prevState, expectedState, context.getGhostState(), target, factory);
    }

    public boolean checksStateSMT(Predicate prevState, Predicate expectedState, SourcePosition p) throws LJError {
        return vcChecker.canProcessSubtyping(prevState, expectedState, context.getGhostState(), p, factory);
    }

    public void createError(SourcePosition position, Predicate expectedType, Predicate foundType) throws LJError {
        vcChecker.raiseSubtypingError(position, expectedType, foundType);
    }

    public void createSameStateError(SourcePosition position, Predicate expectedType, String klass) throws LJError {
        vcChecker.raiseSameStateError(position, expectedType);
    }

    public void createStateMismatchError(SourcePosition position, String method, Predicate found, Predicate expected)
            throws LJError {
        vcChecker.raiseStateMismatchError(position, found, expected);
    }
}
