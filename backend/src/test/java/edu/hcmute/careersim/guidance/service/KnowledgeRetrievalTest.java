package edu.hcmute.careersim.guidance.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import edu.hcmute.careersim.guidance.dao.KnowledgeDao;
import edu.hcmute.careersim.guidance.domain.KnowledgeChunk;
import edu.hcmute.careersim.guidance.service.impl.KnowledgeRetrievalServiceImpl;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class KnowledgeRetrievalTest {
    @Test
    void enabledRetrievalCombinesSemanticAndLexicalResultsWithPublishedChunkText() {
        var dao = mock(KnowledgeDao.class);
        var embedding = mock(EmbeddingProvider.class);
        double[] vector = new double[768];
        vector[0] = 1;
        when(embedding.available()).thenReturn(true);
        when(embedding.profile()).thenReturn("test:768");
        when(embedding.embed("SQL joins", true)).thenReturn(vector);
        var common = new KnowledgeChunk(1, 1, "SQL", "Join", "Published join text", "EN", null, .8);
        var lexical =
                new KnowledgeChunk(2, 1, "SQL", "Query", "Published query text", "EN", null, .7);
        when(dao.vector(vector, "test:768", 20, .5)).thenReturn(List.of(common));
        when(dao.lexical("sql | joins", 20)).thenReturn(List.of(lexical, common));
        var result =
                new KnowledgeRetrievalServiceImpl(dao, embedding, .5).retrieve("SQL joins", true);
        assertThat(result.status()).isEqualTo("HYBRID");
        assertThat(result.chunks()).extracting(KnowledgeChunk::id).containsExactly(1L, 2L);
        assertThat(result.chunks().get(0).content()).isEqualTo("Published join text");
    }

    @Test
    void noConsentUsesOnlyLocalSearchAndReturnsHonestEmptyState() {
        var dao = mock(KnowledgeDao.class);
        var embedding = mock(EmbeddingProvider.class);
        var service = new KnowledgeRetrievalServiceImpl(dao, embedding, .5);
        assertThat(service.retrieve("unknown subject", false).status())
                .isEqualTo("NO_RELEVANT_KNOWLEDGE");
        verifyNoInteractions(embedding);
        verify(dao).lexical("unknown | subject", 20);
    }

    @Test
    void embeddingFailureFallsBackToLexicalAndDoesNotRetryWithinTurn() {
        var dao = mock(KnowledgeDao.class);
        var embedding = mock(EmbeddingProvider.class);
        when(embedding.available()).thenReturn(true);
        when(embedding.embed(anyString(), eq(true)))
                .thenThrow(new IllegalStateException("synthetic outage"));
        when(dao.lexical(anyString(), eq(20)))
                .thenReturn(
                        List.of(
                                new KnowledgeChunk(
                                        1, 1, "SQL", "Query", "Reviewed fixture", "EN", null, 1)));
        var service = new KnowledgeRetrievalServiceImpl(dao, embedding, .5);
        try (var scope = SynExecutionScope.open(Duration.ofSeconds(20), () -> true)) {
            var first = service.retrieve("SQL contact me at private@example.test 123", true);
            assertThat(first.status()).isEqualTo("EMBEDDING_UNAVAILABLE");
            assertThat(service.retrieve("SQL contact me at private@example.test 123", true))
                    .isSameAs(first);
        }
        verify(embedding, times(1)).embed("SQL contact me at", true);
        verify(dao, never()).vector(any(), any(), anyInt(), anyDouble());
    }

    @Test
    void unreviewedDocumentsNeverReachEmbeddingOrDatabase() {
        var dao = mock(KnowledgeDao.class);
        var embedding = mock(EmbeddingProvider.class);
        var service = new KnowledgeIngestionService(dao, embedding);
        assertThatThrownBy(
                        () ->
                                service.publish(
                                        new KnowledgeDao.Document(
                                                "draft",
                                                "Draft",
                                                null,
                                                "original",
                                                "EN",
                                                "BACKEND",
                                                "",
                                                false,
                                                "Text")))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(dao, embedding);
    }
}
