package liquidjava.diagnostics;

import java.util.ArrayList;
import java.util.List;
import liquidjava.api.CommandLineLauncher;
import liquidjava.processor.VCImplication;
import liquidjava.processor.context.Context;
import liquidjava.processor.context.ContextHistory;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.GroupExpression;
import liquidjava.utils.Utils;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtTypeReference;

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

    private static final String SMT_TAG = Colors.BLUE + "[SMT]" + Colors.RESET;
    private static final String SMT_CHECK = Colors.SALMON + "[SMT CHECK]" + Colors.RESET;
    private static final String SMP_TAG = Colors.YELLOW + "[SMP]" + Colors.RESET;
    private static final String CTX_TAG = Colors.BRIGHT_MAGENTA + "[CTX]" + Colors.RESET;

    private DebugLog() {
    }

    public static boolean enabled() {
        return CommandLineLauncher.cmdArgs.debugMode;
    }

    /** Gate for the full-context dump, driven by the {@code -a} / {@code --all-context} flag (independent of debug). */
    public static boolean contextEnabled() {
        return CommandLineLauncher.cmdArgs.printContext;
    }

    /**
     * One-line header for a verification check: emits the absolute file path + line so terminals (iTerm2, VS Code,
     * WezTerm, …) make it ⌘/Ctrl-clickable. Replaces the older two-line {@code info()} prints.
     */
    public static void smtVerifying(SourcePosition position) {
        if (!enabled() || position == null) {
            return;
        }
        String where = position.getFile().getAbsolutePath() + ":" + position.getLine();
        String exp = Utils.getExpressionFromPosition(position);
        System.out.println(SMT_CHECK);
        System.out.println(SMT_TAG + " Verifying " + Colors.CYAN + where + Colors.RESET
                + (exp != null ? "  on '" + exp + "'" : ""));
    }

    private static final String SEPARATOR = Colors.GREY + "   ────────────────────────────────────────" + Colors.RESET;

    /**
     * Flat-predicate fallback: prints top-level conjuncts in order with no per-variable grouping. Used by SMT entry
     * points that don't carry the structured per-variable {@link VCImplication} chain (e.g. ExpressionSimplifier).
     */
    public static void smtStart(Predicate premises, Predicate conclusion) {
        if (!enabled()) {
            return;
        }
        List<Expression> conjuncts = new ArrayList<>();
        flattenConjunction(premises.getExpression(), conjuncts);
        System.out.println(SMT_TAG);
        for (Expression c : conjuncts) {
            System.out.println(SMT_TAG + "   " + c);
        }
        System.out.println(SMT_TAG + SEPARATOR);
        System.out.println(SMT_TAG + "   " + formatConclusion(conclusion));
    }

    /**
     * Structured form: walks the {@link VCImplication} chain produced by {@code joinPredicates}, printing one line per
     * refined variable with all of its refinements together.
     */
    public static void smtStart(VCImplication chain, Predicate conclusion) {
        smtStart(chain, null, conclusion);
    }

    /**
     * Structured form with an extra unattributed premise (e.g. the "found" type appended in
     * {@code verifySMTSubtypeStates}).
     */
    public static void smtStart(VCImplication chain, Predicate extraPremise, Predicate conclusion) {
        if (!enabled()) {
            return;
        }
        // Pre-compute label widths for column alignment across all printed rows.
        int labelWidth = 0;
        for (VCImplication node = chain; node != null; node = node.getNext()) {
            if (node.getName() == null && node.getType() == null) {
                continue;
            }
            labelWidth = Math.max(labelWidth, plainLabel(node).length());
        }
        if (extraPremise != null && !extraPremise.isBooleanTrue()) {
            labelWidth = Math.max(labelWidth, "found".length());
        }

        System.out.println(SMT_TAG + " ");
        List<String> printedRefinements = new ArrayList<>();
        for (VCImplication node = chain; node != null; node = node.getNext()) {
            if (node.getName() == null && node.getType() == null) {
                continue; // skip the empty trailing tail node
            }
            String refinement = formatRefinement(node.getRefinement());
            printedRefinements.add(refinement);
            System.out.println(SMT_TAG + "   " + paintLabel(node, labelWidth) + "  " + refinement);
        }
        if (extraPremise != null && !extraPremise.isBooleanTrue()) {
            String extra = formatRefinement(extraPremise);
            // Skip when the appended "found" type is identical to a premise we just printed
            // (common in verifySMTSubtypeStates when `type` IS the variable's current refinement).
            if (!printedRefinements.contains(extra)) {
                String label = Colors.GREY + padRight("found", labelWidth) + Colors.RESET;
                System.out.println(SMT_TAG + "   " + label + "  " + extra);
            }
        }
        System.out.println(SMT_TAG + SEPARATOR);
        System.out.println(SMT_TAG + " " + formatConclusion(conclusion));
    }

    /**
     * Print the simplifier input and output side by side. This keeps the raw expression visible in debug traces while
     * callers continue using the simplified expression for user-facing diagnostics. Long predicates are split on
     * top-level {@code &&} so each conjunct lands on its own line.
     */
    public static void simplification(Expression input, Expression output) {
        if (!enabled()) {
            return;
        }
        printSplitConjunction("Before simplification:", Colors.YELLOW, input);
        printSplitConjunction("After simplification: ", Colors.BOLD_YELLOW, output);
    }

    private static void printSplitConjunction(String header, String color, Expression exp) {
        List<Expression> conjuncts = new ArrayList<>();
        flattenConjunction(exp, conjuncts);
        if (conjuncts.size() <= 1) {
            System.out.println(SMP_TAG + " " + header + " " + color + exp + Colors.RESET);
            return;
        }
        System.out.println(SMP_TAG + " " + header);
        String joiner = " " + Colors.GREY + "&&" + Colors.RESET;
        for (int i = 0; i < conjuncts.size(); i++) {
            String suffix = (i < conjuncts.size() - 1) ? joiner : "";
            System.out.println(SMP_TAG + "   " + color + conjuncts.get(i) + Colors.RESET + suffix);
        }
    }

    private static final String PASS_NAME_COLOR = Colors.GOLD;
    private static final int PASS_NAME_WIDTH = 28;
    private static int simplificationPass;
    private static String previousSimplification;

    /**
     * Start a simplification log. DebugLog owns the running pass number and the previous expression snapshot because
     * both are only needed for debug output.
     */
    public static void simplificationStart(Expression input) {
        if (!enabled()) {
            return;
        }
        previousSimplification = input.toString();
        simplificationPass = 0;
        printSimplificationPass(simplificationPass, "initial expression", previousSimplification);
    }

    /**
     * One line per simplifier phase.
     */
    public static void simplificationPass(String name, Expression result) {
        if (!enabled()) {
            return;
        }
        String resultStr = result.toString();
        printSimplificationPass(++simplificationPass, name, previousSimplification, resultStr);
        previousSimplification = resultStr;
    }

    /**
     * Prints {@code (no change)} when the step left the expression unchanged, and otherwise emits a unified-diff-style
     * pair (red {@code -} for the previous expression with removed tokens highlighted, green {@code +} for the new one
     * with added tokens highlighted), so substitutions inside a long predicate are obvious at a glance.
     *
     * <p>
     * {@code previous} is taken as a string rather than an {@link Expression} because the simplifier mutates the AST in
     * place: caching an {@code Expression} reference and re-stringifying it after a later pass would yield the
     * already-mutated form, masking real changes as "no change".
     */
    private static void printSimplificationPass(int pass, String name, String previous, String result) {
        if (previous != null && previous.equals(result)) {
            System.out.printf("%s pass %02d: %s %s(no change)%s%n", SMP_TAG, pass, paintPassName(name), Colors.GREY,
                    Colors.RESET);
            return;
        }
        System.out.printf("%s pass %02d: %s%s%s%n", SMP_TAG, pass, PASS_NAME_COLOR, name, Colors.RESET);
        if (previous == null) {
            System.out.printf("%s   %s%n", SMP_TAG, result);
            return;
        }
        String[] diff = wordDiff(previous, result);
        System.out.printf("%s   %s-%s %s%n", SMP_TAG, Colors.RED, Colors.RESET, diff[0]);
        System.out.printf("%s   %s+%s %s%n", SMP_TAG, Colors.GREEN, Colors.RESET, diff[1]);
    }

    private static void printSimplificationPass(int pass, String name, String result) {
        System.out.printf("%s pass %02d: %s%n %s%n", SMP_TAG, pass, paintPassName(name), result);
    }

    /**
     * Color the pass name without breaking column alignment: pad to {@link #PASS_NAME_WIDTH} first, then wrap only the
     * visible characters in {@link #PASS_NAME_COLOR}. The trailing spaces stay uncolored so {@code printf}'s width
     * accounting stays correct.
     */
    private static String paintPassName(String name) {
        int pad = Math.max(0, PASS_NAME_WIDTH - name.length());
        return PASS_NAME_COLOR + name + Colors.RESET + " ".repeat(pad);
    }

    /**
     * Word-level LCS diff. Returns {@code [previousColored, currentColored]} where tokens that don't appear in the LCS
     * are wrapped in red (for the previous string) and green (for the current string). Splitting on a single space is
     * intentional — the {@link Expression#toString()} output spaces operators and operands.
     */
    private static String[] wordDiff(String previous, String current) {
        String[] prev = previous.split(" ");
        String[] curr = current.split(" ");
        int[][] dp = new int[prev.length + 1][curr.length + 1];
        for (int i = 1; i <= prev.length; i++) {
            for (int j = 1; j <= curr.length; j++) {
                if (prev[i - 1].equals(curr[j - 1])) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        boolean[] prevKept = new boolean[prev.length];
        boolean[] currKept = new boolean[curr.length];
        int i = prev.length;
        int j = curr.length;
        while (i > 0 && j > 0) {
            if (prev[i - 1].equals(curr[j - 1])) {
                prevKept[i - 1] = true;
                currKept[j - 1] = true;
                i--;
                j--;
            } else if (dp[i - 1][j] >= dp[i][j - 1]) {
                i--;
            } else {
                j--;
            }
        }
        return new String[] { colorizeDiff(prev, prevKept, Colors.RED), colorizeDiff(curr, currKept, Colors.GREEN) };
    }

    private static String colorizeDiff(String[] tokens, boolean[] kept, String color) {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < tokens.length; k++) {
            if (k > 0) {
                sb.append(' ');
            }
            if (kept[k]) {
                sb.append(tokens[k]);
            } else {
                sb.append(color).append(tokens[k]).append(Colors.RESET);
            }
        }
        return sb.toString();
    }

    private static String plainLabel(VCImplication node) {
        return node.getName() + " : " + simpleType(node.getType());
    }

    private static String paintLabel(VCImplication node, int width) {
        String name = node.getName();
        String type = simpleType(node.getType());
        String padded = padRight(name + " : " + type, width);
        // Color only the name; keep alignment by computing padding on the plain string.
        String coloredName = Colors.CYAN + name + Colors.RESET;
        String coloredType = Colors.GREY + type + Colors.RESET;
        String tail = padded.substring((name + " : " + type).length()); // padding spaces
        return coloredName + Colors.GREY + " : " + Colors.RESET + coloredType + tail;
    }

    private static String simpleType(CtTypeReference<?> type) {
        if (type == null) {
            return "?";
        }
        String qual = type.getQualifiedName();
        return qual.contains(".") ? Utils.getSimpleName(qual) : qual;
    }

    private static String padRight(String s, int width) {
        if (s.length() >= width) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s);
        for (int i = s.length(); i < width; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    /**
     * Render a refinement so multi-conjunct predicates are unambiguous on a single line: each top-level conjunct is
     * wrapped in parens and joined with ∧.
     */
    private static String formatRefinement(Predicate p) {
        List<Expression> conjuncts = new ArrayList<>();
        flattenConjunction(p.getExpression(), conjuncts);
        if (conjuncts.size() <= 1) {
            return p.toString();
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < conjuncts.size(); i++) {
            if (i > 0) {
                sb.append(Colors.GREY).append(" ∧ ").append(Colors.RESET);
            }
            sb.append('(').append(conjuncts.get(i)).append(')');
        }
        return sb.toString();
    }

    /**
     * Conclusion needs its own painter: nesting {@link #formatRefinement} (which already emits ANSI {@code RESET}
     * around its operators) inside an outer color would clear the outer color after the first inner reset, leaving the
     * tail of the line uncoloured. Paint each conjunct individually instead.
     */
    private static String formatConclusion(Predicate p) {
        List<Expression> conjuncts = new ArrayList<>();
        flattenConjunction(p.getExpression(), conjuncts);
        if (conjuncts.size() <= 1) {
            return Colors.BOLD_YELLOW + p + Colors.RESET;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < conjuncts.size(); i++) {
            if (i > 0) {
                sb.append(Colors.GREY).append(" ∧ ").append(Colors.RESET);
            }
            sb.append(Colors.BOLD_YELLOW).append('(').append(conjuncts.get(i)).append(')').append(Colors.RESET);
        }
        return sb.toString();
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
        System.out.println(SMT_TAG + " Result: " + Colors.GREEN + "UNSAT (subtype holds)" + Colors.RESET);
    }

    public static void smtSat(Object counterexample) {
        if (!enabled()) {
            return;
        }
        String header = SMT_TAG + " Result: " + Colors.RED + "SAT (subtype fails)" + Colors.RESET;
        String pretty = formatCounterexample(counterexample);
        if (pretty == null) {
            System.out.println(header);
        } else if (pretty.contains("\n")) {
            System.out.println(header + Colors.GREY + " — counterexample:" + Colors.RESET);
            System.out.println(pretty);
        } else {
            System.out.println(header + Colors.GREY + " — counterexample: " + Colors.RESET + pretty);
        }
    }

    /**
     * Render a {@link liquidjava.smt.Counterexample} as {@code lhs = value} pairs. Single assignment goes inline;
     * multiple assignments are listed one per indented line. Returns {@code null} when there is nothing useful to show
     * — caller prints just the SAT header.
     */
    private static String formatCounterexample(Object counterexample) {
        if (!(counterexample instanceof liquidjava.smt.Counterexample ce)) {
            return counterexample == null ? null : counterexample.toString();
        }
        var pairs = ce.assignments();
        if (pairs == null || pairs.isEmpty()) {
            return null;
        }
        if (pairs.size() == 1) {
            var p = pairs.get(0);
            return p.first() + " = " + p.second();
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pairs.size(); i++) {
            var p = pairs.get(i);
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(SMT_TAG).append("     ").append(p.first()).append(" = ").append(p.second());
        }
        return sb.toString();
    }

    public static void smtUnknown() {
        if (!enabled()) {
            return;
        }
        System.out.println(SMT_TAG + " Result: " + Colors.YELLOW + "UNKNOWN (treated as OK)" + Colors.RESET);
    }

    /**
     * Print the result of an SMT check whose {@code smtStart} was emitted by the caller (e.g. VCChecker's structured
     * print). {@link liquidjava.smt.SMTResult} doesn't preserve UNKNOWN, so this maps OK → UNSAT and ERROR → SAT.
     */
    public static void smtResult(liquidjava.smt.SMTResult result) {
        if (!enabled()) {
            return;
        }
        if (result.isError()) {
            smtSat(result.getCounterexample());
        } else {
            smtUnsat();
        }
    }

    /**
     * Print an SMT-side failure (e.g. Z3 sort mismatch) so the trace doesn't end with a dangling header. Caller is
     * still responsible for surfacing the user-facing error.
     */
    public static void smtError(String message) {
        if (!enabled()) {
            return;
        }
        System.out.println(SMT_TAG + " Result: " + Colors.RED + "ERROR" + Colors.RESET + " — "
                + (message == null ? "(no message)" : message));
    }

    public static void contextAtElement(CtElement element, String contextInfo) {
        if (!contextEnabled()) {
            return;
        }
        SourcePosition position = element.getPosition();
        String header;
        if (position != null && position.getFile() != null) {
            // file:line so terminals make it ⌘/Ctrl-clickable (same form as smtVerifying), plus the source line itself.
            String where = position.getFile().getAbsolutePath() + ":" + position.getLine();
            String code = Utils.getExpressionFromPosition(position);
            header = CTX_TAG + " Context at " + Colors.CYAN + where + Colors.RESET
                    + (code != null ? "  \n" + Colors.BOLD_YELLOW + code + Colors.RESET : ":");
        } else {
            header = CTX_TAG + " Context at " + position + ":";
        }
        System.out.println(header);
        System.out.println(contextInfo);
    }

    public static void log(String string) {
        if (!enabled()) {
            return;
        }
        System.out.println(string);

    }
}
