package liquidjava.smt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import liquidjava.processor.context.Context;
import liquidjava.processor.context.GhostFunction;
import liquidjava.processor.context.RefinedVariable;
import liquidjava.rj_language.ast.AliasInvocation;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Enum;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.FunctionInvocation;
import liquidjava.rj_language.ast.Ite;
import liquidjava.rj_language.ast.LiteralBoolean;
import liquidjava.rj_language.ast.LiteralChar;
import liquidjava.rj_language.ast.LiteralInt;
import liquidjava.rj_language.ast.LiteralLong;
import liquidjava.rj_language.ast.LiteralReal;
import liquidjava.rj_language.ast.LiteralString;
import liquidjava.rj_language.ast.UnaryExpression;
import liquidjava.rj_language.ast.Var;
import liquidjava.rj_language.visitors.ExpressionVisitor;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.reference.CtTypeReference;

/**
 * Results of solver queries, kept across verification runs. A long-lived process such as the language server verifies
 * the whole program again after every save, and asks the solver mostly the same queries each time.
 * <p>
 * A query is identified by everything {@link TranslatorToZ3} reads to build it: the expression, node by node, the type
 * and supertypes of each of its variables, and the enums, ghosts and ghost states of the context. Variables are renamed
 * in order of appearance, so that a query whose variables were numbered differently by an earlier edit is still found.
 * Names that denote enum constants are values, not variables, and keep their name. A counterexample names the
 * variables, so a cached one is only reused when the names are the same; otherwise a caller that needs one asks the
 * solver again. Unknown results and errors are never kept. Results only live as long as the process, so the verifier
 * and the solver cannot change under them.
 */
final class QueryCache {

    private static final int MAX_RESULTS = 50_000;

