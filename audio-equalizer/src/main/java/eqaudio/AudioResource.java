package eqaudio;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/audio")
public class AudioResource {

    private static Thread playThread;

    @GET
    @Path("/test")
    public String testDecoder() {
        try {
            // Evita iniciar 2 execuções ao mesmo tempo
            if (playThread != null && playThread.isAlive()) {
                return "Já está tocando. Aguarde terminar ou reinicie o servidor.";
            }

            playThread = new Thread(() -> {
                try {
                    // Caminho relativo à raiz do projeto (onde está o pom.xml)
                    Mp3RealTimeDecoder decoder = new Mp3RealTimeDecoder("/home/guilherme/processamento-de-sinais/AmyWinehouse.mp3", 1024);
                    decoder.enableAudioOutput(true); // toca o áudio

                    System.out.println("Iniciando decodificação em tempo real...");

                    decoder.start(block -> {
                        // Aqui você vai plugar o DSP depois.
                        // System.out.println("[Quarkus] bloco recebido: " + block.length + " samples");
                    });

                    System.out.println("Fim do MP3.");

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, "Mp3PlayerThread");

            playThread.start();

            return "Decodificador iniciado. Verifique o console e o áudio.";

        } catch (Exception e) {
            e.printStackTrace();
            return "Erro ao iniciar decodificador: " + e.getMessage();
        }
    }
}
