package liquidjava.rj_language.opt;

import liquidjava.processor.VCImplication;

/**
 * Applies one simplification step to a VC implication chain.
 */
public interface VCSimplificationPass {
    VCImplication apply(VCImplication implication);
}
