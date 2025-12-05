package br.ufxx.filters;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class DFTVisualizer {

    /**
     * Gera imagem da magnitude da DFT em escala log (dados no formato JTransforms: [h][2*w]).
     */
    public static void saveMagnitudeSpectrum(double[][] fft, String path) throws Exception {
        int h = fft.length;
        int w = fft[0].length / 2;

        double[][] mag = new double[h][w];
        double max = Double.NEGATIVE_INFINITY;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double re = fft[y][2 * x];
                double im = fft[y][2 * x + 1];
                double m = Math.sqrt(re * re + im * im);
                mag[y][x] = m;
                if (m > max) max = m;
            }
        }

        double[][] logImg = new double[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double v = Math.log(1 + mag[y][x]) / Math.log(1 + max);
                logImg[y][x] = v * 255.0;
            }
        }

        double[][] shifted = fftShift(logImg);
        saveAsGray(shifted, path);
    }

    /**
     * Salva uma máscara (filtro ideal em frequência) como imagem (0–1 → 0–255),
     * já com fftshift para centralizar.
     */
    public static void saveMask(double[][] mask, String path) throws Exception {
        double[][] shifted = fftShift(mask);

        int h = shifted.length;
        int w = shifted[0].length;
        double max = 0.0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (shifted[y][x] > max) max = shifted[y][x];
            }
        }
        if (max == 0) max = 1;

        double[][] img = new double[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img[y][x] = shifted[y][x] / max * 255.0;
            }
        }

        saveAsGray(img, path);
    }

    /** fftshift: troca quadrantes para centralizar a baixa frequência. */
    private static double[][] fftShift(double[][] img) {
        int h = img.length;
        int w = img[0].length;
        double[][] out = new double[h][w];

        int h2 = h / 2;
        int w2 = w / 2;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int yy = (y + h2) % h;
                int xx = (x + w2) % w;
                out[yy][xx] = img[y][x];
            }
        }
        return out;
    }

    private static void saveAsGray(double[][] data, String path) throws Exception {
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

        File out = new File(path);
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
    }
}
