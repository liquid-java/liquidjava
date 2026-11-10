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

/**
 * The notes of a refinement error that say where the values its expected refinement is about come from. Each is given
 * only when it restates what the verifier used, and is left out otherwise
 */
final class FromNotes {

    private FromNotes() {
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
                    || !declared.getMainRefinement().isBooleanTrue())
                return List.of();
            notes.add(String.format("From: %s is a parameter of %s (line %d) with no refinement",
                    error.getDisplayNames().get(variable), signature(method.get()),
                    parameter.get().getPosition().getLine()));
        }
        return notes;
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
