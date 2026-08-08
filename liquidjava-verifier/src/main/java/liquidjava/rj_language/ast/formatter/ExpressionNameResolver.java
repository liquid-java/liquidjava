package liquidjava.rj_language.ast.formatter;

import java.util.HashSet;
import java.util.Set;

import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.FunctionInvocation;
import liquidjava.rj_language.ast.Var;
import liquidjava.utils.Utils;

/** Simplifies unambiguous qualified names within one expression */
final class ExpressionNameResolver {
    private final Set<String> variables = new HashSet<>();
    private final Set<String> functions = new HashSet<>();

    public static ExpressionNameResolver forExpression(Expression expression) {
        ExpressionNameResolver resolver = new ExpressionNameResolver();
        resolver.collect(expression);
        return resolver;
    }

    public String resolveVariable(String name) {
        return resolve(VariableFormatter.format(name), variables);
    }

    public String resolveFunction(String name) {
        return resolve(name, functions);
    }

    private void collect(Expression expression) {
        if (expression instanceof Var var)
            variables.add(VariableFormatter.format(var.getName()));
        else if (expression instanceof FunctionInvocation function)
            functions.add(function.getName());
        expression.getChildren().forEach(this::collect);
    }

    private static String resolve(String name, Set<String> names) {
        String simpleName = Utils.getSimpleName(name);
        boolean ambiguous = names.stream()
                .anyMatch(other -> !other.equals(name) && Utils.getSimpleName(other).equals(simpleName));
        return ambiguous ? name : simpleName;
    }
}
