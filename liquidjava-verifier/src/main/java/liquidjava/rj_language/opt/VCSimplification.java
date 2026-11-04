package liquidjava.rj_language.opt;

import java.util.List;

import liquidjava.diagnostics.DebugLog;
import liquidjava.processor.VCImplication;

/**
 * Simplifies VCImplication chains by applying various simplification steps
 */
public class VCSimplification {

    /**
     * Applies all available simplification steps to a VC chain until a fixed point is reached
     */
    public static VCSimplificationResult simplifyToFixedPoint(VCImplication implication) {
        return simplifyToFixedPoint(implication, NeededVariables.NONE);
    }

    /**
     * Applies all available simplification steps to a VC chain until a fixed point is reached, keeping the binders of
     * the needed variables
     */
    public static VCSimplificationResult simplifyToFixedPoint(VCImplication implication, NeededVariables needed) {
        if (implication == null)
            return null;

        List<VCSimplificationPass> passes = passes(needed);
        DebugLog.simplificationStart(implication);
        VCSimplificationResult current = new VCSimplificationResult(implication);
        while (true) {
            VCSimplificationResult simplified = simplifyOnce(current, passes);
            if (simplified == current) {
                DebugLog.simplificationEnd(current);
                return current; // fixed point reached
            }
            current = simplified;
        }
    }

    /**
     * Applies one simplification step to a VC chain from all available simplification passes
     */
    public static VCSimplificationResult simplifyOnce(VCSimplificationResult implication) {
        return simplifyOnce(implication, passes(NeededVariables.NONE));
    }

    private static VCSimplificationResult simplifyOnce(VCSimplificationResult implication,
            List<VCSimplificationPass> passes) {
        for (VCSimplificationPass pass : passes) {
            VCSimplificationResult simplified = simplifyOnce(implication, pass);
            if (simplified != implication)
                return simplified;
        }
        return implication;
    }

    private static List<VCSimplificationPass> passes(NeededVariables needed) {
        return List.of(new VCSubstitution(), new VCFunctionSubstitution(), new VCBinderSimplification(needed),
                new VCFolding(), new VCArithmeticSimplification(), new VCLogicalSimplification(),
                new VCConstraintSimplification());
    }

    /**
     * Applies one selected simplification pass to a VC chain
     */
    public static VCSimplificationResult simplifyOnce(VCSimplificationResult implication, VCSimplificationPass pass) {
        if (implication == null)
            return null;

        VCImplication simplified = pass.apply(implication.getImplication());
        if (implication.getImplication().equals(simplified))
            return implication;
        VCSimplificationResult result = new VCSimplificationResult(simplified, implication, pass.getName());
        DebugLog.simplificationPass(result);
        return result;
    }
}
