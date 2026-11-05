package liquidjava.rj_language.opt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.FunctionInvocation;
import liquidjava.rj_language.ast.LiteralBoolean;
import liquidjava.rj_language.ast.UnaryExpression;
import liquidjava.rj_language.ast.Var;
import liquidjava.utils.Utils;

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
     * Terms an expression constrains: its variables and its ghost and state applications, those over variables named as
     * in the SMT model such as {@code size(x)} and the others as written
     */
    public static List<String> termsOf(Expression expression) {
        List<String> terms = variablesOf(expression);
        addApplications(expression, terms);
        return terms;
    }

    private static void addApplications(Expression expression, List<String> terms) {
        if (expression instanceof FunctionInvocation) {
            String term = termName(expression) != null ? termName(expression) : expression.toString();
            if (!terms.contains(term))
                terms.add(term);
        }
        for (Expression child : expression.getChildren())
            addApplications(child, terms);
    }

    /**
     * Name of a variable, or of a ghost application over variables, as in the SMT model; null for other expressions
     */
    public static String termName(Expression expression) {
        if (expression instanceof Var var)
            return var.getName();
        if (expression instanceof FunctionInvocation invocation
                && invocation.getArgs().stream().allMatch(Var.class::isInstance)) {
            String args = invocation.getArgs().stream().map(Expression::toString).collect(Collectors.joining(", "));
            return Utils.getSimpleName(invocation.getName()) + "(" + args + ")";
        }
        return null;
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

    /**
     * Conjuncts of an expression, also splitting negated disjunctions ({@code !(a || b)} is {@code !a && !b}) and
     * dropping double negations, so each fact links only the terms it relates
     */
    public static List<Expression> factsOf(Expression expression) {
        List<Expression> facts = new ArrayList<>();
        if (expression instanceof BinaryExpression binary && "&&".equals(binary.getOperator())) {
            facts.addAll(factsOf(binary.getFirstOperand()));
            facts.addAll(factsOf(binary.getSecondOperand()));
        } else if (expression instanceof UnaryExpression not && "!".equals(not.getOp())
                && not.getExpression()instanceof BinaryExpression binary && "||".equals(binary.getOperator())) {
            facts.addAll(factsOf(new UnaryExpression("!", binary.getFirstOperand())));
            facts.addAll(factsOf(new UnaryExpression("!", binary.getSecondOperand())));
        } else if (expression instanceof UnaryExpression not && "!".equals(not.getOp())
                && not.getExpression()instanceof UnaryExpression inner && "!".equals(inner.getOp())) {
            facts.addAll(factsOf(inner.getExpression()));
        } else {
            facts.add(expression);
        }
        return facts;
    }

    /**
     * The seed terms plus those of every fact sharing a term with them, directly or through other facts. The remaining
     * facts constrain other values only
     */
    public static Set<String> linkedTerms(List<Expression> facts, Collection<String> seed) {
        Set<String> linked = new HashSet<>(seed);
        List<List<String>> unlinked = facts.stream().map(VCSimplificationUtils::termsOf)
                .collect(Collectors.toCollection(ArrayList::new));
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Iterator<List<String>> it = unlinked.iterator(); it.hasNext();) {
                List<String> terms = it.next();
                if (terms.stream().anyMatch(linked::contains)) {
                    linked.addAll(terms);
                    it.remove();
                    changed = true;
                }
            }
        }
        return linked;
    }

    /**
     * The facts of found linked to the terms of expected, as a conjunction, or true when there are none; found as
     * written when every fact is linked
     */
    public static Expression linkedToExpected(Expression found, Expression expected) {
        List<Expression> facts = factsOf(found);
        Set<String> linked = linkedTerms(facts, termsOf(expected));
        List<Expression> linkedFacts = facts.stream().filter(fact -> termsOf(fact).stream().anyMatch(linked::contains))
                .toList();
        if (linkedFacts.size() == facts.size())
            return found;
        return linkedFacts.stream().reduce((first, second) -> new BinaryExpression(first, "&&", second))
                .orElse(new LiteralBoolean(true));
    }
}
