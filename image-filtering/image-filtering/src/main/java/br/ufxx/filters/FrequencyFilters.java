package br.ufxx.filters;

import org.jtransforms.fft.DoubleFFT_2D;

public class FrequencyFilters {

    // ========= FUNÇÕES PÚBLICAS =========

    /** Passa-baixa ideal 2D (radial), corte wc em radianos (0..π). */
    public static double[][] lowPass2D(double[][] img,
                                       double wc,
                                       String namePrefix) throws Exception {

        int h = img.length;
        int w = img[0].length;

        // FFT (com centralização via (-1)^{x+y})
        double[][] fft = forwardFFT(img);

        // máscara passa-baixa radial normalizada (centrada)
        double[][] mask = buildIdealLowPassRadialMask(h, w, wc);

        // salvar máscara para o relatório
        DFTVisualizer.saveMask(mask, "out/" + namePrefix + "_filter_mask.png");

        // aplicar máscara
        applyMaskInPlace(fft, mask);

        // salvar espectro filtrado
        DFTVisualizer.saveMagnitudeSpectrum(
                fft, "out/" + namePrefix + "_output_spectrum.png");

        // voltar ao domínio espacial (desfazendo a centralização)
        return inverseFFT(fft);
    }

    /** Passa-alta ideal 2D: HP = original - LP + 128. */
    public static double[][] highPass2D(double[][] img,
                                        double wc,
                                        String namePrefix) throws Exception {

        double[][] lp = lowPass2D(img, wc, namePrefix + "_LPbase");

        int h = img.length;
        int w = img[0].length;
        double[][] hp = new double[h][w];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                hp[y][x] = img[y][x] - lp[y][x] + 128.0;
            }
        }
        return hp;
    }

    /** Passa-baixa ideal apenas na direção horizontal (corte wcX). */
    public static double[][] lowPassHorizontal(double[][] img,
                                               double wcX,
                                               String namePrefix) throws Exception {

        int h = img.length;
        int w = img[0].length;

        // FFT com centralização
        double[][] fft = forwardFFT(img);

        double[][] mask = buildIdealLowPassHorizontalMask(h, w, wcX);

        DFTVisualizer.saveMask(mask, "out/" + namePrefix + "_filter_mask.png");

        applyMaskInPlace(fft, mask);

        DFTVisualizer.saveMagnitudeSpectrum(
                fft, "out/" + namePrefix + "_output_spectrum.png");

        return inverseFFT(fft);
    }

    /** Passa-alta apenas na direção horizontal: HP = original - LP + 128. */
    public static double[][] highPassHorizontal(double[][] img,
                                                double wcX,
                                                String namePrefix) throws Exception {

        double[][] lp = lowPassHorizontal(img, wcX, namePrefix + "_LPbase");

        int h = img.length;
        int w = img[0].length;
        double[][] hp = new double[h][w];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                hp[y][x] = img[y][x] - lp[y][x] + 128.0;
            }
        }
        return hp;
    }

    // ========= FUNÇÕES INTERNAS (FFT, MÁSCARAS, ETC.) =========

    /**
     * FFT 2D complexa usando JTransforms: retorna [h][2*w] (real, imag).
     * Aplica centralização multiplicando a imagem de entrada por (-1)^{x+y}.
     */
    private static double[][] forwardFFT(double[][] img) throws Exception {
        int h = img.length;
        int w = img[0].length;

        double[][] data = new double[h][2 * w];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                // fator de centralização: (-1)^(x+y)
                double factor = ((x + y) % 2 == 0) ? 1.0 : -1.0;
                data[y][2 * x]     = img[y][x] * factor; // real
                data[y][2 * x + 1] = 0.0;                // imag
            }
        }

        DoubleFFT_2D fft = new DoubleFFT_2D(h, w);
        fft.complexForward(data);

        // opcional: espectro da entrada (já centrado)
        DFTVisualizer.saveMagnitudeSpectrum(
                data, "out/input_spectrum.png");

        return data;
    }

    /**
     * Inversa da FFT 2D (complexInverse com normalização).
     * Desfaz a centralização multiplicando a saída por (-1)^{x+y}.
     */
    private static double[][] inverseFFT(double[][] fft) {
        int h = fft.length;
        int w2 = fft[0].length;
        int w = w2 / 2;

        DoubleFFT_2D fft2d = new DoubleFFT_2D(h, w);
        fft2d.complexInverse(fft, true); // true = normalizado

        double[][] out = new double[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double factor = ((x + y) % 2 == 0) ? 1.0 : -1.0;
                out[y][x] = fft[y][2 * x] * factor; // parte real re-centralizada
            }
        }
        return out;
    }

    /** Aplica máscara (0..1) aos coeficientes complexos. */
    private static void applyMaskInPlace(double[][] fft, double[][] mask) {
        int h = fft.length;
        int w = mask[0].length;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double m = mask[y][x];
                fft[y][2 * x]     *= m;
                fft[y][2 * x + 1] *= m;
            }
        }
    }

    /**
     * Máscara passa-baixa ideal RADIAL.
     * wc em radianos, de 0 a π; usamos wc/π como raio normalizado (0..1).
     * Frequência 0 está no centro (pois já centralizamos a FFT).
     */
    private static double[][] buildIdealLowPassRadialMask(int h, int w, double wc) {
        double[][] mask = new double[h][w];

        double cx = w / 2.0;
        double cy = h / 2.0;

        double cutoffNorm = wc / Math.PI; // π -> 1.0, π/2 -> 0.5, etc.
        if (cutoffNorm > 1.0) cutoffNorm = 1.0;

        for (int y = 0; y < h; y++) {
            double vy = (y - cy) / cy; // -1..1
            for (int x = 0; x < w; x++) {
                double vx = (x - cx) / cx; // -1..1
                double r = Math.sqrt(vx * vx + vy * vy); // raio normalizado

                mask[y][x] = (r <= cutoffNorm) ? 1.0 : 0.0;
            }
        }
        return mask;
    }

    /**
     * Máscara passa-baixa ideal SOMENTE na direção horizontal (u).
     * wcX em radianos -> cutoffNormX em [0,1].
     * Frequência 0 está no centro do eixo horizontal.
     */
    private static double[][] buildIdealLowPassHorizontalMask(int h, int w, double wcX) {
        double[][] mask = new double[h][w];

        double cx = w / 2.0;
        double cutoffNormX = wcX / Math.PI;
        if (cutoffNormX > 1.0) cutoffNormX = 1.0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double vx = Math.abs((x - cx) / cx); // |u| normalizado (0..1)
                boolean pass = (vx <= cutoffNormX);
                mask[y][x] = pass ? 1.0 : 0.0;
            }
        }
        return mask;
    }
}
