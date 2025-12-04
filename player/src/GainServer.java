import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GainServer {

    private final int port;
    private final EqualizerEngine eqEngine;
    private volatile boolean running = true;
    private final ObjectMapper mapper = new ObjectMapper();

    public GainServer(int port, EqualizerEngine eqEngine) {
        this.port = port;
        this.eqEngine = eqEngine;
    }

    public void start() {
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(port)) {
                System.out.println("[GainServer] Listening on TCP port " + port);

                while (running) {
                    try (Socket client = server.accept()) {
                        System.out.println("[GainServer] Client connected");

                        BufferedReader reader =
                                new BufferedReader(new InputStreamReader(client.getInputStream()));

                        String line;
                        while ((line = reader.readLine()) != null) {

                            Map<String, Object> msg =
                                    mapper.readValue(line, Map.class);

                            handleMessage(msg);
                        }
                    }
                }

            } catch (Exception e) {
                System.err.println("[GainServer] Error: " + e);
            }
        }).start();
    }

    private void handleMessage(Map<String, Object> msg) {
    String type = (String) msg.get("type");

    if ("setGain".equals(type)) {
        int band = (int) msg.get("band");
        double gain = (double) msg.get("gainDb");

        eqEngine.setBandGainDb(band, gain);

        System.out.printf(
            "[EQ-UPDATE] Banda %d (%s) ajustada para %.1f dB%n",
            band,
            getBandName(band),
            gain
        );
    }

    if ("setAll".equals(type)) {
        System.out.println("[EQ-UPDATE] Atualização de todas as bandas:");

        for (int i = 0; i < 5; i++) {
            String key = "band" + i;
            if (msg.containsKey(key)) {
                double gain = (double) msg.get(key);
                eqEngine.setBandGainDb(i, gain);

                System.out.printf(
                        "   - Banda %d (%s) -> %.1f dB%n",
                        i,
                        getBandName(i),
                        gain
                );
            }
        }

        System.out.println("[EQ-UPDATE] Todas as bandas aplicadas.\n");
    }
}

    private String getBandName(int band) {
        return switch (band) {
            case 0 -> "100 Hz";
            case 1 -> "330 Hz";
            case 2 -> "1 kHz";
            case 3 -> "3.3 kHz";
            case 4 -> "10 kHz";
            default -> "??";
        };
    }


    public void stop() {
        running = false;
    }
}
