package liquidjava.rj_language.opt;

import liquidjava.processor.VCImplication;

/**
 * Simplifies VCImplication chains by applying various simplification steps
 */
public class VCSimplification {

    /**
     * Applies all available simplification steps to a VC chain until a fixed point is reached
     */
    public static VCImplication simplifyToFixedPoint(VCImplication implication) {
        if (implication == null)
            return null;

        // keep applying simplification steps until a fixed point is reached
        VCImplication current = implication.clone();
        while (true) {
            VCImplication simplified = simplifyOnce(current);
            if (current.equals(simplified))
                return simplified; // fixed point reached
            current = simplified;
        }
    }

    /**
     * Applies one simplification step to a VC chain
     */
    public static VCImplication simplifyOnce(VCImplication implication) {
        if (implication == null)
            return null;

        // substitution
        VCImplication substituted = VCSubstitution.apply(implication);
        if (!implication.equals(substituted))
            return substituted;

        // folding
        VCImplication folded = VCFolding.apply(implication);
        if (!implication.equals(folded))
            return folded;

        // arithmetic simplification
        return VCArithmeticSimplification.apply(implication);
    }
}
