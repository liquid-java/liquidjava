package liquidjava.diagnostics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import liquidjava.api.CommandLineLauncher;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.GroupExpression;
import liquidjava.utils.Utils;
import spoon.reflect.cu.SourcePosition;

/**
 * Centralised debug-mode logging for verification activity. Output is gated on the global {@code --debug} / {@code -d}
 * flag and is purely informational.
 *
 * <p>
 * Layers of output, from outermost to innermost:
 * <ul>
 * <li>{@link #info} — verification context (caller-level predicates, source position).</li>
 * <li>{@link #smtStart} — premises and conclusion as fed to Z3.</li>
 * <li>{@link #smtUnsat} / {@link #smtSat} / {@link #smtUnknown} — solver outcome.</li>
 * </ul>
 */
public final class DebugLog {

    private static final String INFO_TAG = Colors.YELLOW + "[INFO]" + Colors.RESET;
    private static final String SMT_TAG = Colors.BLUE + "[SMT]" + Colors.RESET;

    private DebugLog() {
    }

    public static boolean enabled() {
        return CommandLineLauncher.cmdArgs.debugMode;
    }

    public static void info(String verb, Object expected, Object found, SourcePosition position) {
        if (!enabled()) {
            return;
        }
        String exp = Utils.getExpressionFromPosition(position);
        String where = position.getFile().getName() + ":" + position.getLine();
        System.out.println(INFO_TAG + " " + verb + ": " + found + " <: " + expected);
        System.out.println(INFO_TAG + "   at " + where + (exp != null ? "  on '" + exp + "'" : ""));
    }

    public static void smtStart(Predicate premises, Predicate conclusion) {
        if (!enabled()) {
            return;
        }
        printPremisesByVariable(premises);
        System.out.println(SMT_TAG + " conclusion: " + conclusion);
    }

    /**
     * Splits the premise conjunction into top-level conjuncts and groups them by the variable they constrain. Conjuncts
     * mentioning a single variable are printed under that variable; conjuncts mentioning zero or several variables are
     * printed under a {@code <shared>} bucket.
     */
    private static void printPremisesByVariable(Predicate premises) {
        Expression root = premises.getExpression();
        List<Expression> conjuncts = new ArrayList<>();
        flattenConjunction(root, conjuncts);

        Map<String, List<String>> grouped = new LinkedHashMap<>();
        for (Expression c : conjuncts) {
            List<String> names = new ArrayList<>();
            c.getVariableNames(names);
            Set<String> unique = new LinkedHashSet<>(names);
            String key;
            if (unique.isEmpty()) {
                key = "";
            } else if (unique.size() == 1) {
                key = unique.iterator().next();
            } else {
                key = "<shared>";
            }
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(c.toString());
        }

        System.out.println(SMT_TAG + " premises:");
        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            String prefix = entry.getKey().isEmpty() ? "" : entry.getKey() + ": ";
            for (String c : entry.getValue()) {
                System.out.println(SMT_TAG + "   " + prefix + c);
            }
        }
    }

    private static void flattenConjunction(Expression e, List<Expression> out) {
        if (e instanceof GroupExpression g) {
            flattenConjunction(g.getExpression(), out);
            return;
        }
        if (e instanceof BinaryExpression b && "&&".equals(b.getOperator())) {
            flattenConjunction(b.getFirstOperand(), out);
            flattenConjunction(b.getSecondOperand(), out);
            return;
        }
        out.add(e);
    }

    public static void smtUnsat() {
        if (!enabled()) {
            return;
        }
        System.out.println(SMT_TAG + " result: " + Colors.GREEN + "UNSAT (subtype holds)" + Colors.RESET);
    }

    public static void smtSat(Object counterexample) {
        if (!enabled()) {
            return;
        }
        System.out.println(SMT_TAG + " result: " + Colors.RED + "SAT (subtype fails)" + Colors.RESET
                + "; counterexample: " + counterexample);
    }

    public static void smtUnknown() {
        if (!enabled()) {
            return;
        }
        System.out.println(SMT_TAG + " result: " + Colors.YELLOW + "UNKNOWN (treated as OK)" + Colors.RESET);
    }
}
