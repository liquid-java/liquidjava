package liquidjava.processor.refinement_checker;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import liquidjava.diagnostics.categories.ErrorCategory;
import liquidjava.diagnostics.errors.RefinementError;
import liquidjava.processor.context.Context;
import liquidjava.processor.context.Variable;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtType;
import liquidjava.utils.Utils;
import liquidjava.utils.constants.Keys;
import liquidjava.processor.context.CallResult;
import liquidjava.processor.context.VariableInstance;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.FunctionInvocation;
import liquidjava.rj_language.ast.LiteralBoolean;
import liquidjava.rj_language.ast.Var;
import liquidjava.rj_language.ast.formatter.ExpressionFormatter;
import spoon.reflect.cu.SourcePosition;

import static liquidjava.rj_language.opt.VCSimplificationUtils.containsExpression;
import static liquidjava.rj_language.opt.VCSimplificationUtils.factsOf;
import static liquidjava.rj_language.opt.VCSimplificationUtils.isTrue;

/**
 * The origin notes of a refinement error, which say where the values its expected refinement is about come from. Each
 * is given only when it restates what the verifier used, and is left out otherwise
 */
final class OriginNotes {

    private OriginNotes() {
    }

    /**
     * When nothing is known about the values expected constrains and each is a parameter of the enclosing method that
     * is declared with no refinement, a note per parameter saying so. Nothing is said about the callers
     */
    static List<String> unrefinedParameters(RefinementError error, CtElement checked, Context context) {
        Optional<CtExecutable<?>> method = enclosingMethod(checked);
        List<String> variables = error.getExpected().getVariableNames().stream().distinct().toList();
        if (error.getCategory().filter(ErrorCategory.UNKNOWN::equals).isEmpty() || method.isEmpty()
                || variables.isEmpty())
            return List.of();

        List<String> notes = new ArrayList<>();
        for (String variable : variables) {
            Optional<CtParameter<?>> parameter = method.get().getParameters().stream()
                    .filter(p -> p.getSimpleName().equals(variable)).findFirst();
            // a written parameter has an instance, which expected would name instead
            if (parameter.isEmpty() || !parameter.get().getPosition().isValidPosition()
                    || !(context.getVariableByName(variable)instanceof Variable declared)
                    || !declared.getMainRefinement().isBooleanTrue() || isRefined(parameter.get(), context))
                return List.of();
            notes.add(String.format("Origin: line %d, %s is a parameter of %s with no refinement",
                    parameter.get().getPosition().getLine(), error.getDisplayNames().get(variable),
                    signature(method.get())));
        }
        return notes;
    }

    /**
     * For each argument of expected that is a call to a refined method, what the method's return refinement guarantees
     * about it. An equality defining a ghost, {@code _ == size(this)}, is left out when the ghost is nowhere else in
     * the refinement: it then constrains no value, so what is left is all it says about the value
     */
    static List<String> returnedValues(RefinementError error, Context context) {
        List<String> notes = new ArrayList<>();
        for (String variable : error.getExpected().getVariableNames().stream().distinct().toList()) {
            // a call argument, which has no variable of its own and is shown as the call
            if (!(context.getVariableByName(variable)instanceof VariableInstance instance)
                    || instance.getReturnedBy().isEmpty() || context.getVariableFromInstance(instance) != null)
                continue;
            CallResult call = instance.getReturnedBy().get();
            SourcePosition declared = call.function().getPlacementInCode() == null ? null
                    : call.function().getPlacementInCode().getAnnotationPosition();
            if (declared == null || !declared.isValidPosition() || declared.getFile() == null)
                continue;
            Expression guaranteed = withoutGhostDefinitions(call.returned().getExpression());
            String value = ExpressionFormatter.format(new Var(variable), error.getDisplayNames());
            String says = isTrue(guaranteed) ? "guarantees nothing about " + value
                    : "guarantees only " + ExpressionFormatter.format(
                            guaranteed.substitute(new Var(Keys.WILDCARD), new Var(variable)), error.getDisplayNames());
            notes.add(String.format("From: %s (%s:%d) %s", value, declared.getFile().getName(), declared.getLine(),
                    says));
        }
        return notes;
    }

    private static Expression withoutGhostDefinitions(Expression refinement) {
        List<Expression> facts = factsOf(refinement);
        Expression kept = new LiteralBoolean(true);
        for (Expression fact : facts) {
            boolean definesGhost = ghostDefined(fact).filter(
                    ghost -> facts.stream().noneMatch(other -> other != fact && containsExpression(other, ghost)))
                    .isPresent();
            if (!definesGhost)
                kept = isTrue(kept) ? fact : new BinaryExpression(kept, "&&", fact);
        }
        return kept;
    }

    /** The ghost application {@code g(...)} of a fact {@code _ == g(...)} */
    private static Optional<Expression> ghostDefined(Expression fact) {
        if (!(fact instanceof BinaryExpression equality) || !"==".equals(equality.getOperator()))
            return Optional.empty();
        Expression left = equality.getFirstOperand();
        Expression right = equality.getSecondOperand();
        if (isWildcard(left) && right instanceof FunctionInvocation)
            return Optional.of(right);
        if (isWildcard(right) && left instanceof FunctionInvocation)
            return Optional.of(left);
        return Optional.empty();
    }

    private static boolean isWildcard(Expression expression) {
        return expression instanceof Var var && Keys.WILDCARD.equals(var.getName());
    }

    /**
     * Whether the parameter is written with a refinement, even {@code true}, or a specification of its method refines
     * it, such as an external one, which the method's body is not checked against
     */
    private static boolean isRefined(CtParameter<?> parameter, Context context) {
        if (parameter.getAnnotations().stream()
                .anyMatch(a -> a.getAnnotationType().getQualifiedName().equals("liquidjava.specification.Refinement")))
            return true;
        CtExecutable<?> method = parameter.getParent();
        int index = method.getParameters().indexOf(parameter);
        String owner = method.getParent(CtType.class).getQualifiedName();
        String name = method instanceof CtConstructor<?> ? Utils.getSimpleName(owner) : method.getSimpleName();
        List<String> types = method.getParameters().stream().map(p -> p.getType().getQualifiedName()).toList();
        return context.getCtxFunctions().stream()
                .filter(f -> owner.equals(f.getTargetClass()) && name.equals(Utils.getSimpleName(f.getName()))
                        && types.equals(f.getArguments().stream()
                                .map(a -> a.getType() == null ? null : a.getType().getQualifiedName()).toList()))
                .anyMatch(f -> !f.getArguments().get(index).getMainRefinement().isBooleanTrue());
    }

    private static Optional<CtExecutable<?>> enclosingMethod(CtElement element) {
        CtElement parent = element.getParent();
        while (parent != null && !(parent instanceof CtMethod<?> || parent instanceof CtConstructor<?>))
            parent = parent.getParent();
        return Optional.ofNullable((CtExecutable<?>) parent);
    }

    /** The method as {@code name(ParameterType, ...)}, a constructor by its class name */
    private static String signature(CtExecutable<?> method) {
        String name = method instanceof CtConstructor<?> constructor ? constructor.getDeclaringType().getSimpleName()
                : method.getSimpleName();
        return name + method.getParameters().stream().map(p -> p.getType().getSimpleName())
                .collect(Collectors.joining(", ", "(", ")"));
    }
}
