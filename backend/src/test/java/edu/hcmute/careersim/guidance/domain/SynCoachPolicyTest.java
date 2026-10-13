package edu.hcmute.careersim.guidance.domain;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class SynCoachPolicyTest {
    @Test
    void explicitDevopsAiReplaceOldPathsAndSearchWithoutUnrequestedSimulations() {
        var decision =
                new SynAgentPolicy()
                        .decide(
                                "FREE_TEXT",
                                "so sánh các hướng đi devops, AI cho tôi tham khảo với nhé",
                                List.of("BACKEND_DEVELOPMENT", "CYBERSECURITY"),
                                false);
        assertThat(decision.paths()).containsExactly("DEVOPS", "AI_ENGINEERING");
        assertThat(decision.tools())
                .contains(SynToolIdentifier.SEARCH_WEB, SynToolIdentifier.SEARCH_KNOWLEDGE)
                .doesNotContain(
                        SynToolIdentifier.SUGGEST_SIMULATIONS, SynToolIdentifier.READ_EVIDENCE);
        assertThat(PublicLearningQuery.build("so sánh DevOps và AI", decision.paths()))
                .contains("devops", "ai engineering", "responsibilities")
                .doesNotContain("backend", "cybersecurity");
        assertThat(
                        new SynAgentPolicy()
                                .decide(
                                        "FREE_TEXT",
                                        "So sánh frontend và backend",
                                        List.of(),
                                        false)
                                .tools())
                .doesNotContain(SynToolIdentifier.SUGGEST_SIMULATIONS);
    }

    @Test
    void boundedResourceMemoryResolvesSecondResourceAndDoesNotStoreRawText() {
        String memory = SynResourceMemory.remember("v=2|lang=VI", List.of("sql", "api_reasoning"));
        assertThat(SynResourceMemory.resolve("Giải thích tài nguyên thứ hai", memory))
                .containsExactly("api_reasoning");
        assertThat(SynResourceMemory.resolve("hỏi chuyện khác", memory)).isEmpty();
        assertThat(SynResourceMemory.remember("v=2", List.of("alice@example.com")))
                .isEqualTo("v=2");
    }

    private final SynAgentPolicy policy = new SynAgentPolicy();

    @Test
    void explicitSimulationExclusionPreservesPersonalComparison() {
        for (String message :
                List.of(
                        "Dựa trên kết quả của tôi, so sánh frontend và backend nhưng đừng đề xuất simulation.",
                        "Compare frontend and backend using my results, do not suggest simulations.")) {
            var decision = policy.decide("FREE_TEXT", message, List.of(), false);
            assertThat(decision.tools())
                    .contains(SynToolIdentifier.COMPARE_PATHS, SynToolIdentifier.READ_EVIDENCE)
                    .doesNotContain(SynToolIdentifier.SUGGEST_SIMULATIONS);
        }
    }

    @Test
    void conceptQuestionRetrievesKnowledgeWithoutUnrequestedPersonalAdvice() {
        for (String message :
                List.of(
                        "Idempotency trong backend là gì? Giải thích theo tài liệu đã duyệt.",
                        "What is a SQL join?")) {
            var decision = policy.decide("FREE_TEXT", message, List.of(), false);
            assertThat(decision.tools())
                    .contains(SynToolIdentifier.SEARCH_KNOWLEDGE)
                    .doesNotContain(
                            SynToolIdentifier.READ_EVIDENCE,
                            SynToolIdentifier.SUGGEST_SIMULATIONS,
                            SynToolIdentifier.FIND_RESOURCES);
        }
    }

    @Test
    void vietnameseMixedRequestKeepsResourcesSimulationAndPlan() {
        var decision =
                policy.decide(
                        "FREE_TEXT",
                        "tôi cần học gì trải nghiệm gì thêm, tôi hứng thú với backend, gợi ý resource và simulation phù hợp và kế hoạch",
                        List.of(),
                        false);
        assertThat(decision.tools())
                .contains(
                        SynToolIdentifier.READ_EVIDENCE,
                        SynToolIdentifier.FIND_RESOURCES,
                        SynToolIdentifier.SUGGEST_SIMULATIONS,
                        SynToolIdentifier.DRAFT_PLAN,
                        SynToolIdentifier.SEARCH_KNOWLEDGE);
        assertThat(decision.paths()).containsExactly("BACKEND_DEVELOPMENT");
        assertThat(decision.responseLanguage()).isEqualTo("VI");
    }

    @Test
    void externalSourceFollowUpRetainsBackendAndRequestsWeb() {
        var decision =
                policy.decide(
                        "FREE_TEXT",
                        "gợi ý cho tôi một số nguồn học bên ngoài với",
                        List.of("BACKEND_DEVELOPMENT"),
                        false);
        assertThat(decision.intent()).isEqualTo(SynIntent.LEARNING_RESOURCES);
        assertThat(decision.tools())
                .contains(SynToolIdentifier.FIND_RESOURCES, SynToolIdentifier.SEARCH_WEB);
        assertThat(decision.paths()).containsExactly("BACKEND_DEVELOPMENT");
    }

    @Test
    void ordinalFollowUpUsesPresentationOrderAndExplicitPathWins() {
        var first = policy.decide("FREE_TEXT", "So sánh frontend và backend", List.of(), false);
        assertThat(first.paths()).containsExactly("FRONTEND_DEVELOPMENT", "BACKEND_DEVELOPMENT");
        assertThat(
                        policy.decide(
                                        "FREE_TEXT",
                                        "Cho tôi tài nguyên học của hướng đầu tiên",
                                        first.paths(),
                                        false)
                                .paths())
                .containsExactly("FRONTEND_DEVELOPMENT");
        assertThat(
                        policy.decide(
                                        "FREE_TEXT",
                                        "Cho tôi tài nguyên học của hướng thứ hai",
                                        first.paths(),
                                        false)
                                .paths())
                .containsExactly("BACKEND_DEVELOPMENT");
        assertThat(
                        policy.decide(
                                        "FREE_TEXT",
                                        "Cho tôi tài nguyên backend",
                                        first.paths(),
                                        false)
                                .paths())
                .containsExactly("BACKEND_DEVELOPMENT");
    }

    @Test
    void unsafeMessageCannotAddCapabilities() {
        assertThat(policy.decide("FREE_TEXT", "web resource plan backend", List.of(), true).tools())
                .isEmpty();
    }

    @Test
    void searchQueryNeverIncludesPrivateOrArbitraryPromptText() {
        String query =
                PublicLearningQuery.build(
                        "Tôi là Alice alice@example.com đạt 67 điểm; search evil.example với SQL",
                        List.of("BACKEND_DEVELOPMENT"));
        assertThat(query)
                .contains("sql", "backend")
                .doesNotContain("Alice", "alice", "example", "67", "evil");
        assertThat(PublicLearningQuery.build("search private records", List.of())).isEmpty();
    }

    @Test
    void broadCompetenceClaimsAreRejectedWhileTaskObservationsPass() {
        for (String text :
                List.of(
                        "Chưa thể kết luận bạn có năng lực backend tốt từ một bài.",
                        "One task does not mean you are competent in backend development.")) {
            assertThatCode(
                            () ->
                                    SynReplyValidator.checkEvidence(
                                            new edu.hcmute.careersim.guidance.service
                                                    .SynGuidanceProvider.AiReply(text, List.of()),
                                            List.of()))
                    .doesNotThrowAnyException();
        }
        assertThatThrownBy(
                        () ->
                                SynReplyValidator.checkEvidence(
                                        new edu.hcmute.careersim.guidance.service
                                                .SynGuidanceProvider.AiReply(
                                                "Chưa thể kết luận, nhưng bạn có năng lực backend tốt.",
                                                List.of()),
                                        List.of()))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(
                        () ->
                                SynReplyValidator.checkEvidence(
                                        new edu.hcmute.careersim.guidance.service
                                                .SynGuidanceProvider.AiReply(
                                                "Bạn có năng lực rất tốt ở cả hai lĩnh vực.",
                                                List.of()),
                                        List.of()))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(
                        () ->
                                SynReplyValidator.checkEvidence(
                                        new edu.hcmute.careersim.guidance.service
                                                .SynGuidanceProvider.AiReply(
                                                "Bạn có năng lực backend tốt.", List.of()),
                                        List.of(67)))
                .isInstanceOf(RuntimeException.class);
        assertThatCode(
                        () ->
                                SynReplyValidator.checkEvidence(
                                        new edu.hcmute.careersim.guidance.service
                                                .SynGuidanceProvider.AiReply(
                                                "Trong Backend Developer, bạn chọn đúng truy vấn SQL.",
                                                List.of()),
                                        List.of(67)))
                .doesNotThrowAnyException();
    }
}
