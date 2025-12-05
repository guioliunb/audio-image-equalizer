package br.ufxx.filters;


import java.awt.image.BufferedImage;

public class Main {

    public static void main(String[] args) throws Exception {

        // Caminho da imagem de entrada (8 bits, >= 512x512)
        String inputPath = "input/enqt.png";
        if (args.length > 0) {
            inputPath = args[0];
        }

        System.out.println("Carregando imagem: " + inputPath);
        BufferedImage img = ImageIOUtils.loadGray(inputPath);
        double[][] f = ImageIOUtils.toArray(img);

        // ========= DOMÍNIO ESPACIAL =========

        // 1) passa-baixa 3x3 (filtro de médias)
        double[][] h3 = SpatialFilters.meanKernel(3);
        double[][] y1 = SpatialFilters.convolve(f, h3);
        ImageIOUtils.saveGray(y1, "out/01_lp_spatial_3x3.png");

        // 2) passa-baixa 7x7
        double[][] h7 = SpatialFilters.meanKernel(7);
        double[][] y2 = SpatialFilters.convolve(f, h7);
        ImageIOUtils.saveGray(y2, "out/02_lp_spatial_7x7.png");

        // 3) passa-alta correspondente ao 3x3
        double[][] h3High = SpatialFilters.highFromLow(h3);
        double[][] y3 = SpatialFilters.convolve(f, h3High);
        SpatialFilters.addOffsetInPlace(y3, 128.0);
        ImageIOUtils.saveGray(y3, "out/03_hp_spatial_3x3.png");

        // 4) passa-alta correspondente ao 7x7
        double[][] h7High = SpatialFilters.highFromLow(h7);
        double[][] y4 = SpatialFilters.convolve(f, h7High);
        SpatialFilters.addOffsetInPlace(y4, 128.0);
        ImageIOUtils.saveGray(y4, "out/04_hp_spatial_7x7.png");

        // ========= DOMÍNIO DA DFT =========

        // 5) passa-baixa em frequência, wc = pi/2 (horizontal e vertical)
        double[][] y5 = FrequencyFilters.lowPass2D(
                f, Math.PI / 2,
                "05_lp_freq_pi2");
        ImageIOUtils.saveGray(y5, "out/05_lp_freq_pi2_image.png");

        // 6) passa-baixa em frequência, wc = pi/4
        double[][] y6 = FrequencyFilters.lowPass2D(
                f, Math.PI / 4,
                "06_lp_freq_pi4");
        ImageIOUtils.saveGray(y6, "out/06_lp_freq_pi4_image.png");

        // 7) passa-alta em frequência, wc = pi/2
        double[][] y7 = FrequencyFilters.highPass2D(
                f, Math.PI / 2,
                "07_hp_freq_pi2");
        ImageIOUtils.saveGray(y7, "out/07_hp_freq_pi2_image.png");

        // 8) passa-alta em frequência, wc = pi/4
        double[][] y8 = FrequencyFilters.highPass2D(
                f, Math.PI / 4,
                "08_hp_freq_pi4");
        ImageIOUtils.saveGray(y8, "out/08_hp_freq_pi4_image.png");

        // 9) passa-baixa com corte wc = pi/8 apenas na direção horizontal
        double[][] y9 = FrequencyFilters.lowPassHorizontal(
                f, Math.PI / 8,
                "09_lp_freq_horizontal_pi8");
        ImageIOUtils.saveGray(y9, "out/09_lp_freq_horizontal_pi8_image.png");

        // 10) passa-alta com corte wc = pi/8 apenas na direção horizontal
        double[][] y10 = FrequencyFilters.highPassHorizontal(
                f, Math.PI / 8,
                "10_hp_freq_horizontal_pi8");
        ImageIOUtils.saveGray(y10, "out/10_hp_freq_horizontal_pi8_image.png");  

        System.out.println("Processamento concluído. Verifique a pasta 'out/'.");
    }
}
