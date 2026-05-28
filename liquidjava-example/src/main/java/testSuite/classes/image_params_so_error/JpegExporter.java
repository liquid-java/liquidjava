package testSuite.classes.image_params_so_error;
import java.util.Locale;

import javax.imageio.ImageWriteParam;

class JpegExporter {

    // Adapted from https://stackoverflow.com/questions/72024965/how-to-compress-jpg-and-png-images-in-java
    ImageWriteParam setCompressionPreferences() {
        ImageWriteParam param = new ImageWriteParam(Locale.getDefault());
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_DEFAULT);
            param.setCompressionQuality(0.85f); // State Refinement Error
        }
        return param;
    }
}