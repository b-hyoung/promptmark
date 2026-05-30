package local.promptmark.service.llm;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Deterministic 1536-dim embedding for tests. Seeded with the input text's
 * hash so the same string maps to the same vector. Vectors are unit-normalised
 * so cosine similarity comparisons stay stable.
 */
public final class FakeEmbeddingClient implements EmbeddingClient {

    public static final int DIM = 1536;

    private final List<String> received = new ArrayList<>();

    @Override
    public float[] embed(String text) {
        received.add(text);
        long seed = text == null ? 0L : (long) text.hashCode();
        Random rng = new Random(seed);
        float[] out = new float[DIM];
        double sumSq = 0.0;
        for (int i = 0; i < DIM; i++) {
            out[i] = (float) rng.nextGaussian();
            sumSq += out[i] * out[i];
        }
        double norm = Math.sqrt(sumSq);
        if (norm > 0) {
            for (int i = 0; i < DIM; i++) out[i] = (float) (out[i] / norm);
        }
        return out;
    }

    @Override
    public boolean enabled() {
        return true;
    }

    public List<String> received() { return received; }
}
