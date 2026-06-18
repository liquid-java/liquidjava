package liquidjava.rj_language.opt;

import java.util.ArrayList;
import java.util.List;

import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.LiteralBoolean;

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

    public static boolean isTrue(Expression expression) {
        return expression instanceof LiteralBoolean literal && literal.isBooleanTrue();
    }

    public static boolean isFalse(Expression expression) {
        return expression instanceof LiteralBoolean literal && !literal.isBooleanTrue();
    }
}
