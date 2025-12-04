package eqaudio;

import jakarta.enterprise.context.ApplicationScoped;

import java.io.File;
import java.io.IOException;

@ApplicationScoped
public class PlayerLauncher {

    // caminho da pasta do player standalone
    // ajuste se for diferente
    private static final String PLAYER_DIR =
            "/home/guilherme/processamento-de-sinais/player";

    private static final String JAVA_CMD = "java";

    // processo atual do player (se houver)
    private Process currentProcess;

    /**
     * Inicia o player externo tocando o MP3 indicado.
     * Se já houver um player rodando, opcionalmente paramos antes.
     */
    public synchronized void play(String mp3Path) throws IOException {
        // se já tiver um player rodando, para antes
        if (currentProcess != null && currentProcess.isAlive()) {
            System.out.println("Parando player anterior antes de iniciar novo PLAY...");
            currentProcess.destroy();
        }

        ProcessBuilder pb = new ProcessBuilder(
                JAVA_CMD,
                "-cp", "src:lib/*",
                "PlayerStandalone",
                mp3Path
        );

        pb.directory(new File(PLAYER_DIR));
        pb.inheritIO(); // logs do player vão para o console do Quarkus

        System.out.println("Lançando player externo para arquivo: " + mp3Path);
        currentProcess = pb.start();
    }

    /**
     * Para o player externo, se estiver rodando.
     */
    public synchronized void stop() {
        if (currentProcess != null && currentProcess.isAlive()) {
            System.out.println("Enviando destroy() para o processo do player...");
            currentProcess.destroy();
        } else {
            System.out.println("Nenhum player em execução no momento.");
        }
    }
}
