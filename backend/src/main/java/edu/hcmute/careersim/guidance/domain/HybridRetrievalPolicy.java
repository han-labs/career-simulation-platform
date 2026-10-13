package edu.hcmute.careersim.guidance.domain;

import java.util.*;

public final class HybridRetrievalPolicy {
    private HybridRetrievalPolicy() {}

    private static final Set<String> STOP =
            Set.of(
                    "i", "my", "me", "the", "a", "an", "is", "are", "what", "how", "can", "could",
                    "you", "to", "of", "and", "for", "in", "do", "does", "explain", "please", "toi",
                    "minh", "em", "la", "gi", "cua", "va", "cho", "ve", "voi", "nhu", "nao", "co",
                    "khong", "hay", "giai", "thich", "muon", "biet", "nen", "duoc");

    public static String query(String message) {
        return Arrays.stream(KnowledgeChunker.normalize(message).split(" "))
                .filter(t -> t.length() > 1 && !STOP.contains(t))
                .distinct()
                .limit(18)
                .reduce((a, b) -> a + " | " + b)
                .orElse("");
    }

    public static List<KnowledgeChunk> merge(
            List<KnowledgeChunk> vector, List<KnowledgeChunk> lexical) {
        Map<Long, Double> scores = new HashMap<>();
        Map<Long, KnowledgeChunk> chunks = new HashMap<>();
        for (var list : List.of(vector, lexical))
            for (int i = 0; i < list.size(); i++) {
                var chunk = list.get(i);
                chunks.put(chunk.id(), chunk);
                scores.merge(chunk.id(), 1.0 / (60 + i + 1), Double::sum);
            }
        List<KnowledgeChunk> output = new ArrayList<>();
        int characters = 0;
        for (var c :
                chunks.values().stream()
                        .sorted(
                                Comparator.<KnowledgeChunk>comparingDouble(c -> scores.get(c.id()))
                                        .reversed()
                                        .thenComparingLong(KnowledgeChunk::id))
                        .toList()) {
            if (output.size() == 5) break;
            if (characters + c.content().length() > 10000) continue;
            output.add(c);
            characters += c.content().length();
        }
        return List.copyOf(output);
    }
}
