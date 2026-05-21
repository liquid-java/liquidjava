package testSuite.classes.imagewrite_correct;

import java.util.Locale;

import javax.imageio.ImageWriteParam;

/**
 * A JPEG export pipeline configured correctly against {@link ImageWriteParamsRefinements}.
 *
 * <p>
 * Both ghost-state dimensions are driven through their full transition path: each dimension's mode is set to
 * {@code MODE_EXPLICIT} before the dimension-specific setters run, and {@code getTileWidth} is reached only after
 * {@code setTiling} has moved the param into {@code tilingSet}. No state refinement is violated.
 */
class JpegExporter {

    ImageWriteParam buildJpegParam() {
        ImageWriteParam param = new ImageWriteParam(Locale.getDefault());
        param.setTilingMode(ImageWriteParam.MODE_EXPLICIT);
        param.setTiling(10, 30, 10, 30);
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.85f);
        return param;
    }

    int firstTileWidth() {
        ImageWriteParam param = new ImageWriteParam(Locale.getDefault());
        param.setTilingMode(ImageWriteParam.MODE_EXPLICIT);
        param.setTiling(8, 8, 0, 0);
        return param.getTileWidth();
    }
}
