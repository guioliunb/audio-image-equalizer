package eqaudio;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class EqualizerState {

    private static final int NUM_BANDS = 5;

    private final double[] gainDb = new double[NUM_BANDS];

    public EqualizerState() {
        for (int i = 0; i < NUM_BANDS; i++) {
            gainDb[i] = 0.0; // todas as bandas começam em 0 dB
        }
    }

    public boolean setGainDb(int band, double db) {
        if (band < 0 || band >= NUM_BANDS) return false;
        gainDb[band] = db;
        return true;
    }

    public double getGainDb(int band) {
        if (band < 0 || band >= NUM_BANDS) return 0.0;
        return gainDb[band];
    }

    public Map<Integer, Double> getAllGainsDb() {
        Map<Integer, Double> map = new HashMap<>();
        for (int i = 0; i < NUM_BANDS; i++) {
            map.put(i, gainDb[i]);
        }
        return map;
    }
}
