package liquidjava.diagnostics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import liquidjava.processor.context.AliasWrapper;
import liquidjava.rj_language.ast.AliasInvocation;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.Var;
import spoon.reflect.cu.SourcePosition;

/**
 * An alias invocation in a diagnostic and what it stands for
 *
 * @param invocation
 *            the invocation as shown in the diagnostic, e.g. {@code Nat(n)}
 * @param expansion
 *            the definition with the parameters replaced by the arguments, e.g. {@code n >= 0}; aliases it uses are
 *            kept and have their own expansions
 * @param alias
 *            the alias name
 * @param parameters
 *            the parameter names of the alias
 * @param definition
 *            the definition as written in the alias declaration
 * @param declarationPosition
 *            where the alias is declared
 */
public record AliasExpansion(String invocation, String expansion, String alias, List<String> parameters,
        String definition, SourcePosition declarationPosition) {

    /** The alias as it is declared, e.g. {@code Nat(x) = x >= 0} */
    public String declaration() {
        return alias + "(" + String.join(", ", parameters) + ") = " + definition;
    }

    /**
     * The expansions of the alias invocations in an expression, each invocation once in order of appearance, followed
     * by those of the aliases their definitions use
     */
    public static List<AliasExpansion> of(Expression expression, List<AliasWrapper> aliases) {
        Map<String, AliasWrapper> byName = new LinkedHashMap<>();
        for (AliasWrapper alias : aliases)
            byName.put(alias.getName(), alias);
        Map<String, AliasExpansion> expansions = new LinkedHashMap<>();
        collect(expression, byName, expansions, new HashSet<>());
        return new ArrayList<>(expansions.values());
    }

    // Expands the invocations under expression, skipping aliases already being expanded so recursive ones end
    private static void collect(Expression expression, Map<String, AliasWrapper> aliases,
            Map<String, AliasExpansion> expansions, Set<String> expanding) {
        List<AliasInvocation> invocations = new ArrayList<>();
        invocationsOf(expression, invocations);
        Map<String, Expression> bodies = new LinkedHashMap<>();
        for (AliasInvocation invocation : invocations) {
            AliasWrapper alias = aliases.get(invocation.getName());
            // keyed by the invocation itself, since different names can display the same
            String key = invocation.toString();
            if (alias == null || expanding.contains(alias.getName()) || expansions.containsKey(key))
                continue;
            Expression body = substituteArguments(alias, invocation.getArgs());
            expansions.put(key, new AliasExpansion(invocation.toDisplayString(), body.toDisplayString(),
                    alias.getName(), alias.getVarNames(), alias.getDefinition(), alias.getPosition()));
            bodies.put(key, body);
        }
        for (Map.Entry<String, Expression> body : bodies.entrySet()) {
            Set<String> inner = new HashSet<>(expanding);
            inner.add(expansions.get(body.getKey()).alias());
            collect(body.getValue(), aliases, expansions, inner);
        }
    }

    private static void invocationsOf(Expression expression, List<AliasInvocation> invocations) {
        if (expression instanceof AliasInvocation invocation)
            invocations.add(invocation);
        for (Expression child : expression.getChildren())
            invocationsOf(child, invocations);
    }

    // Replaces all parameters at once, through placeholders, so an argument naming another parameter is kept
    private static Expression substituteArguments(AliasWrapper alias, List<Expression> args) {
        Expression body = alias.getClonedPredicate().getExpression();
        List<String> parameters = alias.getVarNames();
        int n = Math.min(parameters.size(), args.size());
        for (int i = 0; i < n; i++)
            body = body.substitute(new Var(parameters.get(i)), new Var("#alias_parameter_" + i));
        for (int i = 0; i < n; i++)
            body = body.substitute(new Var("#alias_parameter_" + i), args.get(i).clone());
        return body;
    }
}
