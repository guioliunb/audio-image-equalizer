import javax.sound.sampled.*;

public class TestMixers {
    public static void main(String[] args) {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        System.out.println("Total mixers = " + mixers.length);

        for (int i = 0; i < mixers.length; i++) {
            System.out.println(i + ": " + mixers[i].getName()
                                 + " - " + mixers[i].getDescription());
        }
    }
}
