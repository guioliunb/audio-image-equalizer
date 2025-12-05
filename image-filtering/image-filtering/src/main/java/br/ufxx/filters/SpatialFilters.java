package br.ufxx.filters;


public class SpatialFilters {

    public static double[][] convolve(double[][] img, double[][] kernel) {
        int h = img.length;
        int w = img[0].length;
        int kh = kernel.length;
        int kw = kernel[0].length;
        int cy = kh / 2;
        int cx = kw / 2;

        double[][] out = new double[h][w];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double sum = 0.0;

                for (int j = 0; j < kh; j++) {
                    for (int i = 0; i < kw; i++) {
                        int yy = y + j - cy;
                        int xx = x + i - cx;

                        // borda: replicação
                        if (yy < 0) yy = 0;
                        if (yy >= h) yy = h - 1;
                        if (xx < 0) xx = 0;
                        if (xx >= w) xx = w - 1;

                        sum += img[yy][xx] * kernel[j][i];
                    }
                }

                out[y][x] = sum;
            }
        }

        return out;
    }

    /** Kernel de média N×N (passa-baixa) */
    public static double[][] meanKernel(int size) {
        double[][] k = new double[size][size];
        double v = 1.0 / (size * size);
        for (int j = 0; j < size; j++) {
            for (int i = 0; i < size; i++) {
                k[j][i] = v;
            }
        }
        return k;
    }

    /** Passa-alta δ - h (mesmo tamanho de h) */
    public static double[][] highFromLow(double[][] low) {
        int kh = low.length;
        int kw = low[0].length;
        double[][] hp = new double[kh][kw];

        int cy = kh / 2;
        int cx = kw / 2;

        for (int j = 0; j < kh; j++) {
            for (int i = 0; i < kw; i++) {
                double delta = (j == cy && i == cx) ? 1.0 : 0.0;
                hp[j][i] = delta - low[j][i];
            }
        }
        return hp;
    }

    public static void addOffsetInPlace(double[][] img, double val) {
        int h = img.length;
        int w = img[0].length;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img[y][x] += val;
            }
        }
    }
}
