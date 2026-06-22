package liquidjava.rj_language.opt;

import liquidjava.processor.VCImplication;

/**
 * Applies one simplification step to a VC implication chain.
 */
public interface VCSimplificationPass {
    VCImplication apply(VCImplication implication);

    default String getName() {
        String className = getClass().getSimpleName();
        String name = className.startsWith("VC") ? className.substring(2) : className;
        return name.replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ");
    }
}
