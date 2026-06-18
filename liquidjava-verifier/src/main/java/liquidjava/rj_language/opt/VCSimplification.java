package liquidjava.rj_language.opt;

import java.util.List;

import liquidjava.processor.VCImplication;

/**
 * Simplifies VCImplication chains by applying various simplification steps
 */
public class VCSimplification {

    private static final List<VCSimplificationPass> PASSES = List.of(new VCSubstitution(), new VCBinderSimplification(),
            new VCFolding(), new VCArithmeticSimplification(), new VCLogicalSimplification());

    /**
     * Applies all available simplification steps to a VC chain until a fixed point is reached
     */
    public static VCSimplificationResult simplifyToFixedPoint(VCImplication implication) {
        if (implication == null)
            return null;

        VCSimplificationResult current = new VCSimplificationResult(implication);
        while (true) {
            VCSimplificationResult simplified = simplifyOnce(current.getImplication(), current);
            if (simplified.getOrigin() == null)
                return current; // fixed point reached
            current = simplified;
        }
    }

    /**
     * Applies one simplification step to a VC chain
     */
    public static VCSimplificationResult simplifyOnce(VCImplication implication) {
        if (implication == null)
            return null;

        return simplifyOnce(implication, new VCSimplificationResult(implication));
    }

    /**
     * Applies one simplification step to a VC chain, keeping track of the origin of the simplification
     */
    private static VCSimplificationResult simplifyOnce(VCImplication implication, VCSimplificationResult origin) {
        for (VCSimplificationPass pass : PASSES) {
            VCImplication simplified = pass.apply(implication);
            if (!implication.equals(simplified))
                return new VCSimplificationResult(simplified, origin);
        }
        return new VCSimplificationResult(implication);
    }
}
