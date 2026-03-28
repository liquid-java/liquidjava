package liquidjava.rj_language.opt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;
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
        map.entrySet().removeIf(entry -> !hasUsage(exp, entry.getKey()));

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
        if (!(exp instanceof BinaryExpression be))
            return;

        String op = be.getOperator();
        if ("&&".equals(op)) {
            resolveRecursive(be.getFirstOperand(), map);
            resolveRecursive(be.getSecondOperand(), map);
        } else if ("==".equals(op)) {
            Expression left = be.getFirstOperand();
            Expression right = be.getSecondOperand();
            if (left instanceof Var var && right.isLiteral()) {
                map.put(var.getName(), right.clone());
            } else if (right instanceof Var var && left.isLiteral()) {
                map.put(var.getName(), left.clone());
            } else if (left instanceof Var leftVar && right instanceof Var rightVar) {
                // to substitute internal variable with user-facing variable
                if (leftVar.isInternal() && !rightVar.isInternal()) {
                    map.put(leftVar.getName(), right.clone());
                } else if (rightVar.isInternal() && !leftVar.isInternal()) {
                    map.put(rightVar.getName(), left.clone());
                } else if (leftVar.isInternal() && rightVar.isInternal()) {
                    // to substitute the lower-counter variable with the higher-counter one
                    boolean isLeftCounterLower = leftVar.getCounter() <= rightVar.getCounter();
                    Var lowerVar = isLeftCounterLower ? leftVar : rightVar;
                    Var higherVar = isLeftCounterLower ? rightVar : leftVar;
                    map.putIfAbsent(lowerVar.getName(), higherVar.clone());
                }
            }
        }
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
        if (!(exp instanceof Var))
            return exp;

        String name = exp.toString();
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
     *
     * @return true if used, false otherwise
     */
    private static boolean hasUsage(Expression exp, String name) {
        // exclude own definitions
        if (exp instanceof BinaryExpression binary && "==".equals(binary.getOperator())) {
            Expression left = binary.getFirstOperand();
            Expression right = binary.getSecondOperand();
            if (left instanceof Var v && v.getName().equals(name) && right.isLiteral())
                return false;
            if (right instanceof Var v && v.getName().equals(name) && left.isLiteral())
                return false;
        }

        // usage found
        if (exp instanceof Var var && var.getName().equals(name)) {
            return true;
        }

        // recurse children
        if (exp.hasChildren()) {
            for (Expression child : exp.getChildren())
                if (hasUsage(child, name))
                    return true;
        }

        // usage not found
        return false;
    }
}