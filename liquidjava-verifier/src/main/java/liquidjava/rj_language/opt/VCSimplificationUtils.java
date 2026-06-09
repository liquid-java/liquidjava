package liquidjava.rj_language.opt;

import java.util.ArrayList;
import java.util.List;

import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.SimplifiedPredicate;
import liquidjava.rj_language.SimplifiedPredicate.Binder;
import liquidjava.rj_language.ast.Expression;

class VCSimplificationUtils {

    public static Expression activeExpression(Predicate refinement) {
        if (refinement instanceof SimplifiedPredicate simplified)
            return simplified.getSimplifiedPredicate().getExpression().clone();
        return refinement.getExpression().clone();
    }

    public static Predicate originPredicate(Predicate refinement) {
        if (refinement instanceof SimplifiedPredicate simplified)
            return simplified.getOrigin().clone();
        return refinement.clone();
    }

    public static VCImplication copyWithRefinement(VCImplication implication, Predicate refinement) {
        if (implication.hasBinder())
            return new VCImplication(implication.getName(), implication.getType(), refinement);
        return new VCImplication(refinement);
    }

    public static boolean sameVc(VCImplication left, VCImplication right) {
        if (left == null || right == null)
            return left == right;
        return left.toString().equals(right.toString());
    }
}
