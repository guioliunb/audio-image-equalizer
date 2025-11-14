package eqaudio;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Consumer;

/**
 * Decodifica MP3 em PCM 44.1 kHz estéreo e fornece os dados em tempo real.
 * Aplica EqualizerEngine sobre blocos em float[], toca o áudio processado
 * e salva o resultado em arquivo WAV.
 */
public class Mp3RealTimeDecoder {

    private static final int BLOCK_SIZE_SAMPLES = 1024; // frames por bloco
    private static final int SAMPLE_SIZE_BITS = 16;
    private static final float SAMPLE_RATE = 44100f;
    private static final int CHANNELS = 2; // JLayer saída estéreo

    private final File mp3File;
    private final int blockSizeFrames;
    private boolean playing = false;
    private boolean outputAudio = false;  // se true, envia para alto-falante
    private final EqualizerEngine eqEngine = new EqualizerEngine();

    public Mp3RealTimeDecoder(String path) {
        this(path, BLOCK_SIZE_SAMPLES);
    }

    public Mp3RealTimeDecoder(String path, int blockSizeFrames) {
        this.mp3File = new File(path);
        this.blockSizeFrames = blockSizeFrames;
    }

    public void enableAudioOutput(boolean enable) {
        this.outputAudio = enable;
    }

    /**
     * Inicia o streaming real-time do MP3.
     * O callback opcional recebe blocos PCM já processados em float[-1,1].
     */
    public void start(Consumer<float[]> onBlock) throws Exception {

        playing = true;

        try (InputStream fileStream = new BufferedInputStream(new FileInputStream(mp3File))) {

            Bitstream bitstream = new Bitstream(fileStream);
            Decoder decoder = new Decoder();

            // Formato PCM para saída de áudio
            AudioFormat pcmFmt = new AudioFormat(
                    SAMPLE_RATE,
                    SAMPLE_SIZE_BITS,
                    CHANNELS,
                    true,   // signed
                    false   // little-endian
            );

            // Linha de áudio (opcional)
            // === 3) Linha de áudio (opcional) ===
            SourceDataLine line = null;

            if (outputAudio) {
                try {
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, pcmFmt);
                    line = (SourceDataLine) AudioSystem.getLine(info);
                    line.open(pcmFmt);
                    line.start();

                    System.out.println("Placa de som encontrada. Áudio ON.");

                } catch (Exception e) {
                    System.err.println("⚠ Nenhum dispositivo de áudio disponível. "
                            + "Continuando em modo silencioso (silent mode).");
                    line = null;       // impede uso da placa
                    outputAudio = false;
                }
            }


            // Arquivo WAV de saída para debug (sempre salva o áudio processado)
            File outFile = new File("output_eq.wav");
            try (RandomAccessFile wavOut = new RandomAccessFile(outFile, "rw")) {
                wavOut.setLength(0); // limpa se já existir
                writeWavHeaderPlaceholder(wavOut, SAMPLE_RATE, CHANNELS, SAMPLE_SIZE_BITS);

                // buffers de trabalho
                int samplesPerBlock = blockSizeFrames * CHANNELS;
                float[] floatBlock = new float[samplesPerBlock];
                byte[] byteBlock = new byte[samplesPerBlock * 2]; // 16 bits = 2 bytes
                int floatPos = 0;
                long dataBytesWritten = 0;

                // Loop de decodificação frame-a-frame
                while (playing) {

                    Header frameHeader = bitstream.readFrame();
                    if (frameHeader == null) {
                        break; // fim do MP3
                    }

                    SampleBuffer output = (SampleBuffer) decoder.decodeFrame(frameHeader, bitstream);
                    short[] pcmSamples = output.getBuffer();
                    int samplesDecoded = output.getBufferLength(); // L,R,L,R,...

                    // Copia samples decodificados para o bloco float
                    for (int i = 0; i < samplesDecoded; i++) {
                        if (floatPos >= floatBlock.length) {
                            // bloco cheio → processa
                            dataBytesWritten += processAndOutputBlock(
                                    floatBlock, floatPos,
                                    byteBlock, line, wavOut,
                                    onBlock
                            );
                            floatPos = 0;
                        }
                        floatBlock[floatPos++] = pcmSamples[i] / 32768f;
                    }

                    bitstream.closeFrame();
                }

                // Processa bloco parcial (último)
                if (floatPos > 0) {
                    dataBytesWritten += processAndOutputBlock(
                            floatBlock, floatPos,
                            byteBlock, line, wavOut,
                            onBlock
                    );
                }

                // Atualiza cabeçalho WAV com tamanho correto
                finalizeWavHeader(wavOut, dataBytesWritten, SAMPLE_RATE, CHANNELS, SAMPLE_SIZE_BITS);

                if (outputAudio && line != null) {
                    line.drain();
                    line.stop();
                    line.close();
                }

                bitstream.close();
            }
        }
    }

    /**
     * Processa um bloco: EQ in-place, converte para PCM16, toca e grava no WAV.
     *
     * @param floatBlock  samples em float[-1,1]
     * @param length      quantidade válida de samples no bloco
     * @param byteBlock   buffer para saída em PCM16
     * @param line        linha de áudio (pode ser null)
     * @param wavOut      arquivo WAV de saída
     * @param onBlock     callback opcional para debug/visualização
     * @return número de bytes de áudio escritos no arquivo WAV
     */
    private long processAndOutputBlock(
            float[] floatBlock,
            int length,
            byte[] byteBlock,
            SourceDataLine line,
            RandomAccessFile wavOut,
            Consumer<float[]> onBlock
    ) {
        // 1) Equalização in-place (por enquanto bypass)
        eqEngine.processInPlace(floatBlock, length);

        // 2) Callback do usuário (visualização, debug, espectro, etc.)
        if (onBlock != null) {
            onBlock.accept(floatBlock);
        }

        // 3) Converter para PCM16 LE
        int bytesValid = floatToPcm16(floatBlock, length, byteBlock);

        // 4) Tocar áudio processado
        try {
            if (line != null && outputAudio) {
                try {
                    line.write(byteBlock, 0, bytesValid);
                } catch (Exception ex) {
                    System.err.println("Falha ao escrever na placa de som: " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            // Se der problema na placa de som da VM, não queremos quebrar o fluxo
            System.err.println("Falha ao escrever na placa de som: " + e.getMessage());
        }

        // 5) Gravar áudio processado em arquivo WAV
        try {
            wavOut.write(byteBlock, 0, bytesValid);
        } catch (IOException e) {
            System.err.println("Falha ao escrever no arquivo WAV: " + e.getMessage());
        }

        return bytesValid;
    }

    /** Para parar o streaming (em uso futuro com controle externo) */
    public void stop() {
        playing = false;
    }

    // ===========================
    // Conversão float → PCM16 LE
    // ===========================
    private int floatToPcm16(float[] samples, int length, byte[] out) {
        int byteIndex = 0;
        for (int i = 0; i < length; i++) {
            float v = samples[i];
            // clamp
            if (v > 1.0f) v = 1.0f;
            else if (v < -1.0f) v = -1.0f;

            short s = (short) Math.round(v * 32767f);
            out[byteIndex++] = (byte) (s & 0xFF);
            out[byteIndex++] = (byte) ((s >>> 8) & 0xFF);
        }
        return byteIndex;
    }

    // ===========================
    // WAV header helpers
    // ===========================
    private void writeWavHeaderPlaceholder(RandomAccessFile raf, float sampleRate,
                                           int channels, int bitsPerSample) throws IOException {
        raf.seek(0);
        // RIFF chunk descriptor
        raf.writeBytes("RIFF");
        raf.writeInt(0); // placeholder para chunk size
        raf.writeBytes("WAVE");

        // fmt subchunk
        raf.writeBytes("fmt ");
        raf.writeInt(Integer.reverseBytes(16)); // Subchunk1Size = 16 para PCM
        raf.writeShort(Short.reverseBytes((short) 1)); // AudioFormat = 1 (PCM)
        raf.writeShort(Short.reverseBytes((short) channels)); // NumChannels
        raf.writeInt(Integer.reverseBytes((int) sampleRate)); // SampleRate
        int byteRate = (int) sampleRate * channels * bitsPerSample / 8;
        raf.writeInt(Integer.reverseBytes(byteRate)); // ByteRate
        short blockAlign = (short) (channels * bitsPerSample / 8);
        raf.writeShort(Short.reverseBytes(blockAlign)); // BlockAlign
        raf.writeShort(Short.reverseBytes((short) bitsPerSample)); // BitsPerSample

        // data subchunk
        raf.writeBytes("data");
        raf.writeInt(0); // placeholder para Subchunk2Size
    }

    private void finalizeWavHeader(RandomAccessFile raf, long dataSize, float sampleRate,
                                   int channels, int bitsPerSample) throws IOException {
        // ChunkSize = 4 + (8 + Subchunk1Size) + (8 + Subchunk2Size)
        long chunkSize = 36 + dataSize;

        // Atualiza ChunkSize
        raf.seek(4);
        raf.writeInt(Integer.reverseBytes((int) chunkSize));

        // Atualiza Subchunk2Size (data size)
        raf.seek(40);
        raf.writeInt(Integer.reverseBytes((int) dataSize));

        // (Opcional) poderia validar byteRate, blockAlign aqui, mas já foi escrito certo.
    }
}
