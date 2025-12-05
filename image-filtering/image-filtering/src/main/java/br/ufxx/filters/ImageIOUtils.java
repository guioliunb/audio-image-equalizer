package br.ufxx.filters;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.color.ColorSpace;
import java.awt.image.ColorConvertOp;
import java.io.File;
import java.io.IOException;

public class ImageIOUtils {

    public static BufferedImage loadGray(String path) throws IOException {
        BufferedImage img = ImageIO.read(new File(path));
        if (img == null) {
            throw new IOException("Não foi possível carregar a imagem: " + path);
        }

        if (img.getType() != BufferedImage.TYPE_BYTE_GRAY) {
            BufferedImage gray = new BufferedImage(
                    img.getWidth(),
                    img.getHeight(),
                    BufferedImage.TYPE_BYTE_GRAY
            );
            ColorConvertOp op = new ColorConvertOp(
                    ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
            op.filter(img, gray);
            img = gray;
        }
        return img;
    }

    public static double[][] toArray(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        double[][] data = new double[h][w];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int v = rgb & 0xFF; // cinza
                data[y][x] = v;
            }
        }
        return data;
    }

    public static void saveGray(double[][] data, String path) throws IOException {
        int h = data.length;
        int w = data[0].length;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int v = (int)Math.round(data[y][x]);
                if (v < 0) v = 0;
                if (v > 255) v = 255;
                int rgb = (v << 16) | (v << 8) | v;
                img.setRGB(x, y, rgb);
            }
        }

        File outFile = new File(path);
        outFile.getParentFile().mkdirs();
        ImageIO.write(img, "png", outFile);
    }
}
