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

        // --- Janela gráfica do analisador de espectro (10 bandas) ---
        SpectrumWindow spectrumWindow = new SpectrumWindow(10);

        // --- Decoder de MP3 com equalizador interno ---
        Mp3RealTimeDecoder decoder = new Mp3RealTimeDecoder(mp3);
        decoder.enableAudioOutput(true);  // ativa som na VM

        // EqualizerEngine já está dentro do decoder.
        // Iniciamos o GainServer para receber ganhos do Quarkus.
        GainServer gainServer = new GainServer(5555, decoder.getEqEngine());
        gainServer.start();
        System.out.println("[OK] GainServer escutando na porta 5555.");

        // --- Analisador de espectro (FFT 1024, Fs=44100) ---
        // Toda vez que houver um novo vetor de 10 bandas em dB,
        // ele é enviado para a janela atualizar as barras.
        SpectrumAnalyzer analyzer = new SpectrumAnalyzer(
                44100f,
                1024,
                bandDb -> spectrumWindow.updateBands(bandDb)
        );

        System.out.println("Reproduzindo áudio... Pressione Ctrl+C para parar.");

        // Inicia a decodificação em tempo real
        decoder.start(block -> {
            // block é float[] intercalado (L,R,L,R,...), já equalizado
            // Enviamos esse bloco para o analisador de espectro
            analyzer.analyze(block);
        });

        System.out.println("Fim da execução.");
    }
}
