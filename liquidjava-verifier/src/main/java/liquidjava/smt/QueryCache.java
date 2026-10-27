package liquidjava.smt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import liquidjava.utils.CacheMode;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.reference.CtTypeReference;

/**
 * Results of solver queries, kept across verification runs. A long-lived process such as the language server verifies
 * the whole program again after every save, and asks the solver mostly the same queries each time.
 * <p>
 * A query is identified by everything {@link TranslatorToZ3} reads to build it: the expression, node by node, the type
 * and supertypes of each of its variables, the enums and the ghosts and ghost states of the context, and the solver
 * configuration. Variables are renamed in order of appearance, so that a query whose variables were numbered
 * differently by an earlier edit is still found. A counterexample names the variables, so a cached one is only reused
 * when the names are the same; otherwise a caller that needs one asks the solver again. Unknown results and errors are
 * never kept.
 */
final class QueryCache {

    /** Changes when the way queries are solved changes */
    private static final String SOLVER = "simple solver with rlimit " + TranslatorToZ3.SIMPLE_SOLVER_RLIMIT
            + ", then default solver";

    private static final int MAX_RESULTS = 50_000;

    private static final Map<String, Result> RESULTS = Collections
            .synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Result> eldest) {
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

    /** {@code counterexample} is null when the query holds; it names the variables in {@code names} */
    private record Result(Counterexample counterexample, List<String> names) {
    }

    private QueryCache() {
    }

    static Key key(Expression expression, Context context) {
        Map<String, CtTypeReference<?>> types = context.getContext();
        List<String> occurrences = new ArrayList<>();
        expression.getVariableNames(occurrences);
        List<String> names = occurrences.stream().distinct().filter(types::containsKey).toList();

        StringBuilder text = new StringBuilder(SOLVER).append('\n');
        text.append(expression.accept(new Canonical(names))).append('\n');
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            text.append('@').append(i).append(": ").append(describe(types.get(name)));
            for (RefinedVariable variable : context.getAllVariablesWithSupertypes())
                if (variable.getName().equals(name))
                    text.append(" <: ").append(variable.getSuperTypes().stream().map(QueryCache::describe)
                            .collect(Collectors.joining(", ")));
            text.append('\n');
        }
        // every enum of the context, as their constants can be named in the query
        for (CtTypeReference<?> type : types.values())
            if (type.isEnum())
                text.append("enum ").append(describe(type)).append('\n');
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
        if (CacheMode.current() == CacheMode.OFF)
            return Optional.empty();
        Result result = RESULTS.get(key.text());
        if (result == null)
            return Optional.empty();
        if (result.counterexample() == null)
            return Optional.of(SMTResult.ok());
        if (result.names().equals(key.names()))
            return Optional.of(SMTResult.error(result.counterexample()));
        return needsCounterexample ? Optional.empty() : Optional.of(SMTResult.error(new Counterexample(List.of())));
    }

    static void put(Key key, SMTResult result) {
        if (CacheMode.current() != CacheMode.OFF)
            RESULTS.put(key.text(), new Result(result.getCounterexample(), key.names()));
    }

    /** Fails if {@code recomputed} differs from the result kept for {@code key} */
    static void checkSame(Key key, SMTResult cached, SMTResult recomputed) {
        CacheMode.checkSame("solver query", cached.isOk(), recomputed.isOk());
        if (cached.isError() && !cached.getCounterexample().isEmpty())
            CacheMode.checkSame("solver query", cached.getCounterexample(), recomputed.getCounterexample());
    }

    /** What the translator reads from a type: its name, and the constants of an enum */
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
