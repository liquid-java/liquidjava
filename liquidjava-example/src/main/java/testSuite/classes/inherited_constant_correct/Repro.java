package testSuite.classes.inherited_constant_correct;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;

public class Repro {
    public static void main(String[] args) {
        ImageWriteParam p = ImageIO.getImageWritersByFormatName("jpeg").next().getDefaultWriteParam();
        p.setCompressionMode(JPEGImageWriteParam.MODE_EXPLICIT);
        p.setCompressionQuality(0.5f);
    }
}
