package testSuite.classes.state_hint_compression_mode_error;

import java.util.Locale;

import javax.imageio.ImageWriteParam;

// Study case A1_01 (dcm4che Transcoder): the compression type is set without setting the compression mode first
public class Recoder {
    static ImageWriteParam params() {
        ImageWriteParam wParam = new ImageWriteParam(Locale.ROOT);
        wParam.setCompressionType("JPEG"); // Expect: State Refinement Error
        return wParam;
    }

    // the hint followed
    static ImageWriteParam explicitParams() {
        ImageWriteParam wParam = new ImageWriteParam(Locale.ROOT);
        wParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        wParam.setCompressionType("JPEG");
        return wParam;
    }
}
