package testSuite.classes.alias_static_final_state_correct;

import java.util.Locale;

import javax.imageio.ImageWriteParam;

class TilingClient {

    ImageWriteParam tiled() {
        ImageWriteParam param = new ImageWriteParam(Locale.getDefault());
        param.setTilingMode(ImageWriteParam.MODE_EXPLICIT);
        param.setTiling(256, 256, 0, 0);
        return param;
    }
}
