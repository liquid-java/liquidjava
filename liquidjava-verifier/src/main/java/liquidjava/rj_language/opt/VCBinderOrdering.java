package liquidjava.rj_language.opt;

import static liquidjava.rj_language.opt.VCSimplificationUtils.containsVar;
import static liquidjava.rj_language.opt.VCSimplificationUtils.copyWithRefinement;
import static liquidjava.rj_language.opt.VCSimplificationUtils.isTrue;

import java.util.ArrayList;
import java.util.List;

import liquidjava.processor.VCImplication;

/**
 * Moves a binder refined only by true that comes after an implication using its variable to just before that
 * implication, so that each variable is introduced before it is used
 */
public class VCBinderOrdering implements VCSimplificationPass {

    /**
     * Moves the first such binder of a VC chain
     */
    @Override
    public VCImplication apply(VCImplication implication) {
        List<VCImplication> nodes = new ArrayList<>();
        for (VCImplication current = implication; current != null; current = current.getNext())
            nodes.add(current);

        for (int i = 0; i < nodes.size(); i++) {
            VCImplication binder = nodes.get(i);
            if (!binder.hasBinder() || !isTrue(binder.getRefinement().getExpression()))
                continue;
            for (int j = 0; j < i; j++) {
                if (containsVar(nodes.get(j).getRefinement().getExpression(), binder.getName())) {
                    nodes.add(j, nodes.remove(i));
                    return relink(nodes);
                }
            }
        }
        return implication.clone();
    }

    private static VCImplication relink(List<VCImplication> nodes) {
        VCImplication first = null;
        VCImplication last = null;
        for (VCImplication node : nodes) {
            VCImplication copy = copyWithRefinement(node, node.getRefinement().clone());
            if (first == null)
                first = copy;
            else
                last.setNext(copy);
            last = copy;
        }
        return first;
    }
}
