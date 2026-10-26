package testSuite.classes.alias_static_final_state_correct;

import java.util.Locale;

import javax.imageio.ImageWriteParam;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.Refinement;
import liquidjava.specification.RefinementAlias;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"startTiling", "tilingExplicit", "tilingSet"})
@RefinementAlias("Mode(int x) { x == ImageWriteParam.MODE_EXPLICIT || x == ImageWriteParam.MODE_DEFAULT "
        + "|| x == ImageWriteParam.MODE_COPY_FROM_METADATA || x == ImageWriteParam.MODE_DISABLED }")
@ExternalRefinementsFor("javax.imageio.ImageWriteParam")
public interface ImageWriteParamRefinements {

    @StateRefinement(to = "startTiling(this)")
    void ImageWriteParam(Locale locale);

    @StateRefinement(to = "mode == ImageWriteParam.MODE_EXPLICIT ? tilingExplicit(this) : startTiling(this)")
    void setTilingMode(@Refinement("Mode(_)") int mode);

    @StateRefinement(from = "tilingExplicit(this)", to = "tilingSet(this)")
    @StateRefinement(from = "tilingSet(this)", to = "tilingSet(this)")
    void setTiling(@Refinement("_ > 0") int tileWidth, @Refinement("_ > 0") int tileHeight, int tileGridXOffset,
            int tileGridYOffset);
}
