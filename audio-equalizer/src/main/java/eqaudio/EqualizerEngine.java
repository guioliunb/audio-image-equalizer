package eqaudio;

/**
 * Engine de equalização.
 * Por enquanto apenas bypass (não altera o áudio),
 * mas já define a interface para aplicar filtros no futuro.
 */
public class EqualizerEngine {

    /**
     * Processa o bloco de áudio IN-PLACE.
     * @param samples array de samples em float[-1,1] (interleaved se estéreo)
     * @param length  quantidade válida de samples no array
     */
    public void processInPlace(float[] samples, int length) {
        // FUTURO: aplicar filtros por banda aqui.
        // Neste momento é bypass total.
        // Exemplo de estrutura:
        // for (int i = 0; i < length; i += 2) {
        //     float left = samples[i];
        //     float right = samples[i+1];
        //     ...
        //     samples[i] = leftProcessado;
        //     samples[i+1] = rightProcessado;
        // }
    }
}
