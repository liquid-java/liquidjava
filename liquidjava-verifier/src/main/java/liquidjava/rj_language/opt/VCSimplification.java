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

        for (VCSimplificationPass pass : PASSES) {
            VCImplication simplified = pass.apply(implication);
            if (!implication.equals(simplified))
                return simplified;
        }
        return implication;
    }
}
