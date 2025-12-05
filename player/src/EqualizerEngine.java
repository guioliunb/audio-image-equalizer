public class EqualizerEngine {

    private static final float FS = 44100f;

    // 5 bandas:
    // 1: 100 – 215 Hz
    // 2: 215 – 665 Hz
    // 3: 665 – 2150 Hz
    // 4: 2150 – 6650 Hz
    // 5: 6650 – 10000 Hz
    private final FirBandPass[] bands = new FirBandPass[5];

    // ganhos em dB e linear para cada banda
    private final double[] gainDb  = new double[5];
    private final double[] gainLin = new double[5];

    public EqualizerEngine() {
        int K = 64; // N = 2K+1 taps

        bands[0] = new FirBandPass(FS,  100.0,   215.0,  K);
        bands[1] = new FirBandPass(FS,  215.0,   665.0,  K);
        bands[2] = new FirBandPass(FS,  665.0,  2150.0,  K);
        bands[3] = new FirBandPass(FS, 2150.0,  6650.0,  K);
        bands[4] = new FirBandPass(FS, 6650.0, 10000.0,  K);

        // todos começam em 0 dB (ganho 1.0)
        for (int i = 0; i < 5; i++) {
            gainDb[i]  = 0.0;
            gainLin[i] = 1.0;
        }
    }

    public int getNumBands() {
        return 5;
    }

    public void setBandGainDb(int band, double db) {
        if (band < 0 || band >= 5) return;

        // limitar range de ganho, ex.: [-12, +12] dB
        if (db > 12.0) db = 12.0;
        if (db < -12.0) db = -12.0;

        gainDb[band]  = db;
        gainLin[band] = Math.pow(10.0, db / 20.0);
    }

    public double getBandGainDb(int band) {
        if (band < 0 || band >= 5) return 0.0;
        return gainDb[band];
    }

    /**
     * Processa IN-PLACE o bloco de samples intercalados (L,R,L,R,...).
     *
     * Modelo:
     *   y[n] = x[n] + Σ (G_i - 1) * b_i[n]
     *
     * onde:
     *   x[n]   = sinal original (dry)
     *   b_i[n] = x[n] filtrado na banda i (passa-faixa FIR)
     *   G_i    = ganho linear da banda i
     *
     * Se todos G_i = 1 (0 dB), então:
     *   y[n] = x[n]
     */
    public void processInPlace(float[] samples, int length) {
        if (length <= 0) return;

        // 1) dry = cópia do sinal original
        float[] dry = new float[length];
        System.arraycopy(samples, 0, dry, 0, length);

        // 2) acc = começa como o sinal original
        float[] acc = new float[length];
        System.arraycopy(dry, 0, acc, 0, length);

        // 3) buffer temporário: saída da banda b
        float[] tmp = new float[length];

        // 4) para cada banda
        for (int b = 0; b < 5; b++) {
            double G = gainLin[b];

            // Se G == 1.0 (0 dB), essa banda não muda nada → pode pular
            if (Math.abs(G - 1.0) < 1e-6) {
                continue;
            }

            // tmp = cópia do sinal original
            System.arraycopy(dry, 0, tmp, 0, length);

            // aplica o FIR passa-faixa dessa banda
            bands[b].processBlock(tmp, length);

            // fator de correção = (G - 1)
            float gCorr = (float) (G - 1.0);

            // acumula: acc[n] += (G - 1) * tmp[n]
            for (int n = 0; n < length; n++) {
                acc[n] += gCorr * tmp[n];
            }
        }

        // 5) LIMITER SUAVE DE PICO (normalização de bloco)
        float threshold = 0.95f;  // teto de segurança (< 1.0)
        float maxAbs = 0.0f;

        // encontra o pico absoluto no bloco
        for (int n = 0; n < length; n++) {
            float v = Math.abs(acc[n]);
            if (v > maxAbs) {
                maxAbs = v;
            }
        }

        // se ultrapassou o teto, normaliza todo o bloco
        if (maxAbs > threshold) {
            float scale = threshold / maxAbs;
            for (int n = 0; n < length; n++) {
                acc[n] *= scale;
            }
        }

        // 6) escreve saída final de volta em samples
        System.arraycopy(acc, 0, samples, 0, length);
    }
}
