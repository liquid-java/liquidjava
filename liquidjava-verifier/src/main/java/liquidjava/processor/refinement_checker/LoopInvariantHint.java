package liquidjava.processor.refinement_checker;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import liquidjava.diagnostics.Code;
import liquidjava.diagnostics.SourceReference;
import liquidjava.diagnostics.errors.RefinementError;
import liquidjava.diagnostics.hints.Hint;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.formatter.ExpressionFormatter;
import liquidjava.rj_language.ast.formatter.VariableFormatter;
import liquidjava.utils.Pair;
import liquidjava.utils.constants.Keys;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtLoop;
import spoon.reflect.code.CtVariableWrite;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.visitor.filter.TypeFilter;

/**
 * The hint of a refinement error in a loop whose counterexample gives a value to an {@code int} local variable the loop
 * changes: the loop keeps only what holds on every iteration (see {@link LoopInvariants}), which a refinement on the
 * declaration of the variable states. Expected is suggested as that refinement when it can be written for the variable.
 */
final class LoopInvariantHint {

    private LoopInvariantHint() {
    }

    /**
     * Adds the hint for the first variable of the counterexample that one of the loops changes, unless the error has
     * one already. None is added when the variable is not a local one declared without a refinement, when expected is
     * not about it, or when expected does not hold on the first iteration of that loop either, as what the loop forgets
     * is then not the cause; nor when the refinement it would suggest does not hold before the loop.
     *
     * @param loops
     *            the invariants of the loops being checked, innermost first
     */
    static void addTo(RefinementError error, List<LoopInvariants> loops) {
        if (error.getHints().stream().anyMatch(hint -> hint.kind() == Hint.Kind.LOOP_INVARIANT))
            return;
        for (Pair<String, String> assignment : error.getCounterexample().assignments()) {
            String variable = VariableFormatter.withoutInstance(assignment.first());
            Optional<LoopInvariants> loop = loops.stream()
                    .filter(invariants -> invariants.variables().contains(variable)).findFirst();
            if (loop.isPresent()) {
                Optional<CtLocalVariable<?>> declaration = declaration(variable, loop.get().loop());
                boolean aboutVariable = error.getExpected().getVariableNames().stream()
                        .anyMatch(name -> isValueOf(name, variable));
                if (declaration.isPresent() && !isRefined(declaration.get()) && aboutVariable
                        && loop.get().holdsOnFirstIteration(error.getExpected()))
                    hint(error, declaration.get(), loop.get()).ifPresent(error::addHint);
                return;
            }
        }
    }

    private static Optional<Hint> hint(RefinementError error, CtLocalVariable<?> declaration,
            LoopInvariants invariants) {
        String variable = declaration.getSimpleName();
        CtLoop loop = invariants.loop();
        SourcePosition position = loop.getPosition();
        String line = "line " + position.getLine();
        String message = String.format(
                "%s changes in the loop at %s, and LiquidJava only keeps what holds for it on every iteration. ",
                Code.of(variable), line);
        Optional<Predicate> refinement = refinementFor(variable, error.getExpected(), loop);
        if (refinement.isPresent() && !invariants.holdsBefore(variable, refinement.get()))
            return Optional.empty();
        if (refinement.isPresent())
            message += String.format("If %s should hold on every iteration, state it on the declaration: %s",
                    Code.of(ExpressionFormatter.format(refinement.get().substituteVariable(Keys.WILDCARD, variable))),
                    Code.of(annotated(declaration, ExpressionFormatter.format(refinement.get()), loop)));
        else
            message += "To state what holds on every iteration, add a " + Code.of("@Refinement")
                    + " to its declaration.";
        return Optional.of(new Hint(Hint.Kind.LOOP_INVARIANT, message, SourceReference.of(message, line, position)));
    }

    /**
     * Expected as a refinement of the variable, on {@code _}, if it is about one of its values and otherwise only about
     * variables the loop does not write, which the code can name
     */
    private static Optional<Predicate> refinementFor(String variable, Predicate expected, CtLoop loop) {
        List<String> names = expected.getVariableNames().stream().distinct().toList();
        List<String> values = names.stream().filter(name -> isValueOf(name, variable)).toList();
        Set<String> written = writes(loop).stream().map(write -> write.getVariable().getSimpleName())
                .collect(Collectors.toSet());
        boolean othersNamed = names.stream().filter(name -> !values.contains(name))
                .allMatch(name -> !name.contains("#") && !written.contains(name));
        if (values.size() != 1 || !othersNamed)
            return Optional.empty();
        return Optional.of(expected.substituteVariable(values.get(0), Keys.WILDCARD));
    }

    /** Whether a name in a predicate is the variable or one of its instances */
    private static boolean isValueOf(String name, String variable) {
        return VariableFormatter.withoutInstance(name).equals(variable);
    }

    private static List<CtVariableWrite<?>> writes(CtLoop loop) {
        return loop.getElements(new TypeFilter<CtVariableWrite<?>>(CtVariableWrite.class));
    }

    private static boolean isRefined(CtLocalVariable<?> declaration) {
        return declaration.getAnnotations().stream().anyMatch(annotation -> annotation.getAnnotationType()
                .getQualifiedName().equals("liquidjava.specification.Refinement"));
    }

    /** The declaration of a local variable the loop writes */
    private static Optional<CtLocalVariable<?>> declaration(String variable, CtLoop loop) {
        return writes(loop).stream().filter(write -> write.getVariable().getSimpleName().equals(variable))
                .map(write -> write.getVariable().getDeclaration())
                .filter(declaration -> declaration instanceof CtLocalVariable<?>)
                .<CtLocalVariable<?>> map(declaration -> (CtLocalVariable<?>) declaration).findFirst();
    }

    /** The declaration with the refinement, as written in code, in its for if it is in a for's initialization */
    private static String annotated(CtLocalVariable<?> declaration, String refinement, CtLoop loop) {
        CtExpression<?> value = declaration.getDefaultExpression();
        String annotated = String.format("@Refinement(\"%s\") %s %s%s", refinement,
                declaration.getType().getSimpleName(), declaration.getSimpleName(),
                value == null ? "" : " = " + sourceOf(value));
        boolean inForInit = loop instanceof CtFor forLoop && forLoop.getForInit().contains(declaration);
        return inForInit ? "for (" + annotated + "; ...)" : annotated + ";";
    }

    private static String sourceOf(CtExpression<?> expression) {
        SourcePosition position = expression.getPosition();
        if (position.isValidPosition() && position.getCompilationUnit() != null) {
            String source = position.getCompilationUnit().getOriginalSourceCode();
            if (source != null && position.getSourceEnd() < source.length())
                return source.substring(position.getSourceStart(), position.getSourceEnd() + 1);
        }
        return expression.toString();
    }
}
