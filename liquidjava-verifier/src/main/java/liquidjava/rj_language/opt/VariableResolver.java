package liquidjava.rj_language.opt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Enum;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.FunctionInvocation;
import liquidjava.rj_language.ast.LiteralBoolean;
import liquidjava.rj_language.ast.UnaryExpression;
import liquidjava.rj_language.ast.Var;

public class VariableResolver {

    /**
     * Extracts variables with constant values from an expression
     * 
     * @param exp
     * 
     * @returns map from variable names to their values
     */
    public static Map<String, Expression> resolve(Expression exp) {
        Map<String, Expression> map = new HashMap<>();

        // extract variable equalities recursively
        resolveRecursive(exp, map);

        // remove variables that were not used in the expression
        map.entrySet().removeIf(entry -> !hasUsage(exp, entry.getKey(), entry.getValue(), true));

        // transitively resolve variables
        return resolveTransitive(map);
    }

    /**
     * Recursively extracts variable equalities from an expression (e.g. ... && x == 1 && y == 2 => map: x -> 1, y -> 2)
     * 
     * @param exp
     * @param map
     */
    private static void resolveRecursive(Expression exp, Map<String, Expression> map) {
        // Internal-var conjuncts assert truth: `#x` ⇒ `#x → true`, `!#x` ⇒ `#x → false`. Restricted to
        // internal vars so user-facing predicates like `x && x` still display as `x` instead of `true`.
        if (exp instanceof Var var && isInternal(var)) {
            map.putIfAbsent(var.getName(), new LiteralBoolean(true));
            return;
        }
        if (exp instanceof UnaryExpression unary && "!".equals(unary.getOp())
                && unary.getExpression()instanceof Var inner && isInternal(inner)) {
            map.putIfAbsent(inner.getName(), new LiteralBoolean(false));
            return;
        }

        if (!(exp instanceof BinaryExpression be))
            return;

        String op = be.getOperator();
        if ("&&".equals(op)) {
            resolveRecursive(be.getFirstOperand(), map);
            resolveRecursive(be.getSecondOperand(), map);
            return;
        }
        if (!"==".equals(op))
            return;

        Expression left = be.getFirstOperand();
        Expression right = be.getSecondOperand();
        String leftKey = substitutionKey(left);
        String rightKey = substitutionKey(right);

        if (leftKey != null && isConstant(right)) {
            map.put(leftKey, right.clone());
        } else if (rightKey != null && isConstant(left)) {
            map.put(rightKey, left.clone());
        } else if (left instanceof Var leftVar && right instanceof Var rightVar) {
            // to substitute internal variable with user-facing variable
            if (isInternal(leftVar) && !isInternal(rightVar) && !isReturnVar(leftVar)) {
                map.put(leftVar.getName(), right.clone());
            } else if (isInternal(rightVar) && !isInternal(leftVar) && !isReturnVar(rightVar)) {
                map.put(rightVar.getName(), left.clone());
            } else if (isInternal(leftVar) && isInternal(rightVar)) {
                // to substitute the lower-counter variable with the higher-counter one
                boolean isLeftCounterLower = getCounter(leftVar) <= getCounter(rightVar);
                Var lowerVar = isLeftCounterLower ? leftVar : rightVar;
                Var higherVar = isLeftCounterLower ? rightVar : leftVar;
                if (!isReturnVar(lowerVar) && !isFreshVar(higherVar))
                    map.putIfAbsent(lowerVar.getName(), higherVar.clone());
            }
        } else if (left instanceof Var var && canSubstitute(var, right)) {
            map.put(var.getName(), right.clone());
        } else if (left instanceof FunctionInvocation && !containsExpression(right, left)) {
            map.put(leftKey, right.clone());
        }
    }

    private static String substitutionKey(Expression exp) {
        if (exp instanceof Var var)
            return var.getName();
        if (exp instanceof FunctionInvocation)
            return exp.toString();
        return null;
    }

