package edu.hcmute.careersim.guidance.agent;

import static org.assertj.core.api.Assertions.*;

import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.guidance.agent.domain.AgentResponseSchema;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AgentResponseSchemaTest {
    @Test
    void groundedFinalSchemaContainsOnlyCurrentLiteralPairsAndHostStillRejectsMismatches() {
        String source = "{\"title\":\"Khám phá nghề nghiệp 😀\",\"status\":\"IN_PROGRESS\"}";
        var d =
                new edu.hcmute.careersim.guidance.agent.domain.DomainEvidence(
                        "EN:7",
                        "OWN_ENROLLMENT_PROGRESS",
                        "OWNER",
                        "1",
                        source,
                        CoreValues.tree(Map.of()));
        var policy = CoreValues.tree(Map.of("grounding", "GENERAL_ALLOWED", "maxCharacters", 4000));
        var schema =
                AgentResponseSchema.grounded(
                        "CHAT", policy, java.util.List.of(), java.util.List.of(d));
        var references =
                schema.path("properties")
                        .path("citations")
                        .path("items")
                        .path("properties")
                        .path("referenceId")
                        .path("enum");
        assertThat(references.size()).isEqualTo(1);
        assertThat(references.get(0).asText()).isEqualTo("EN:7");
        assertThat(schema.toString()).doesNotContain("anyOf");
        var options =
                AgentResponseSchema.citationOptions(java.util.List.of(), java.util.List.of(d));
        assertThat(options).isNotEmpty();
        for (var option : options) {
            String quote = option.get("quote");
            assertThat(source).contains(quote);
            var output =
                    CoreValues.tree(
                            Map.of(
                                    "schemaVersion",
                                    1,
                                    "outcome",
                                    "ANSWER",
                                    "text",
                                    "Try one small task.",
                                    "citations",
                                    java.util.List.of(
                                            Map.of("referenceId", "EN:7", "quote", quote)),
                                    "suggestions",
                                    java.util.List.of()));
            assertThat(
                            edu.hcmute.careersim.guidance.agent.domain.StudentOutputValidator
                                    .validate(
                                            output,
                                            "CHAT",
                                            java.util.List.of(),
                                            java.util.List.of(d),
                                            policy))
                    .isNotNull();
            ((com.fasterxml.jackson.databind.node.ObjectNode) output.path("citations").get(0))
                    .put("quote", "A fabricated paraphrase");
            assertThatThrownBy(
                            () ->
                                    edu.hcmute.careersim.guidance.agent.domain
                                            .StudentOutputValidator.validate(
                                            output,
                                            "CHAT",
                                            java.util.List.of(),
                                            java.util.List.of(d),
                                            policy))
                    .isInstanceOf(
                            edu.hcmute.careersim.guidance.service.SynGuidanceProvider
                                    .ProviderException.class);
        }
        assertThat(
                        AgentResponseSchema.grounded(
                                        "CHAT", policy, java.util.List.of(), java.util.List.of())
                                .path("properties")
                                .path("citations")
                                .path("maxItems")
                                .asInt())
                .isZero();
    }

    @Test
    void chatProjectsExistingClosedLimitsWithoutPrivateContent() {
        var schema =
                AgentResponseSchema.forTask("CHAT", CoreValues.tree(Map.of("maxCharacters", 1200)));
        assertThat(schema.path("properties").size()).isEqualTo(5);
        assertThat(schema.path("additionalProperties").asBoolean()).isFalse();
        assertThat(schema.path("properties").path("citations").path("maxItems").asInt())
                .isEqualTo(10);
        assertThat(schema.toString()).doesNotContain("studentId", "password", "assessmentResult");
    }

    @Test
    void studentChatAdvertisesOnlySupportedActionsWithoutChangingAdminChat() {
        var p = CoreValues.tree(Map.of());
        var student = AgentResponseSchema.forTask("STUDENT_CHAT", p).path("properties");
        assertThat(student.size()).isEqualTo(8);
        assertThat(student.path("requestedAction").path("enum").toString())
                .contains("NONE", "INSIGHT_REPORT", "PLAN_SUGGESTION")
                .doesNotContain("PUBLISH", "ASSIGN");
        assertThat(student.has("report")).isTrue();
        assertThat(student.has("planSuggestion")).isTrue();
        assertThat(AgentResponseSchema.forTask("CHAT", p).path("properties").size()).isEqualTo(5);
    }

    @Test
    void studentActionCannotBypassClosedArtifactsEvidenceOrRefusalRules() {
        var policy = CoreValues.tree(Map.of("grounding", "GENERAL_ALLOWED", "maxCharacters", 4000));
        var base =
                (com.fasterxml.jackson.databind.node.ObjectNode)
                        CoreValues.tree(
                                Map.of(
                                        "schemaVersion",
                                        1,
                                        "outcome",
                                        "INSUFFICIENT_EVIDENCE",
                                        "text",
                                        "Complete an assessment or share a relevant question first.",
                                        "citations",
                                        java.util.List.of(),
                                        "suggestions",
                                        java.util.List.of()));
        base.put("requestedAction", "INSIGHT_REPORT");
        assertThat(
                        edu.hcmute.careersim.guidance.agent.domain.StudentOutputValidator.validate(
                                        base,
                                        "CHAT",
                                        java.util.List.of(),
                                        java.util.List.of(),
                                        policy)
                                .has("report"))
                .isFalse();
        base.put("outcome", "ANSWER");
        assertThatThrownBy(
                        () ->
                                edu.hcmute.careersim.guidance.agent.domain.StudentOutputValidator
                                        .validate(
                                                base,
                                                "CHAT",
                                                java.util.List.of(),
                                                java.util.List.of(),
                                                policy))
                .isInstanceOf(
                        edu.hcmute.careersim.guidance.service.SynGuidanceProvider.ProviderException
                                .class);
        base.put("requestedAction", "NONE");
        base.set("report", CoreValues.tree(Map.of()));
        assertThatThrownBy(
                        () ->
                                edu.hcmute.careersim.guidance.agent.domain.StudentOutputValidator
                                        .validate(
                                                base,
                                                "CHAT",
                                                java.util.List.of(),
                                                java.util.List.of(),
                                                policy))
                .isInstanceOf(
                        edu.hcmute.careersim.guidance.service.SynGuidanceProvider.ProviderException
                                .class);
        base.remove("report");
        base.put("outcome", "REFUSED");
        base.put("requestedAction", "NONE");
        assertThat(
                        edu.hcmute.careersim.guidance.agent.domain.StudentOutputValidator.validate(
                                        base,
                                        "CHAT",
                                        java.util.List.of(),
                                        java.util.List.of(),
                                        policy)
                                .path("outcome")
                                .asText())
                .isEqualTo("REFUSED");
    }

    @Test
    void artifactSchemasMatchExistingReportAndDraftValidators() {
        var p = CoreValues.tree(Map.of());
        var report =
                AgentResponseSchema.forTask("INSIGHT_REPORT", p).path("properties").path("report");
        assertThat(report.path("properties").size()).isEqualTo(9);
        assertThat(
                        report.path("properties")
                                .path("strengthsPracticed")
                                .path("items")
                                .path("description")
                                .asText())
                .contains("[] if none");
        var plan =
                AgentResponseSchema.forTask("PLAN_SUGGESTION", p)
                        .path("properties")
                        .path("planSuggestion");
        assertThat(plan.path("properties").size()).isEqualTo(5);
        assertThat(plan.path("properties").path("steps").path("maxItems").asInt()).isEqualTo(5);
        assertThat(plan.toString()).contains("UNSAVED").doesNotContain("confirmed", "writeTool");
    }

    @Test
    void groupedWindowsDoNotSuggestMultipleCitationsForTheSameSourceAndDuplicatesStillFail() {
        var data =
                CoreValues.tree(
                        Map.of(
                                "title",
                                "Career result",
                                "status",
                                "SUBMITTED",
                                "resultData",
                                Map.of("categories", java.util.List.of(Map.of("raw", 13)))));
        String source = CoreValues.write(data);
        var d =
                new edu.hcmute.careersim.guidance.agent.domain.DomainEvidence(
                        "AR:7", "ASSESSMENT_RESULT", "OWNER", "1", source, data);
        var grouped =
                AgentResponseSchema.groupedCitationOptions(
                        java.util.List.of(), java.util.List.of(d));
        assertThat(grouped).hasSize(1);
        assertThat(grouped.get(0).get("referenceId")).isEqualTo("AR:7");
        var windows = (java.util.List<String>) grouped.get(0).get("quoteOptions");
        assertThat(windows).allSatisfy(q -> assertThat(source).contains(q));
        var cite = Map.of("referenceId", "AR:7", "quote", windows.get(0));
        var raw =
                CoreValues.tree(
                        Map.of(
                                "schemaVersion",
                                1,
                                "outcome",
                                "ANSWER",
                                "text",
                                "Your score is 13.",
                                "citations",
                                java.util.List.of(cite, cite),
                                "suggestions",
                                java.util.List.of()));
        var policy = CoreValues.tree(Map.of("grounding", "GENERAL_ALLOWED", "maxCharacters", 4000));
        assertThatThrownBy(
                        () ->
                                edu.hcmute.careersim.guidance.agent.domain.StudentOutputValidator
                                        .validate(
                                                raw,
                                                "CHAT",
                                                java.util.List.of(),
                                                java.util.List.of(d),
                                                policy))
                .isInstanceOf(
                        edu.hcmute.careersim.guidance.service.SynGuidanceProvider.ProviderException
                                .class)
                .satisfies(
                        e ->
                                assertThat(
                                                ((edu.hcmute.careersim.guidance.service
                                                                        .SynGuidanceProvider
                                                                        .ProviderException)
                                                                e)
                                                        .diagnostics()
                                                        .get("validationCheck"))
                                        .isEqualTo("DUPLICATE_CITATION"));
        ((com.fasterxml.jackson.databind.node.ObjectNode) raw)
                .set("citations", CoreValues.tree(java.util.List.of(cite)));
        assertThat(
                        edu.hcmute.careersim.guidance.agent.domain.StudentOutputValidator.validate(
                                raw, "CHAT", java.util.List.of(), java.util.List.of(d), policy))
                .isNotNull();
        ((com.fasterxml.jackson.databind.node.ObjectNode) raw).put("text", "Your score is 99.");
        assertThatThrownBy(
                        () ->
                                edu.hcmute.careersim.guidance.agent.domain.StudentOutputValidator
                                        .validate(
                                                raw,
                                                "CHAT",
                                                java.util.List.of(),
                                                java.util.List.of(d),
                                                policy))
                .isInstanceOf(RuntimeException.class);
    }
}
