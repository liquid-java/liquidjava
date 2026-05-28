package testSuite.classes.image_params_so_error;



import java.util.Locale;
import liquidjava.specification.*;
import javax.imageio.ImageWriteParam;

@ExternalRefinementsFor("javax.imageio.ImageWriteParam")

@StateSet({"start", "acceptCompression", "compressionExplicit", "compressionSet"})
@RefinementAlias("Ratio(float v) { 0 <= v && v <= 1.0 }")
public interface ImageWriteParamsRefinements {

    @StateRefinement(to="start(this)")
    void ImageWriteParam(Locale locale);

    void setProgressiveMode(
        @Refinement("ImageWriteParam.MODE_DISABLED == mode || mode == ImageWriteParam.MODE_DEFAULT || mode == ImageWriteParam.MODE_COPY_FROM_METADATA")
        int mode
    );

    @StateRefinement(from= "compressionExplicit() || compressionSet()", 
                    to = "mode == ImageWriteParam.MODE_EXPLICIT ? compressionExplicit(this) : start(this)")
    @StateRefinement(from="acceptCompression()", 
                    to = "mode == ImageWriteParam.MODE_EXPLICIT ? compressionExplicit(this) : acceptCompression(this)")
    void setCompressionMode(int mode);

    @StateRefinement(from="compressionExplicit() || compressionSet()")
    void setCompressionQuality(@Refinement("Ratio(_)") float quality);

    @StateRefinement(from = "start(this)",
                    to   = "return ? acceptCompression(this) : start(this)")
    boolean canWriteCompressed();
}