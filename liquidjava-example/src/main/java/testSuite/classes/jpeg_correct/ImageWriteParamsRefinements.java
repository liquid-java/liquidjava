package testSuite.classes.jpeg_correct;

import liquidjava.specification.*;
import java.util.Locale;
import javax.imageio.ImageWriteParam;

@SuppressWarnings("unused")
@ExternalRefinementsFor("javax.imageio.ImageWriteParam")
@StateSet({"start", "acceptCompression", "compressionExplicit", "compressionSet"})
@RefinementAlias("Ratio(float v) { 0 <= v && v <= 1.0 }")
public interface ImageWriteParamsRefinements {

    @StateRefinement(to="start()")
    void ImageWriteParam(Locale locale);

    void setProgressiveMode(
        @Refinement("mode == ImageWriteParam.MODE_DISABLED || mode == ImageWriteParam.MODE_DEFAULT || mode == ImageWriteParam.MODE_COPY_FROM_METADATA")
        int mode
    );

    @StateRefinement(
        from="compressionExplicit() || compressionSet()", 
        to="mode == ImageWriteParam.MODE_EXPLICIT ? compressionExplicit() : start()"
    )
    @StateRefinement(
        from="acceptCompression()", 
        to="mode == ImageWriteParam.MODE_EXPLICIT ? compressionExplicit() : acceptCompression()"
    )
    void setCompressionMode(int mode);

    @StateRefinement(from="compressionExplicit() || compressionSet()")
    void setCompressionQuality(@Refinement("Ratio(_)") float quality);

    @StateRefinement(from="start()", to="_ ? acceptCompression(this) : start()")
    @StateRefinement(from="!start()")
    boolean canWriteCompressed();

    // ...
}