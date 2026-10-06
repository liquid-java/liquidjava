package testSuite.classes.inherited_constant_correct;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;

public class SimpleTest {

    public static void main(String[] args) {
        ImageWriteParam param = ImageIO.getImageWritersByFormatName("jpeg").next().getDefaultWriteParam();
        param.setCompressionMode(JPEGImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.5f);
    }
}
