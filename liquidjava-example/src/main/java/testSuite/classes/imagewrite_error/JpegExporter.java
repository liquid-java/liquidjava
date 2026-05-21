package testSuite.classes.imagewrite_error;

import java.util.Locale;

import javax.imageio.ImageWriteParam;

/**
 * A JPEG export pipeline configured against {@link ImageWriteParamsRefinements}.
 *
 * <p>
 * The author did configure a tiling mode — but passed {@code MODE_DEFAULT} instead of {@code MODE_EXPLICIT}. The spec's
 * conditional transition leaves the param in {@code startTiling} for any non-explicit mode, so {@code setTiling}
 * (which requires {@code tilingExplicit} or {@code tilingSet}) violates its from-state.
 *
 * <p>
 * The found-state threads the same {@code param} across SSA versions joined by internal {@code stateN(x) == stateN(y)}
 * equalities; state derivation rewrites those into developer-facing typestate names for the diagnostic.
 */
class JpegExporter {

    ImageWriteParam buildJpegParam() {
        ImageWriteParam param = new ImageWriteParam(Locale.getDefault());
        param.setTilingMode(ImageWriteParam.MODE_DEFAULT);
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.85f);
        param.setTiling(10, 30, 10, 30); // State Refinement Error
        return param;
    }
}
