package edu.hcmute.careersim.guidance.domain;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.hcmute.careersim.guidance.service.*;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class SynRagPolicyTest {
    @Test
    void schemaRejectsCoercionExtrasMissingFieldsAndDuplicateSuggestions() throws Exception {
        var mapper = new ObjectMapper();
        for (String invalid :
                List.of(
                        "[]",
                        "{\"text\":42,\"suggestions\":[]}",
                        "{\"text\":\"hi\"}",
                        "{\"text\":\"hi\",\"suggestions\":null}",
                        "{\"text\":\"hi\",\"suggestions\":[false]}",
                        "{\"text\":\"hi\",\"suggestions\":[\"x\",\"x\"]}",
                        "{\"text\":\"hi\",\"suggestions\":[],\"score\":100}")) {
            var node = mapper.readTree(invalid);
            assertThatThrownBy(() -> SynReplyValidator.parse(node))
                    .isInstanceOf(SynGuidanceProvider.ProviderException.class);
        }
        assertThat(
                        SynReplyValidator.parse(
                                        mapper.readTree(
                                                "{\"text\":\"Giải thích rõ.\",\"suggestions\":[]}"))
                                .text())
                .isEqualTo("Giải thích rõ.");
        assertThatThrownBy(
                        () ->
                                SynReplyValidator.parse(
                                        mapper.valueToTree(
                                                java.util.Map.of(
                                                        "text",
                                                        "x".repeat(2401),
                                                        "suggestions",
                                                        List.of()))))
                .isInstanceOf(SynGuidanceProvider.ProviderException.class);
    }

    @Test
    void chunkingKeepsCodeBlocksAndRejectsOversizeInsteadOfCuttingSource() {
        String content =
                "# SQL\n\nExplain the query.\n\n```sql\nSELECT id\n\nFROM sample;\n```\n\n# API\n\nA request and response.";
        var chunks = KnowledgeChunker.split("Original", content);
        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).text()).contains("SELECT id\n\nFROM sample;");
        assertThat(chunks.get(1).section()).isEqualTo("API");
        assertThatThrownBy(() -> KnowledgeChunker.split("Large", "x".repeat(2401)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KnowledgeChunker.split("Code", "```java\nhello"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lexicalQueryNormalizesVietnameseAndCannotInjectTsqueryOperators() {
        assertThat(HybridRetrievalPolicy.query("Giải thích kiểm thử SQL'; DROP TABLE!"))
                .isEqualTo("kiem | thu | sql | drop | table");
        assertThat(HybridRetrievalPolicy.query("what is")).isEmpty();
    }

    @Test
    void fusionRewardsAgreementAndDeduplicatesWithinBudget() {
        var a = chunk(1);
        var b = chunk(2);
        var c = chunk(3);
        var result = HybridRetrievalPolicy.merge(List.of(a, b), List.of(b, c));
        assertThat(result).extracting(KnowledgeChunk::id).containsExactly(2L, 1L, 3L);
        assertThat(HybridRetrievalPolicy.merge(List.of(), List.of())).isEmpty();
    }

    @Test
    void deadlineAndConsentFailClosedAndScopeIsRemoved() {
        assertThat(SynExecutionScope.mayUseExternal()).isFalse();
        try (var scope = SynExecutionScope.open(Duration.ofMillis(1), () -> false)) {
            assertThatThrownBy(SynExecutionScope::checkExternal)
                    .isInstanceOf(SynGuidanceProvider.ProviderException.class);
            assertThatThrownBy(() -> SynExecutionScope.remaining(Duration.ofSeconds(5)))
                    .isInstanceOf(SynGuidanceProvider.ProviderException.class);
        }
        assertThat(SynExecutionScope.mayUseExternal()).isFalse();
    }

    @Test
    void explicitFrontendWinsOverMentionedSqlAndPersonalEvidenceDoesNotRequireKnowledge() {
        var policy = new SynAgentPolicy();
        assertThat(policy.decide("FREE_TEXT", "frontend cần SQL không", List.of(), false).paths())
                .containsExactly("FRONTEND_DEVELOPMENT");
        assertThat(policy.decide("FREE_TEXT", "What is an API?", List.of(), false).tools())
                .contains(SynToolIdentifier.SEARCH_KNOWLEDGE);
        assertThat(
                        policy.decide("FREE_TEXT", "Explain my RIASEC interests", List.of(), false)
                                .tools())
                .containsExactly(SynToolIdentifier.READ_EVIDENCE);
    }

    private KnowledgeChunk chunk(long id) {
        return new KnowledgeChunk(id, 1, "Title", "Section", "Synthetic content", "EN", null, 0.9);
    }

    @Test
    void rememberedResultIsOnlyAnIdAndCannotOverflowTheMemoryBudget() {
        String summary = SynMemoryEvidence.remember("x".repeat(600), 42L);
        assertThat(summary.length()).isLessThanOrEqualTo(600);
        assertThat(SynMemoryEvidence.selectedResult(summary)).isEqualTo(42L);
        assertThat(SynMemoryEvidence.selectedResult("v=2|result=9999999999999999999")).isNull();
        assertThat(SynMemoryEvidence.refersToResult("giải thích kết quả đó")).isTrue();
    }
}
