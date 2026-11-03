package liquidjava.rj_language.opt;

import java.util.ArrayList;
import java.util.List;

import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.FunctionInvocation;
import liquidjava.rj_language.ast.LiteralBoolean;
import liquidjava.rj_language.ast.Var;

public final class VCSimplificationUtils {

    public static VCImplication copyWithRefinement(VCImplication implication, Predicate refinement) {
        return new VCImplication(implication, refinement);
    }

    public static boolean containsVar(Expression expression, String name) {
        List<String> names = new ArrayList<>();
        expression.getVariableNames(names);
        return names.contains(name);
    }

    public static boolean containsVar(VCImplication implication, String name) {
        for (VCImplication current = implication; current != null; current = current.getNext()) {
            if (containsVar(current.getRefinement().getExpression(), name))
                return true;
        }
        return false;
    }

    public static boolean containsExpression(Expression expression, Expression target) {
        if (expression.equals(target))
            return true;

        for (Expression child : expression.getChildren())
            if (containsExpression(child, target))
                return true;
        return false;
    }

    public static boolean containsExpression(VCImplication implication, Expression target) {
        for (VCImplication current = implication; current != null; current = current.getNext())
            if (containsExpression(current.getRefinement().getExpression(), target))
                return true;
        return false;
    }

    public static boolean isTrue(Expression expression) {
        return expression instanceof LiteralBoolean literal && literal.isBooleanTrue();
    }

    public static boolean isFalse(Expression expression) {
        return expression instanceof LiteralBoolean literal && !literal.isBooleanTrue();
    }

    /**
     * Variable names of an expression
     */
    public static List<String> variablesOf(Expression expression) {
        List<String> names = new ArrayList<>();
        expression.getVariableNames(names);
        return names;
    }

    /**
     * Terms an expression constrains: its variables and its ghost and state applications such as {@code size(x)}
     */
    public static List<String> termsOf(Expression expression) {
        List<String> terms = variablesOf(expression);
        addApplications(expression, terms);
        return terms;
    }

    private static void addApplications(Expression expression, List<String> terms) {
        if (expression instanceof FunctionInvocation && !terms.contains(expression.toString()))
            terms.add(expression.toString());
        for (Expression child : expression.getChildren())
            addApplications(child, terms);
    }

    /**
     * Variables whose own value an expression constrains: those outside the arguments of ghost and state applications.
     * In {@code b == size(x)} that is {@code b}, while {@code x} only names the object whose size is meant
     */
    public static List<String> valueVariablesOf(Expression expression) {
        List<String> names = new ArrayList<>();
        addValueVariables(expression, names);
        return names;
    }

    private static void addValueVariables(Expression expression, List<String> names) {
        if (expression instanceof FunctionInvocation)
            return;
        if (expression instanceof Var var && !names.contains(var.getName()))
            names.add(var.getName());
        for (Expression child : expression.getChildren())
            addValueVariables(child, names);
    }
}
