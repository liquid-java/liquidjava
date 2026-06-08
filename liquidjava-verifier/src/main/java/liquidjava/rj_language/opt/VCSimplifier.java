package liquidjava.rj_language.opt;

import liquidjava.processor.VCImplication;

public class VCSimplifier {

    public static VCImplication simplifyOnce(VCImplication implication) {
        return VCSubstitution.apply(implication);
    }
}
