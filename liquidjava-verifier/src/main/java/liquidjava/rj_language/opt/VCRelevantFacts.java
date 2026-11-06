package liquidjava.rj_language.opt;

import static liquidjava.rj_language.opt.VCSimplificationUtils.copyWithRefinement;
import static liquidjava.rj_language.opt.VCSimplificationUtils.factsOf;
import static liquidjava.rj_language.opt.VCSimplificationUtils.variablesOf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.FunctionInvocation;
import liquidjava.rj_language.ast.Var;

/**
 * Keeps in a state error's Found only the facts the expected state depends on, for display: the solver's VC is not
 * changed. Each fact is split into the symbols it constrains: a ghost function applied to an object, such as the state
 * set {@code state1(x)} that both {@code open(x)} and {@code state1(x) == state1(y)} constrain, or a variable whose own
 * value it constrains. Different ghost functions, or the same one over different objects, are different solver terms,
 * so facts that share no symbol with expected, directly or through other facts, are independent of it and are left out.
 * An object whose value a fact constrains, such as {@code x == y}, or that an alias or unknown function takes, is one
 * symbol with all its ghost functions, so its facts are never split. Nothing changes when every fact or none is
 * relevant
 */
public class VCRelevantFacts implements VCSimplificationPass {

    private final Expression expected;
    private final Function<String, Set<String>> ghostFunctionsOf;

    /**
     * @param ghostFunctionsOf
     *            the ghost functions an invocation name stands for: a state's state set function, or the ghost itself;
     *            none for any other name
     */
    public VCRelevantFacts(Expression expected, Function<String, Set<String>> ghostFunctionsOf) {
        this.expected = expected;
        this.ghostFunctionsOf = ghostFunctionsOf;
    }

    @Override
    public String getName() {
        return "Relevant to Expected";
    }

    @Override
    public VCImplication apply(VCImplication implication) {
        List<List<Expression>> rowFacts = new ArrayList<>();
        for (VCImplication row = implication; row != null; row = row.getNext())
            rowFacts.add(factsOf(row.getRefinement().getExpression()));
        Set<String> values = new HashSet<>();
        addSymbols(expected, new HashMap<>(), values);
        rowFacts.forEach(facts -> facts.forEach(fact -> addSymbols(fact, new HashMap<>(), values)));

        Set<String> linked = symbolsOf(expected, values);
        List<Set<String>> unlinked = new ArrayList<>();
        rowFacts.forEach(facts -> facts.forEach(fact -> unlinked.add(symbolsOf(fact, values))));
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Iterator<Set<String>> it = unlinked.iterator(); it.hasNext();) {
                Set<String> symbols = it.next();
                if (symbols.stream().anyMatch(linked::contains)) {
                    linked.addAll(symbols);
                    it.remove();
                    changed = true;
                }
            }
        }

        List<List<Expression>> kept = rowFacts.stream().map(facts -> facts.stream()
                .filter(fact -> symbolsOf(fact, values).stream().anyMatch(linked::contains)).toList()).toList();
        int relevant = kept.stream().mapToInt(List::size).sum();
        if (relevant == 0 || relevant == rowFacts.stream().mapToInt(List::size).sum())
            return implication;
        return rebuild(implication, rowFacts.iterator(), kept.iterator());
    }

    // The rows with their relevant facts, leaving out those that keep none
    private VCImplication rebuild(VCImplication row, Iterator<List<Expression>> all, Iterator<List<Expression>> kept) {
        if (row == null)
            return null;
        List<Expression> rowAll = all.next(), rowKept = kept.next();
        VCImplication next = rebuild(row.getNext(), all, kept);
        if (rowKept.isEmpty())
            return next;
        Expression refinement = rowKept.size() == rowAll.size() ? row.getRefinement().getExpression().clone()
                : rowKept.stream().map(Expression::clone)
                        .reduce((first, second) -> new BinaryExpression(first, "&&", second)).get();
        VCImplication result = copyWithRefinement(row, new Predicate(refinement));
        result.setNext(next);
        return result;
    }

    // The symbols an expression constrains: f(x) for a ghost function over an object, or the object itself when values
    // has it, and the variables whose value it constrains
    private Set<String> symbolsOf(Expression expression, Set<String> values) {
        Map<String, String> applications = new HashMap<>();
        Set<String> symbols = new HashSet<>();
        addSymbols(expression, applications, symbols);
        applications.forEach((application, object) -> symbols.add(values.contains(object) ? object : application));
        return symbols;
    }

    // Adds each ghost function applied to a variable, as f(x) mapped to x, to applications, and the variables whose
    // value the expression constrains, or that an unknown function takes, to values
    private void addSymbols(Expression expression, Map<String, String> applications, Set<String> values) {
        if (expression instanceof FunctionInvocation invocation) {
            Set<String> functions = ghostFunctionsOf.apply(invocation.getName());
            if (!functions.isEmpty() && invocation.getArgs().size() == 1
                    && invocation.getArgs().get(0)instanceof Var object)
                functions.forEach(
                        function -> applications.put(function + "(" + object.getName() + ")", object.getName()));
            else
                values.addAll(variablesOf(invocation));
            return;
        }
        if (expression instanceof Var var)
            values.add(var.getName());
        for (Expression child : expression.getChildren())
            addSymbols(child, applications, values);
    }
}
