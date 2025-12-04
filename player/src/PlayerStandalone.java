public class PlayerStandalone {

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            System.out.println("Uso:");
            System.out.println("  java PlayerStandalone caminho_do_mp3");
            return;
        }

        String mp3 = args[0];
        System.out.println("=== PlayerStandalone iniciado ===");
        System.out.println("Arquivo MP3: " + mp3);

        // --- 1) Criar decodificador ---
        Mp3RealTimeDecoder decoder = new Mp3RealTimeDecoder(mp3);
        decoder.enableAudioOutput(true);  // ativa som na VM

        // --- 2) Iniciar servidor TCP para receber comandos do Quarkus ---
        GainServer gainServer = new GainServer(5555, decoder.getEqEngine());
        gainServer.start();
        System.out.println("[OK] GainServer escutando na porta 5555.");

        // --- 3) Iniciar decodificação e reprodução ---
        System.out.println("Reproduzindo áudio... Pressione Ctrl+C para parar.");
        decoder.start(block -> {
            // Se quiser analisar blocos ou imprimir FFT, este é o lugar.
            // Por enquanto: nada.
        });

        System.out.println("Fim da execução.");
    }
}
