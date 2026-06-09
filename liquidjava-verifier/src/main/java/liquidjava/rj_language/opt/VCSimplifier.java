package liquidjava.rj_language.opt;

import liquidjava.processor.VCImplication;

/**
 * Simplifies VCImplication chains by applying various simplification steps
 */
public class VCSimplifier {

    /**
     * Applies all available simplification steps to a VC chain
     */
    public static VCImplication simplify(VCImplication implication) {
        // TODO: implement remaining simplification steps
        return VCSubstitution.apply(implication);
    }

    /**
     * Applies one simplification step to a VC chain
     */
    public static VCImplication simplifyOnce(VCImplication implication) {
        // TODO: implement remaining simplification steps
        return VCSubstitution.applyOnce(implication);
    }
}
