package edu.hcmute.careersim.guidance.knowledge.domain;

import edu.hcmute.careersim.guidance.knowledge.dto.KnowledgeContracts.Passage;
import java.util.*;

/** Same reciprocal-rank fusion convention as legacy Syn, with the configured scoped limit. */
public final class KnowledgeRanking {
    private KnowledgeRanking() {}

    public static List<Passage> merge(
            List<Passage> lexical, List<Passage> vectors, int limit, boolean hybrid) {
        var scores = new HashMap<String, Double>();
        var all = new HashMap<String, Passage>();
        for (var list : List.of(lexical, vectors))
            for (int i = 0; i < list.size(); i++) {
                var p = list.get(i);
                all.put(p.chunkId(), p);
                scores.merge(p.chunkId(), 1.0 / (61 + i), Double::sum);
            }
        return all.values().stream()
                .sorted(
                        Comparator.<Passage>comparingDouble(p -> scores.get(p.chunkId()))
                                .reversed()
                                .thenComparingLong(p -> Long.parseLong(p.chunkId())))
                .limit(limit)
                .map(
                        p ->
                                new Passage(
                                        p.referenceId(),
                                        p.chunkId(),
                                        p.versionId(),
                                        p.knowledgeBaseId(),
                                        p.assetKey(),
                                        p.title(),
                                        p.text(),
                                        p.section(),
                                        p.sourceLocator(),
                                        p.sourceUri(),
                                        p.embeddingProfile(),
                                        scores.get(p.chunkId()),
                                        hybrid ? "HYBRID" : "LEXICAL"))
                .toList();
    }
}
