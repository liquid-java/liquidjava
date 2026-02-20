package liquidjava.processor.refinement_checker;

import java.util.List;
import java.util.Optional;

import liquidjava.diagnostics.Diagnostics;
import liquidjava.diagnostics.errors.LJError;
import liquidjava.diagnostics.warnings.ExternalClassNotFoundWarning;
import liquidjava.diagnostics.warnings.ExternalMethodNotFoundWarning;
import liquidjava.processor.context.Context;
import liquidjava.processor.context.ContextHistory;
import liquidjava.processor.context.GhostFunction;
import liquidjava.processor.facade.GhostDTO;
import liquidjava.processor.refinement_checker.general_checkers.MethodsFunctionsChecker;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.parsing.RefinementsParser;
import liquidjava.utils.Utils;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;

public class ExternalRefinementTypeChecker extends TypeChecker {
    private String prefix;
    private final Diagnostics diagnostics = Diagnostics.getInstance();
    private final ContextHistory contextHistory = ContextHistory.getInstance();

    public ExternalRefinementTypeChecker(Context context, Factory factory) {
        super(context, factory);
    }

    @Override
    public <T> void visitCtClass(CtClass<T> ctClass) {
    }

    @Override
    public <T> void visitCtInterface(CtInterface<T> intrface) {
        Optional<CtAnnotation<?>> externalRef = getExternalRefinement(intrface);
        if (externalRef.isPresent()) {
            @SuppressWarnings("unchecked")
            CtLiteral<String> literal = (CtLiteral<String>) externalRef.get().getAllValues().get("value");
            this.prefix = literal.getValue();
            if (!classExists(prefix)) {
                String message = String.format("Could not find class '%s'", prefix);
                diagnostics.add(new ExternalClassNotFoundWarning(externalRef.get().getPosition(), message, prefix));
                return;
            }
            getRefinementFromAnnotation(intrface);
            handleStateSetsFromAnnotation(intrface);
            super.visitCtInterface(intrface);
        }
    }

    @Override
    public <T> void visitCtField(CtField<T> f) {
        Optional<Predicate> oc = getRefinementFromAnnotation(f);
        Predicate c = oc.orElse(new Predicate());
        context.addGlobalVariableToContext(f.getSimpleName(), prefix, f.getType(), c);
        super.visitCtField(f);
    }

    public <R> void visitCtMethod(CtMethod<R> method) {
        CtType<?> targetType = factory.Type().createReference(prefix).getTypeDeclaration();
        if (!(targetType instanceof CtClass))
            return;

        boolean isConstructor = method.getSimpleName().equals(targetType.getSimpleName());
        if (isConstructor) {
            if (!constructorExists(targetType, method)) {
                String message = String.format("Could not find constructor '%s' for '%s'", method.getSignature(),
                        prefix);
                String[] overloads = getOverloads(targetType, method);
                diagnostics.add(new ExternalMethodNotFoundWarning(method.getPosition(), message, method.getSignature(),
                        prefix, overloads));
            }
        } else {
            if (!methodExists(targetType, method)) {
                String message = String.format("Could not find method '%s %s' for '%s'",
                        method.getType().getSimpleName(), method.getSignature(), prefix);
                String[] overloads = getOverloads(targetType, method);
                diagnostics.add(new ExternalMethodNotFoundWarning(method.getPosition(), message, method.getSignature(),
                        prefix, overloads));
                return;
            }
        }
        MethodsFunctionsChecker mfc = new MethodsFunctionsChecker(this);
        mfc.getMethodRefinements(method, prefix);
        super.visitCtMethod(method);
    }

    protected void getGhostFunction(String value, CtElement element) throws LJError {
        GhostDTO f = RefinementsParser.getGhostDeclaration(value);
        if (element.getParent() instanceof CtInterface<?>) {
            GhostFunction gh = new GhostFunction(f, factory, prefix);
            context.addGhostFunction(gh);
        }
    }

    @Override
    protected Optional<GhostFunction> createStateGhost(int order, CtElement element) {
        String klass = Utils.getSimpleName(prefix);
        if (klass != null) {
            CtTypeReference<?> ret = factory.Type().INTEGER_PRIMITIVE;
            List<String> params = List.of(klass);
            String name = String.format("state%d", order);
            GhostFunction gh = new GhostFunction(name, params, ret, factory, prefix);
            return Optional.of(gh);
        }
        return Optional.empty();
    }

    @Override
    protected String getQualifiedClassName(CtElement elem) {
        return prefix;
    }

    @Override
    protected String getSimpleClassName(CtElement elem) {
        return Utils.getSimpleName(prefix);
    }

    private boolean classExists(String className) {
        return factory.Type().createReference(className).getTypeDeclaration() != null;
    }

    private boolean methodExists(CtType<?> targetType, CtMethod<?> method) {
        // find method with matching signature
        return targetType.getMethods().stream().filter(m -> m.getSimpleName().equals(method.getSimpleName()))
                .anyMatch(m -> parametersMatch(m.getParameters(), method.getParameters())
                        && typesMatch(m.getType(), method.getType()));
    }

    private boolean constructorExists(CtType<?> targetType, CtMethod<?> method) {
        // find constructor with matching signature
        CtClass<?> targetClass = (CtClass<?>) targetType;
        return targetClass.getConstructors().stream()
                .anyMatch(c -> parametersMatch(c.getParameters(), method.getParameters()));
    }

    private boolean typesMatch(CtTypeReference<?> type1, CtTypeReference<?> type2) {
        if (type1 == null && type2 == null)
            return true;

        if (type1 == null || type2 == null)
            return false;

        return type1.getQualifiedName().equals(type2.getQualifiedName());
    }

    private boolean parametersMatch(List<?> targetParams, List<?> refinementParams) {
        if (targetParams.size() != refinementParams.size())
            return false;

        for (int i = 0; i < targetParams.size(); i++) {
            CtParameter<?> targetParam = (CtParameter<?>) targetParams.get(i);
            CtParameter<?> refinementParam = (CtParameter<?>) refinementParams.get(i);
            if (targetParam == null || refinementParam == null)
                return false;

            if (!typesMatch(targetParam.getType(), refinementParam.getType()))
                return false;
        }
        return true;
    }

    private String[] getOverloads(CtType<?> targetType, CtMethod<?> method) {
        return targetType.getMethods().stream().filter(m -> m.getSimpleName().equals(method.getSimpleName()))
                .map(m -> String.format("%s %s", m.getType().getSimpleName(), m.getSignature())).toArray(String[]::new);
    }
}
