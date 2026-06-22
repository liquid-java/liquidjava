package liquidjava.rj_language.opt;

import java.util.List;

import liquidjava.diagnostics.DebugLog;
import liquidjava.processor.VCImplication;

/**
 * Simplifies VCImplication chains by applying various simplification steps
 */
public class VCSimplification {

    private static final List<VCSimplificationPass> PASSES = List.of(new VCSubstitution(), new VCFunctionSubstitution(),
            new VCBinderSimplification(), new VCFolding(), new VCArithmeticSimplification(), new VCConstraintElimination(),
            new VCLogicalSimplification());

    /**
     * Applies all available simplification steps to a VC chain until a fixed point is reached
     */
    public static VCSimplificationResult simplifyToFixedPoint(VCImplication implication) {
        if (implication == null)
            return null;

        DebugLog.simplificationStart(implication);
        VCSimplificationResult current = new VCSimplificationResult(implication);
        while (true) {
            VCSimplificationResult simplified = simplifyOnce(current);
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
        for (VCSimplificationPass pass : PASSES) {
            VCSimplificationResult simplified = simplifyOnce(implication, pass);
            if (simplified != implication)
                return simplified;
        }
        return implication;
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
