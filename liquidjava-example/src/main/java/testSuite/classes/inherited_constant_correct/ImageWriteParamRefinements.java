package testSuite.classes.inherited_constant_correct;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"start", "explicit"})
@ExternalRefinementsFor("javax.imageio.ImageWriteParam")
public interface ImageWriteParamRefinements {
    @StateRefinement(to = "mode == 2 ? explicit(this) : start(this)")
    void setCompressionMode(@Refinement("_ >= 0 && _ <= 3") int mode);

    @StateRefinement(from = "explicit(this)")
    void setCompressionQuality(@Refinement("_ >= 0.0 && _ <= 1.0") float quality);
}
