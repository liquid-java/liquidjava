package liquidjava.rj_language.ast.formatter;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.FunctionInvocation;
import liquidjava.rj_language.ast.Var;
import liquidjava.utils.Utils;

/** Simplifies unambiguous qualified names within one expression */
final class ExpressionNameResolver {
    private final Set<String> variables = new HashSet<>();
    private final Set<String> functions = new HashSet<>();
    private final DisplayNames names;

    private ExpressionNameResolver(DisplayNames names) {
        this.names = names;
    }

    public static ExpressionNameResolver forExpression(Expression expression, DisplayNames names) {
        ExpressionNameResolver resolver = new ExpressionNameResolver(names);
        resolver.collect(expression);
        return resolver;
    }

    public String resolveVariable(String name) {
        String formatted = names.get(name);
        // a field or call argument is always written as in code, as its binder and the counterexample write it
        if (VariableFormatter.isField(name) || ArgumentDisplayNames.argumentCode(name) != null)
            return formatted;
        return isAmbiguous(name, variables, ExpressionNameResolver::getVariableSimpleName) ? formatted
                : Utils.getSimpleName(formatted);
    }

    public String resolveFunction(String name) {
        return isAmbiguous(name, functions, Utils::getSimpleName) ? name : Utils.getSimpleName(name);
    }

    private void collect(Expression expression) {
        if (expression instanceof Var var)
            variables.add(var.getName());
        else if (expression instanceof FunctionInvocation fun)
            functions.add(fun.getName());
        expression.getChildren().forEach(this::collect);
    }

    private static String getVariableSimpleName(String name) {
        return Utils.getSimpleName(VariableFormatter.withoutInstance(name));
    }

    private static boolean isAmbiguous(String name, Set<String> names, Function<String, String> getSimpleName) {
        String simpleName = getSimpleName.apply(name);
        return names.stream().anyMatch(other -> !other.equals(name) && getSimpleName.apply(other).equals(simpleName));
    }
}
