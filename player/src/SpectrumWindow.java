import javax.swing.*;
import java.awt.*;

public class SpectrumWindow extends JFrame {

    private final SpectrumPanel panel;

    public SpectrumWindow(int numBands) {
        super("Spectrum Analyzer");

        this.panel = new SpectrumPanel(numBands);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 300);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
        setVisible(true);
    }

    public void updateBands(double[] bandDb) {
        // copia para evitar problemas se o array for reutilizado
        double[] copy = bandDb.clone();
        SwingUtilities.invokeLater(() -> panel.updateBands(copy));
    }

    // Painel interno que desenha as barras
    private static class SpectrumPanel extends JPanel {

        private double[] levels;  // valores já normalizados [0..1]
        private double[] smooth;  // níveis suavizados no tempo

        public SpectrumPanel(int numBands) {
            this.levels = new double[numBands];
            this.smooth = new double[numBands];
        }

        public synchronized void updateBands(double[] bandDb) {
            if (bandDb.length != levels.length) {
                levels = new double[bandDb.length];
                smooth = new double[bandDb.length];
            }

            // Converte dB relativos (0 a -40) para [0..1] e suaviza
            for (int i = 0; i < bandDb.length; i++) {
                double db = bandDb[i]; // 0 dB = banda mais forte

                double norm = (db + 40.0) / 40.0;  // -40 dB → 0, 0 dB → 1
                if (norm < 0.0) norm = 0.0;
                if (norm > 1.0) norm = 1.0;

                levels[i] = norm;

                // suavização exponencial simples (attack/decay leve)
                double alpha = 0.3; // maior = mais rápido
                smooth[i] = smooth[i] + alpha * (levels[i] - smooth[i]);
            }

            repaint();
        }

        @Override
        protected synchronized void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (smooth == null || smooth.length == 0) return;

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            int n = smooth.length;
            int barWidth = Math.max(10, w / (n * 2));
            int gap = barWidth;

            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, w, h);

            g2.setColor(new Color(100, 200, 255));

            for (int i = 0; i < n; i++) {
                int x = i * (barWidth + gap) + gap / 2;
                int barHeight = (int) (smooth[i] * (h - 40));
                int y = h - barHeight - 20;

                g2.fillRoundRect(x, y, barWidth, barHeight, 6, 6);
            }
        }
    }
}
