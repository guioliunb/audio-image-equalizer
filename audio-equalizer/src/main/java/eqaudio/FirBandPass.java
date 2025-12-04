package eqaudio;

public class FirBandPass {

    private final int N;         // número de coeficientes = 2K+1
    private final double[] h;    // coeficientes FIR
    private final float[] delay; // linha de atraso para convolução
    private int index;           // índice circular

    /**
     * @param fs      frequência de amostragem (ex.: 44100.0)
     * @param fLow    frequência de corte inferior (Hz)
     * @param fHigh   frequência de corte superior (Hz)
     * @param K       metade da largura da janela (N = 2K+1)
     */
    public FirBandPass(double fs, double fLow, double fHigh, int K) {
        this.N = 2 * K + 1;          // comprimento 2K+1, como no enunciado
        this.h = new double[N];
        this.delay = new float[N];
        this.index = 0;

        design(fs, fLow, fHigh, K);
    }

    /**
     * Projeta o filtro FIR passa-faixa via janelamento de Hamming.
     */
    private void design(double fs, double fLow, double fHigh, int K) {
        int N = this.N;
        int M = N - 1; // usado na fórmula da janela

        double wc1 = 2.0 * Math.PI * fLow  / fs;
        double wc2 = 2.0 * Math.PI * fHigh / fs;

        for (int n = 0; n < N; n++) {

            int k = n - K; // índice centrado em 0: k ∈ [-K, ..., K]

            double h_lp1, h_lp2;

            if (k == 0) {
                // valor em k = 0 para passa-baixa ideal
                h_lp1 = wc1 / Math.PI;
                h_lp2 = wc2 / Math.PI;
            } else {
                h_lp1 = Math.sin(wc1 * k) / (Math.PI * k);
                h_lp2 = Math.sin(wc2 * k) / (Math.PI * k);
            }

            double h_bp = h_lp2 - h_lp1;  // passa-faixa ideal

            // janela de Hamming: comprimento N = 2K+1
            double w = 0.54 - 0.46 * Math.cos(2.0 * Math.PI * n / M);

            h[n] = h_bp * w;
        }

        // Normalização simples para ganho ~1 na banda
        double sum = 0.0;
        for (int n = 0; n < N; n++) {
            sum += h[n];
        }
        if (Math.abs(sum) > 1e-9) {
            for (int n = 0; n < N; n++) {
                h[n] /= sum;
            }
        }
    }

    /**
     * Processa um bloco de samples (em float) in-place,
     * aplicando convolução FIR com buffer circular.
     */
    public void processBlock(float[] samples, int length) {
        for (int i = 0; i < length; i++) {
            // insere amostra atual no buffer circular
            delay[index] = samples[i];

            // convolução: y[i] = Σ h[k] * delay[index-k]
            double acc = 0.0;
            int idx = index;
            for (int k = 0; k < N; k++) {
                acc += h[k] * delay[idx];
                idx--;
                if (idx < 0) {
                    idx = N - 1;
                }
            }

            samples[i] = (float) acc;

            // avança índice circular
            index++;
            if (index >= N) {
                index = 0;
            }
        }
    }
}