    /**
     * Handles transitive variable equalities in the map (e.g. map: x -> y, y -> 1 => map: x -> 1, y -> 1)
     * 
     * @param map
     * 
     * @return new map with resolved values
     */
    private static Map<String, Expression> resolveTransitive(Map<String, Expression> map) {
        Map<String, Expression> result = new HashMap<>();
        for (Map.Entry<String, Expression> entry : map.entrySet()) {
            result.put(entry.getKey(), lookup(entry.getValue(), map, new HashSet<>()));
        }
        return result;
    }

    /**
     * Returns the value of a variable by looking up in the map recursively Uses the seen set to avoid circular
     * references (e.g. x -> y, y -> x) which would cause infinite recursion
     * 
     * @param exp
     * @param map
     * @param seen
     * 
     * @return resolved expression
     */
    private static Expression lookup(Expression exp, Map<String, Expression> map, Set<String> seen) {
        String name = substitutionKey(exp);
        if (name == null)
            return exp;

        if (seen.contains(name))
            return exp; // circular reference

        Expression value = map.get(name);
        if (value == null)
            return exp;

        seen.add(name);
        return lookup(value, map, seen);
    }

    /**
     * Checks if a variable is used in the expression (excluding its own definitions)
     *
     * @param exp
     * @param name
     * @param value
     * @param topLevel
     *
     * @return true if used, false otherwise
     */
    private static boolean hasUsage(Expression exp, String name, Expression value, boolean topLevel) {
        if (topLevel && exp instanceof BinaryExpression binary && "&&".equals(binary.getOperator())) {
            return hasUsage(binary.getFirstOperand(), name, value, true)
                    || hasUsage(binary.getSecondOperand(), name, value, true);
        }

        if (topLevel && exp instanceof BinaryExpression binary && "==".equals(binary.getOperator())) {
            Expression left = binary.getFirstOperand();
            Expression right = binary.getSecondOperand();
            boolean leftDefinition = name.equals(substitutionKey(left)) && right.equals(value)
                    && (isConstant(right)
                            || (left instanceof Var v && !(right instanceof Var) && canSubstitute(v, right))
                            || (left instanceof FunctionInvocation && !(right instanceof Var)
                                    && !containsExpression(right, left)));
            boolean rightDefinition = name.equals(substitutionKey(right)) && left.equals(value) && isConstant(left);
            if (leftDefinition || rightDefinition)
                return false;
        }

        if (name.equals(substitutionKey(exp)))
            return true;
        for (Expression child : exp.getChildren())
            if (hasUsage(child, name, value, false))
                return true;
        return false;
    }

    private static int getCounter(Var var) {
        if (!isInternal(var))
            throw new IllegalStateException("Cannot get counter of non-internal variable");
        int lastUnderscore = var.getName().lastIndexOf('_');
        return Integer.parseInt(var.getName().substring(lastUnderscore + 1));
    }

    private static boolean isInternal(Var var) {
        return var.getName().startsWith("#");
    }

    private static boolean isReturnVar(Var var) {
        return var.getName().startsWith("#ret_");
    }

    private static boolean isFreshVar(Var var) {
        return var.getName().startsWith("#fresh_");
    }

    private static boolean canSubstitute(Var var, Expression value) {
        return !isReturnVar(var) && !isFreshVar(var) && !containsVariable(value, var.getName());
    }

    private static boolean isConstant(Expression exp) {
        return exp.isLiteral() || exp instanceof Enum;
    }

    private static boolean containsVariable(Expression exp, String name) {
        if (exp instanceof Var var)
            return var.getName().equals(name);

        if (!exp.hasChildren())
            return false;

        for (Expression child : exp.getChildren()) {
            if (containsVariable(child, name))
                return true;
        }
        return false;
    }

    private static boolean containsExpression(Expression exp, Expression target) {
        if (exp.equals(target))
            return true;

        if (!exp.hasChildren())
            return false;

        for (Expression child : exp.getChildren()) {
            if (containsExpression(child, target))
                return true;
        }
        return false;
    }
}
