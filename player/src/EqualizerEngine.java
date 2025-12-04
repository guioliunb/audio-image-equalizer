public class EqualizerEngine {

    private static final float FS = 44100f;
    private static final int NUM_BANDS = 5;

    // 5 filtros FIR, um por banda
    private final FirBandPass[] bands = new FirBandPass[NUM_BANDS];

    // ganhos por banda (dB e linear)
    private final double[] gainDb = new double[NUM_BANDS];
    private final double[] gainLinear = new double[NUM_BANDS];

    public EqualizerEngine() {
        int K = 64; // N = 2K+1 = 129 taps

        // Frequências centrais (Hz)
        double[] fc = { 100.0, 330.0, 1000.0, 3300.0, 10000.0 };

        // Cortes entre bandas (meio entre centrais)
        double[] cut = new double[NUM_BANDS + 1];
        cut[0] = 0.0;                     // início (DC)
        cut[1] = (fc[0] + fc[1]) / 2.0;   // 215 Hz
        cut[2] = (fc[1] + fc[2]) / 2.0;   // 665 Hz
        cut[3] = (fc[2] + fc[3]) / 2.0;   // 2150 Hz
        cut[4] = (fc[3] + fc[4]) / 2.0;   // 6650 Hz
        cut[5] = FS / 2.0;                // Nyquist ~ 22050 Hz

        // Cria as 5 bandas
        for (int i = 0; i < NUM_BANDS; i++) {
            double fLow  = cut[i];
            double fHigh = cut[i + 1];
            bands[i] = new FirBandPass(FS, fLow, fHigh, K);

            gainDb[i] = 0.0;
            gainLinear[i] = 1.0; // 0 dB
        }
    }

    /**
     * Define o ganho de UMA banda em dB.
     */
    public void setBandGainDb(int bandIndex, double db) {
        if (bandIndex < 0 || bandIndex >= NUM_BANDS) return;
        gainDb[bandIndex] = db;
        gainLinear[bandIndex] = Math.pow(10.0, db / 20.0);
    }

    /**
     * Compatibilidade antiga: considera band 2 como banda de médios.
     */
    public void setGainDb(double db) {
        setBandGainDb(2, db);
    }

    /**
     * Novo modelo:
     *
     *   y[n] = x[n] + Σ (A_i - 1) * banda_i[n]
     *
     * - x[n]         = samples original
     * - banda_i[n]   = x[n] filtrado pela banda i
     * - A_i          = ganho linear da banda i
     */
    public void processInPlace(float[] samples, int length) {
        if (length <= 0) return;

        // 1) Guarda o sinal original x[n]
        float[] original = new float[length];
        System.arraycopy(samples, 0, original, 0, length);

        // 2) Começa a saída com o próprio sinal original (dry)
        System.arraycopy(original, 0, samples, 0, length);

        // Buffer de trabalho para cada banda
        float[] work = new float[length];

        // 3) Para cada banda, calcula correção relativa (A_i - 1)
        for (int b = 0; b < NUM_BANDS; b++) {

            double gLin = gainLinear[b];
            double delta = gLin - 1.0;

            // Se ganho é ~0 dB (A≈1), não precisa fazer nada para essa banda
            if (Math.abs(delta) < 1e-6) {
                continue;
            }

            // Copia x[n] para o buffer de trabalho
            System.arraycopy(original, 0, work, 0, length);

            // Filtra -> work[] = banda_b[n]
            bands[b].processBlock(work, length);

            float g = (float) delta;

            // y += (A_i - 1) * banda_i[n]
            for (int i = 0; i < length; i++) {
                samples[i] += g * work[i];
            }
        }

        // 4) Clamping leve para evitar estouro além de [-1,1]
        for (int i = 0; i < length; i++) {
            if (samples[i] > 1.0f) samples[i] = 1.0f;
            else if (samples[i] < -1.0f) samples[i] = -1.0f;
        }
    }
}
