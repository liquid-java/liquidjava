package liquidjava.rj_language.opt;

import liquidjava.processor.VCImplication;

/**
 * Simplifies VCImplication chains by applying various simplification steps
 */
public class VCSimplification {

    /**
     * Applies all available simplification steps to a VC chain
     */
    public static VCImplication simplifyToFixedPoint(VCImplication implication) {
        if (implication == null)
            return null;

        // keep applying simplification steps until a fixed point is reached
        VCImplication current = implication.clone();
        while (true) {
            VCImplication simplified = simplifyOnce(current);
            if (current.equals(simplified)) // fixed point reached
                return simplified;
            current = simplified;
        }
    }

    /**
     * Applies one simplification step to a VC chain
     */
    public static VCImplication simplifyOnce(VCImplication implication) {
        if (implication == null)
            return null;

        // first try to apply substitution, then folding
        VCImplication substituted = VCSubstitution.apply(implication);
        if (!implication.equals(substituted))
            return substituted;

        // TODO: add more simplification steps here (e.g., folding)
        return substituted;
    }
}
