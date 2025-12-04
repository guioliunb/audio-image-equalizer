package eqaudio;

public class EqualizerBand {
    public final double fLow;
    public final double fHigh;
    public double gainDb = 0.0;
    public double gainLinear = 1.0;

    public EqualizerBand(double fLow, double fHigh) {
        this.fLow = fLow;
        this.fHigh = fHigh;
    }

    public void setGainDb(double db) {
        this.gainDb = db;
        this.gainLinear = Math.pow(10.0, db / 20.0);
    }
}
