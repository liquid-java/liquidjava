package testSuite.classes.imagewrite_error;

import java.util.Locale;

import javax.imageio.ImageWriteParam;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

/**
 * External typestate specification for {@code javax.imageio.ImageWriteParam}.
 *
 * <p>
 * The class is modelled as two independent ghost-state dimensions — tiling and compression — so a configuration error
 * in one dimension does not mask the other. The conditional {@code setTilingMode} / {@code setCompressionMode}
 * transitions only reach the {@code *Explicit} state when called with {@code MODE_EXPLICIT}; any other mode leaves the
 * param in its {@code start*} state, which the dimension-specific setters reject.
 */
@StateSet({ "startTiling", "tilingExplicit", "tilingSet" })
@StateSet({ "startCompression", "compressionExplicit", "compressionSet" })
@ExternalRefinementsFor("javax.imageio.ImageWriteParam")
public interface ImageWriteParamsRefinements {

    // Constructor
    @StateRefinement(to = "startTiling(this) && startCompression(this)")
    void ImageWriteParam(Locale locale);

    // Tiling related methods

    @StateRefinement(to = "(mode == ImageWriteParam.MODE_EXPLICIT)? tilingExplicit(this) : startTiling(this)")
    void setTilingMode(int mode);

    @StateRefinement(from = "tilingExplicit(this)", to = "tilingSet(this)")
    @StateRefinement(from = "tilingSet(this)", to = "tilingSet(this)")
    void setTiling(@Refinement("_ > 0") int tileWidth, @Refinement("_ > 0") int tileHeight, int tileGridXOffset,
            int tileGridYOffset);

    @StateRefinement(from = "tilingSet(this)")
    int getTileGridXOffset();

    @StateRefinement(from = "tilingSet(this)")
    int getTileGridYOffset();

    @StateRefinement(from = "tilingSet(this)")
    int getTileHeight();

    @StateRefinement(from = "tilingSet(this)")
    int getTileWidth();

    @StateRefinement(from = "tilingExplicit(this)")
    @StateRefinement(from = "tilingSet(this)", to = "tilingExplicit(this)")
    void unsetTiling();

    void setProgressiveMode(@Refinement("ImageWriteParam.MODE_DISABLED == mode || mode == ImageWriteParam.MODE_DEFAULT || mode == ImageWriteParam.MODE_COPY_FROM_METADATA") int mode);

    // Compression related methods

    @StateRefinement(to = "mode == ImageWriteParam.MODE_EXPLICIT? compressionExplicit(this) : startCompression(this)")
    void setCompressionMode(int mode);

    @StateRefinement(from = "compressionExplicit(this)")
    @StateRefinement(from = "compressionSet(this)")
    void setCompressionQuality(@Refinement("_ >= 0.0 && _ <= 1.0") float quality);

    @StateRefinement(from = "compressionExplicit(this)")
    @StateRefinement(from = "compressionSet(this)")
    String getCompressionType();

    @StateRefinement(from = "compressionExplicit(this)", to = "compressionSet(this)")
    void setCompressionType(String compressionType);

    @StateRefinement(from = "compressionExplicit(this)")
    @StateRefinement(from = "compressionSet(this)", to = "compressionExplicit(this)")
    void unsetCompression();

    @StateRefinement(from = "compressionSet(this)")
    String getLocalizedCompressionTypeName();

    @StateRefinement(from = "compressionExplicit(this)")
    @StateRefinement(from = "compressionSet(this)")
    boolean isCompressionLossless();

    @StateRefinement(from = "compressionExplicit(this)")
    @StateRefinement(from = "compressionSet(this)")
    float getCompressionQuality();
}
