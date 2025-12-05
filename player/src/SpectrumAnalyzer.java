import java.util.function.Consumer;

public class SpectrumAnalyzer {

    private final int fftSize;        // Ex: 1024
    private final float sampleRate;   // 44100 Hz
    private final int numBands = 10;

    // limites de banda em Hz (11 valores para 10 bandas)
    private final double[] bandEdgesHz;

    // buffers da FFT
    private final double[] re;
    private final double[] im;

    // contador para reduzir taxa de atualização (ex.: ~10 Hz)
    private int frameCounter = 0;

    // callback para enviar o espectro em dB para quem quiser (UI, log, etc.)
    private final Consumer<double[]> listener;

    public SpectrumAnalyzer(float sampleRate, int fftSize, Consumer<double[]> listener) {
        this.sampleRate = sampleRate;
        this.fftSize = fftSize;
        this.listener = listener;

        this.re = new double[fftSize];
        this.im = new double[fftSize];

        // 10 bandas logarítmicas entre 50 Hz e 20000 Hz
        bandEdgesHz = new double[numBands + 1];
        double fMin = 50.0;
        double fMax = 20000.0;
        double ratio = Math.pow(fMax / fMin, 1.0 / numBands);
        bandEdgesHz[0] = fMin;
        for (int i = 1; i <= numBands; i++) {
            bandEdgesHz[i] = bandEdgesHz[i - 1] * ratio;
        }
    }

    /**
     * Analisa um bloco intercalado estéreo (L,R,L,R,...) e envia o vetor de 10 bandas em dB
     * para o callback listener (caso não seja null).
     */
    public void analyze(float[] interleaved) {
        int totalSamples = interleaved.length;
        int channels = 2;  // no nosso player, é sempre estéreo
        int numFrames = totalSamples / channels;

        int N = Math.min(fftSize, numFrames);
        if (N <= 0) return;

        // 1) Converter pra mono + janela de Hann
        for (int n = 0; n < fftSize; n++) {
            if (n < N) {
                int idxL = n * channels;
                int idxR = idxL + 1;
                float left = interleaved[idxL];
                float right = (idxR < totalSamples) ? interleaved[idxR] : left;
                double mono = 0.5 * (left + right);

                double w = 0.5 * (1.0 - Math.cos(2.0 * Math.PI * n / (N - 1)));
                re[n] = mono * w;
            } else {
                re[n] = 0.0;
            }
            im[n] = 0.0;
        }

        // 2) FFT in-place
        fft(re, im);

        // 3) Magnitude^2 dos bins positivos
        int nBins = fftSize / 2;
        double[] mag2 = new double[nBins];
        for (int k = 0; k < nBins; k++) {
            mag2[k] = re[k] * re[k] + im[k] * im[k];
        }

        // 4) Energia por banda
        double[] bandEnergy = new double[numBands];

        for (int b = 0; b < numBands; b++) {
            double fLow = bandEdgesHz[b];
            double fHigh = bandEdgesHz[b + 1];

            int kMin = (int) Math.ceil(fLow * fftSize / sampleRate);
            int kMax = (int) Math.floor(fHigh * fftSize / sampleRate);

            if (kMin < 1) kMin = 1;
            if (kMax >= nBins) kMax = nBins - 1;
            if (kMax < kMin) kMax = kMin;

            double sum = 0.0;
            int count = 0;
            for (int k = kMin; k <= kMax; k++) {
                sum += mag2[k];
                count++;
            }
            if (count > 0) {
                sum /= count;
            }
            bandEnergy[b] = sum;
        }

        // 5) Converter energia para dB relativos (0 dB = banda mais forte do bloco)
        double maxE = 0.0;
        for (double e : bandEnergy) {
            if (e > maxE) maxE = e;
        }
        if (maxE <= 0.0) maxE = 1e-12;

        double[] bandDb = new double[numBands];
        for (int b = 0; b < numBands; b++) {
            double ratio = bandEnergy[b] / maxE;
            if (ratio <= 1e-12) ratio = 1e-12;
            bandDb[b] = 10.0 * Math.log10(ratio);  // 0 dB … -∞
        }

        // 6) Atualiza UI a cada ~4 blocos (~10 vezes/s)
        frameCounter++;
        if (frameCounter % 4 != 0) {
            return;
        }

        if (listener != null) {
            listener.accept(bandDb);
        }
    }

    // =========================
    // FFT Cooley-Tukey radix-2
    // =========================
    private void fft(double[] real, double[] imag) {
        int n = real.length;
        int logN = 0;
        for (int tmp = n; tmp > 1; tmp >>= 1) {
            logN++;
        }

        // bit-reversal
        for (int i = 0; i < n; i++) {
            int j = reverseBits(i, logN);
            if (j > i) {
                double tr = real[i];
                real[i] = real[j];
                real[j] = tr;

                double ti = imag[i];
                imag[i] = imag[j];
                imag[j] = ti;
            }
        }

        // estágios
        for (int s = 1; s <= logN; s++) {
            int m = 1 << s;
            int m2 = m >> 1;
            double theta = -2.0 * Math.PI / m;
            double wpr = Math.cos(theta);
            double wpi = Math.sin(theta);

            for (int k = 0; k < n; k += m) {
                double wr = 1.0;
                double wi = 0.0;
                for (int j = 0; j < m2; j++) {
                    int t = k + j + m2;
                    int u = k + j;

                    double tr = wr * real[t] - wi * imag[t];
                    double ti = wr * imag[t] + wi * real[t];

                    real[t] = real[u] - tr;
                    imag[t] = imag[u] - ti;

                    real[u] += tr;
                    imag[u] += ti;

                    double tmpWr = wr * wpr - wi * wpi;
                    wi = wr * wpi + wi * wpr;
                    wr = tmpWr;
                }
            }
        }
    }

    private int reverseBits(int x, int bits) {
        int y = 0;
        for (int i = 0; i < bits; i++) {
            y = (y << 1) | (x & 1);
            x >>= 1;
        }
        return y;
    }
}
