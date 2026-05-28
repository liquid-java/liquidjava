package testSuite.classes.image_params_so_error;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.util.Iterator;

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

    public String compressImage(File multipartFile, RenderedImage image) throws IOException {
        String filePath = System.getProperty("java.io.tmpdir");
        File compressedImageFile = new File(filePath);
        OutputStream os = new FileOutputStream(compressedImageFile.getName());
        String extension = multipartFile.getName().substring(multipartFile.getName().lastIndexOf('.') + 1);
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(extension);
        ImageWriter writer = writers.next();

        ImageOutputStream ios = ImageIO.createImageOutputStream(os);
        writer.setOutput(ios);

        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT); // State Refinement Error
        param.setCompressionQuality(0.5f);
        
        writer.write(null, new IIOImage(image, null, null), param);
        os.close();
        ios.close();
        writer.dispose();
        return String.valueOf(compressedImageFile);
    }
}