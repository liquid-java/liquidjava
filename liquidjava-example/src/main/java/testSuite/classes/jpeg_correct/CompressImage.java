package testSuite.classes.jpeg_correct;

import java.io.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.RenderedImage;

public class CompressImage {

    //  Adapted from https://stackoverflow.com/questions/72024965/how-to-compress-jpg-and-png-images-in-java
    public String compressImage(File multipartFile, RenderedImage image) throws IOException {
        String filePath = System.getProperty("java.io.tmpdir");
        File compressedImageFile = new File(filePath);
        OutputStream os = new FileOutputStream(compressedImageFile.getName());
        String extension = multipartFile.getName().substring(multipartFile.getName().lastIndexOf('.') + 1);
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(extension);
        ImageWriter writer = writers.next();
        ImageOutputStream ios = ImageIO.createImageOutputStream(os);
        writer.setOutput(ios);

        ImageWriteParam param = writer.getDefaultWriteParam(); // should initialize state to start()
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.5f);
        }
        writer.write(null, new IIOImage(image, null, null), param);
        os.close();
        ios.close();
        writer.dispose();
        return String.valueOf(compressedImageFile);
    }
}
