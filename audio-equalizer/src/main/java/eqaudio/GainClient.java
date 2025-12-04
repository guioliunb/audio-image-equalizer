package eqaudio;

import jakarta.enterprise.context.ApplicationScoped;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

@ApplicationScoped
public class GainClient {

    private final ObjectMapper mapper = new ObjectMapper();

    public void sendToPlayer(Map<String, Object> json) {
        try (Socket sock = new Socket("localhost", 5555)) {
            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(sock.getOutputStream()), true);

            out.println(mapper.writeValueAsString(json));
        } catch (Exception e) {
            System.err.println("[GainClient] Falha ao enviar comando: " + e.getMessage());
        }
    }
}
