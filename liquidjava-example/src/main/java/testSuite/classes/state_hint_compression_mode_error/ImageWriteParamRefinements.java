package testSuite.classes.state_hint_compression_mode_error;

import java.util.Locale;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// The call that reaches the state setCompressionType needs depends on its argument
@ExternalRefinementsFor("javax.imageio.ImageWriteParam")
@StateSet({"startCompression", "compressionExplicit", "compressionSet"})
public interface ImageWriteParamRefinements {
    @StateRefinement(to = "startCompression(this)")
    void ImageWriteParam(Locale locale);

    @StateRefinement(to = "mode == 2 ? compressionExplicit(this) : startCompression(this)")
    void setCompressionMode(int mode);

    @StateRefinement(from = "compressionExplicit(this)", to = "compressionSet(this)")
    void setCompressionType(String compressionType);
}