    private static final Map<String, Entry> RESULTS = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Entry> eldest) {
            return size() > MAX_RESULTS;
        }
    });

    /**
     * @param text
     *            the query with its variables renamed
     * @param names
     *            the variables of the query, in the order they were renamed
     */
    record Key(String text, List<String> names) {
    }

    /** A result, whose counterexample names the variables in {@code names} */
    private record Entry(SMTResult result, List<String> names) {
    }

    private QueryCache() {
    }

    static Key key(Expression expression, Context context) {
        Map<String, CtTypeReference<?>> types = context.getContext();
        List<String> occurrences = new ArrayList<>();
        expression.getVariableNames(occurrences);
        Set<String> constants = enumConstants(types.values());
        List<String> names = occurrences.stream().distinct().filter(n -> types.containsKey(n) && !constants.contains(n))
                .toList();

        StringBuilder text = new StringBuilder(expression.accept(new Canonical(names))).append('\n');
        Map<String, List<String>> supertypes = new LinkedHashMap<>();
        for (RefinedVariable variable : context.getAllVariablesWithSupertypes())
            if (names.contains(variable.getName()))
                supertypes.computeIfAbsent(variable.getName(), k -> new ArrayList<>()).add(
                        variable.getSuperTypes().stream().map(QueryCache::describe).collect(Collectors.joining(", ")));
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            text.append('@').append(i).append(": ").append(describe(types.get(name)));
            supertypes.getOrDefault(name, List.of()).forEach(s -> text.append(" <: ").append(s));
            text.append('\n');
        }
        // every enum of the context, as their constants can be named in the query
        types.values().stream().filter(CtTypeReference::isEnum).map(QueryCache::describe).distinct()
                .forEach(e -> text.append("enum ").append(e).append('\n'));
        for (GhostFunction ghost : context.getGhosts())
            text.append("ghost ").append(describe(ghost)).append('\n');
        for (GhostFunction state : context.getGhostStates())
            text.append("state ").append(describe(state)).append('\n');
        return new Key(text.toString(), names);
    }

    /**
     * The result kept for {@code key}, or nothing if there is none or it cannot be used: a counterexample is only
     * reused when the variables have the same names
     */
    static Optional<SMTResult> get(Key key, boolean needsCounterexample) {
        Entry entry = RESULTS.get(key.text());
        if (entry == null)
            return Optional.empty();
        if (entry.result().isOk() || entry.names().equals(key.names()))
            return Optional.of(entry.result());
        return needsCounterexample ? Optional.empty() : Optional.of(SMTResult.error(new Counterexample(List.of())));
    }

    static void put(Key key, SMTResult result) {
        RESULTS.put(key.text(), new Entry(result, key.names()));
    }

    /**
     * Fails if {@code recomputed} differs from {@code cached}, with an {@link AssertionError} so that code that
     * recovers from exceptions cannot hide it
     */
    static void checkSame(Key key, SMTResult cached, SMTResult recomputed) {
        boolean same = cached.isOk() == recomputed.isOk() && (cached.isOk() || cached.getCounterexample().isEmpty()
                || cached.getCounterexample().equals(recomputed.getCounterexample()));
        if (!same)
            throw new AssertionError(
                    String.format("The solver query cache returned %s but recomputing gives %s for%n%s",
                            describe(cached), describe(recomputed), key.text()));
    }

    /** The names the translator binds to the constants of the enums, which denote values rather than variables */
    private static Set<String> enumConstants(Collection<CtTypeReference<?>> types) {
        Set<String> constants = new HashSet<>();
        for (CtTypeReference<?> type : types)
            if (type.isEnum() && type.getDeclaration()instanceof CtEnum<?> declaration)
                declaration.getEnumValues().forEach(
                        v -> constants.add(TranslatorContextToZ3.enumConstantName(declaration, v.getSimpleName())));
        return constants;
    }

    private static String describe(SMTResult result) {
        return result.isOk() ? "ok" : "error with " + result.getCounterexample();
    }

    /** What the translator reads from a type: its names, and the constants of an enum */
    private static String describe(CtTypeReference<?> type) {
        if (type == null)
            return "?";
        if (type.isEnum() && type.getDeclaration()instanceof CtEnum<?> declaration)
            return type.getQualifiedName() + declaration.getEnumValues().stream().map(v -> v.getSimpleName()).toList();
        return type.getQualifiedName() + " " + type;
    }

    private static String describe(GhostFunction ghost) {
        return ghost.getQualifiedName() + ghost.getParametersTypes().stream().map(Object::toString).toList() + " "
                + ghost.getReturnType();
    }

    /** The expression node by node, with its variables renamed in order of appearance */
    private static final class Canonical implements ExpressionVisitor<String> {

        private final List<String> names;

        Canonical(List<String> names) {
            this.names = names;
        }

        private String all(List<Expression> expressions) {
            return expressions.stream().map(e -> e.accept(this)).collect(Collectors.joining(" "));
        }

        @Override
        public String visitAliasInvocation(AliasInvocation alias) {
            return "(alias " + alias.getName() + " " + all(alias.getArgs()) + ")";
        }

        @Override
        public String visitBinaryExpression(BinaryExpression exp) {
            return "(" + exp.getOperator() + " " + exp.getFirstOperand().accept(this) + " "
                    + exp.getSecondOperand().accept(this) + ")";
        }

        @Override
        public String visitFunctionInvocation(FunctionInvocation fun) {
            return "(call " + fun.getName() + " " + all(fun.getArgs()) + ")";
        }

        @Override
        public String visitIte(Ite ite) {
            return "(ite " + ite.getCondition().accept(this) + " " + ite.getThen().accept(this) + " "
                    + ite.getElse().accept(this) + ")";
        }

        @Override
        public String visitLiteralInt(LiteralInt lit) {
            return "(int " + lit.getValue() + ")";
        }

        @Override
        public String visitLiteralLong(LiteralLong lit) {
            return "(long " + lit.getValue() + ")";
        }

        @Override
        public String visitLiteralBoolean(LiteralBoolean lit) {
            return "(bool " + lit.isBooleanTrue() + ")";
        }

        @Override
        public String visitLiteralChar(LiteralChar lit) {
            return "(char " + (int) lit.getValue() + ")";
        }

        @Override
        public String visitLiteralReal(LiteralReal lit) {
            return "(real " + Double.doubleToLongBits(lit.getValue()) + ")";
        }

        @Override
        public String visitLiteralString(LiteralString lit) {
            return "(string " + lit.toString().length() + ":" + lit + ")";
        }

        @Override
        public String visitUnaryExpression(UnaryExpression exp) {
            return "(" + exp.getOp() + " " + exp.getExpression().accept(this) + ")";
        }

        @Override
        public String visitEnum(Enum en) {
            Expression literal = en.getResolvedLiteral();
            return "(const " + en.getTypeName() + " " + en.getConstName() + " "
                    + (literal == null ? "-" : literal.accept(this)) + ")";
        }

        @Override
        public String visitVar(Var var) {
            int index = names.indexOf(var.getName());
            return index < 0 ? "(free " + var.getName().length() + ":" + var.getName() + ")" : "@" + index;
        }
    }
}
