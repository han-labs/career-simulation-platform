package edu.hcmute.careersim.system.migration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.hcmute.careersim.assessment.domain.RiasecV1Evaluator;
import edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts;
import edu.hcmute.careersim.assessment.service.*;
import edu.hcmute.careersim.common.exception.ApiException;
import edu.hcmute.careersim.common.storage.FileAssetService;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.enterprise.access.EnterpriseAccess;
import edu.hcmute.careersim.enterprise.service.*;
import edu.hcmute.careersim.guidance.dto.*;
import edu.hcmute.careersim.guidance.service.*;
import edu.hcmute.careersim.identity.access.*;
import edu.hcmute.careersim.identity.dto.IdentityContracts;
import edu.hcmute.careersim.identity.service.*;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.*;
import edu.hcmute.careersim.simulation.service.*;
import edu.hcmute.careersim.simulation.service.CompletionService;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "app.core-v2-enabled=true",
            "app.legacy-write-enabled=false",
            "app.ai.provider=disabled",
            "app.syn.cleanup-enabled=false",
            "server.servlet.session.cookie.secure=false"
        })
@AutoConfigureMockMvc
@Testcontainers
class CorePackageAIntegrationTest {
    @Test
    void orderedQuizZeroScoreRequiresRealSelfReviewAndFreeformCompletesWithoutGrading()
            throws Exception {
        var published = orderedQuizScenario();
        var e = enrollments.enroll(student, Long.parseLong(published.id()));
        long eid = Long.parseLong(e.id()),
                tid = Long.parseLong(e.tasks().get(0).id()),
                next = Long.parseLong(e.tasks().get(1).id());
        assertThat(e.progressionMode()).isEqualTo("ORDERED_SELF_REVIEW");
        assertThat(e.tasks().get(1).definitions()).isEmpty();
        assertThat(e.tasks().get(1).instructions()).isNull();
        assertThat(e.tasks().get(1).translations().path("vi").path("title").asText())
                .isEqualTo("Vietnamese task");
        assertThat(e.tasks().get(1).translations().toString()).doesNotContain("Restricted");
        assertThatThrownBy(() -> submissions.read(student, eid, next))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> submissions.save(student, eid, next, 0, new Items(List.of())))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> answers.readAnswer(student, eid, tid))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> submissions.selfReview(student, eid, tid))
                .isInstanceOf(ApiException.class);
        var payload =
                List.of(
                        new Item(
                                e.tasks().get(0).definitions().get(0).id(),
                                null,
                                null,
                                null,
                                CoreValues.tree(Map.of("choice", "B"))));
        var request = new Finalize(UUID.randomUUID(), payload);
        var result = submissions.finalizeTask(student, eid, tid, 0, request);
        assertThat(result.quizResult().path("percentage").asInt()).isZero();
        assertThat(result.submittedByUserId()).isEqualTo(Long.toString(student));
        assertThat(submissions.finalizeTask(student, eid, tid, 0, request)).isEqualTo(result);
        assertThat(enrollments.read(student, eid).status()).isEqualTo("IN_PROGRESS");
        assertThatThrownBy(() -> submissions.read(student, eid, next))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> submissions.selfReview(other, eid, tid))
                .isInstanceOf(edu.hcmute.careersim.common.exception.NotFoundException.class);
        assertThat(answers.readAnswer(student, eid, tid).answer().content())
                .isEqualTo("Reviewed model answer");
        var reviewed = parallel(() -> submissions.selfReview(student, eid, tid));
        assertThat(reviewed.get(0).reviewedAt())
                .isEqualTo(reviewed.get(1).reviewedAt())
                .isNotNull();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM task_self_review WHERE submission_id=?",
                                Integer.class,
                                Long.parseLong(result.submissionId())))
                .isEqualTo(1);
        var unlocked = enrollments.read(student, eid).tasks().get(1);
        assertThat(unlocked.translations().path("vi").path("instructions").asText())
                .isEqualTo("Restricted translated instructions");
        var finalPractical =
                submissions.finalizeTask(
                        student, eid, next, 0, new Finalize(UUID.randomUUID(), items(unlocked)));
        assertThat(finalPractical.quizResult()).isNull();
        assertThat(finalPractical.reviewedAt()).isNull();
        assertThat(enrollments.read(student, eid).status()).isEqualTo("COMPLETED");
        var completion = completions.read(student, Long.parseLong(finalPractical.completionId()));
        submissions.selfReview(student, eid, next);
        assertThat(completions.read(student, Long.parseLong(completion.id())))
                .isEqualTo(completion);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "UPDATE task_quiz_result SET score_data='{}' WHERE submission_id=?",
                                        Long.parseLong(result.submissionId())))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(
                        () -> jdbc.update("UPDATE task_rubric SET version=2 WHERE task_id=?", tid))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "DELETE FROM scenario_learning_policy WHERE scenario_version_id=?",
                                        Long.parseLong(published.id())))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    private Version orderedQuizScenario() {
        var old = scenario(2, false);
        var draft =
                scenarios.cloneVersion(
                        representative, Long.parseLong(old.scenarioId()), Long.parseLong(old.id()));
        var original = draft.tasks().get(0);
        var definition =
                new Definition(
                        null,
                        1,
                        "Quiz choices",
                        "JSON",
                        true,
                        CoreValues.tree(
                                Map.of(
                                        "schemaVersion",
                                        1,
                                        "jsonSchema",
                                        Map.of(
                                                "type",
                                                "object",
                                                "properties",
                                                Map.of(
                                                        "choice",
                                                        Map.of(
                                                                "type",
                                                                "string",
                                                                "enum",
                                                                List.of("A", "B"))),
                                                "required",
                                                List.of("choice"),
                                                "additionalProperties",
                                                false))));
        var quiz =
                new Task(
                        original.id(),
                        1,
                        original.title(),
                        original.workplaceContext(),
                        original.instructions(),
                        true,
                        original.translations(),
                        List.of(definition),
                        List.of(),
                        List.of(),
                        original.modelAnswer(),
                        "QUIZ",
                        new Rubric(
                                1,
                                "quiz-exact/1",
                                CoreValues.tree(
                                        Map.of(
                                                "schemaVersion",
                                                1,
                                                "definitionPosition",
                                                1,
                                                "answers",
                                                Map.of("choice", "A")))));
        var second = draft.tasks().get(1);
        var translatedSecond =
                new Task(
                        second.id(),
                        second.position(),
                        second.title(),
                        second.workplaceContext(),
                        second.instructions(),
                        true,
                        CoreValues.tree(
                                Map.of(
                                        "vi",
                                        Map.of(
                                                "title",
                                                "Vietnamese task",
                                                "instructions",
                                                "Restricted translated instructions",
                                                "workplaceContext",
                                                "Restricted translated context"))),
                        second.definitions(),
                        second.resources(),
                        second.skillIds(),
                        second.modelAnswer(),
                        second.taskType(),
                        second.rubric());
        draft =
                scenarios.update(
                        representative,
                        Long.parseLong(draft.id()),
                        draft.revision(),
                        new Draft(draft.metadata(), List.of(quiz, translatedSecond)));
        return scenarios.publish(representative, Long.parseLong(draft.id()), draft.revision());
    }

    @Test
    void independentAssessmentMethodsRetakesAndOptionalAssessmentEnrollment() {
        var riasec = assessment();
        var type =
                assessmentAuthor.createType(
                        admin,
                        new VersionedAssessmentContracts.TypeRequest(
                                "PROFILE_" + UUID.randomUUID(), "Synthetic self-report"));
        var config =
                CoreValues.tree(
                        Map.of(
                                "schemaVersion",
                                1,
                                "dimensions",
                                List.of("PEOPLE", "INFORMATION"),
                                "aggregation",
                                "SUM",
                                "minAnswer",
                                1,
                                "maxAnswer",
                                5,
                                "tieOrder",
                                List.of("PEOPLE", "INFORMATION")));
        var questions =
                List.of("PEOPLE", "INFORMATION").stream()
                        .map(
                                d ->
                                        new VersionedAssessmentContracts.QuestionDraft(
                                                d.equals("PEOPLE") ? 1 : 2,
                                                "Synthetic self-report: " + d,
                                                "LIKERT",
                                                CoreValues.tree(
                                                        Map.of(
                                                                "schemaVersion",
                                                                1,
                                                                "min",
                                                                1,
                                                                "max",
                                                                5,
                                                                "dimension",
                                                                d)),
                                                CoreValues.tree(Map.of()),
                                                true))
                        .toList();
        var profile =
                assessmentAuthor.createVersion(
                        admin,
                        Long.parseLong(type),
                        new VersionedAssessmentContracts.VersionDraft(
                                "profile-sum", "1", config, questions));
        profile = assessmentAuthor.publish(admin, Long.parseLong(profile.id()), profile.revision());
        var noAssessment = scenario(1, false);
        assertThat(enrollments.enroll(student, Long.parseLong(noAssessment.id())).id())
                .isNotBlank();
        for (var version : List.of(riasec, profile, profile)) {
            var attempt =
                    assessments.start(student, Long.parseLong(version.id()), UUID.randomUUID());
            assessments.submit(
                    student,
                    Long.parseLong(attempt.id()),
                    0,
                    new VersionedAssessmentContracts.Responses(responses(attempt)));
        }
        var results = assessmentResults.readOwned(student);
        assertThat(results).hasSize(3);
        assertThat(
                        results.stream()
                                .map(VersionedAssessmentContracts.Result::evaluatorKey)
                                .distinct()
                                .toList())
                .containsExactlyInAnyOrder("riasec-sum", "profile-sum");
        assertThat(results.stream().filter(r -> r.evaluatorKey().equals("profile-sum")).toList())
                .allSatisfy(
                        r -> {
                            assertThat(r.resultData().has("dimensions")).isFalse();
                            assertThat(r.resultData().path("categories")).hasSize(2);
                        });
    }

    @Autowired edu.hcmute.careersim.assessment.access.VersionedAssessmentAccess assessmentResults;
    @Autowired edu.hcmute.careersim.simulation.access.ScenarioCatalogAccess catalogue;

    @Test
    void filteredCatalogueUsesStableCursorAndEnterpriseStatisticsNeverReturnPrivateWork()
            throws Exception {
        String term = "Catalogue " + UUID.randomUUID();
        var template = scenario(1, false);
        var metadata = template.metadata();
        for (int i = 0; i < 3; i++) {
            var m =
                    new Metadata(
                            term,
                            metadata.targetRole(),
                            metadata.careerTrack(),
                            metadata.summary(),
                            metadata.introduction(),
                            metadata.learningObjectives(),
                            metadata.difficulty(),
                            10,
                            metadata.translations(),
                            metadata.provenance());
            var task = template.tasks().get(0);
            var copied =
                    new Task(
                            null,
                            1,
                            task.title(),
                            task.workplaceContext(),
                            task.instructions(),
                            true,
                            task.translations(),
                            List.of(
                                    new Definition(
                                            null,
                                            1,
                                            "Response",
                                            "TEXT",
                                            true,
                                            CoreValues.tree(
                                                    Map.of(
                                                            "schemaVersion",
                                                            1,
                                                            "minLength",
                                                            1,
                                                            "maxLength",
                                                            100)))),
                            List.of(),
                            List.of(),
                            task.modelAnswer());
            var v =
                    scenarios.create(
                            representative,
                            new Create(
                                    "catalogue-" + UUID.randomUUID(),
                                    new Draft(m, List.of(copied))));
            scenarios.publish(representative, Long.parseLong(v.id()), v.revision());
        }
        String cursor = null;
        Set<String> ids = new LinkedHashSet<>();
        do {
            var page =
                    catalogue.search(
                            new CatalogQuery(
                                    term,
                                    metadata.careerTrack(),
                                    "INTRODUCTORY",
                                    "TITLE_ASC",
                                    cursor,
                                    1));
            assertThat(page.items()).hasSize(1);
            assertThat(ids.add(page.items().get(0).id())).isTrue();
            cursor = page.nextCursor();
        } while (cursor != null);
        assertThat(ids).hasSize(3);
        var page = catalogue.search(new CatalogQuery(term, null, null, "DURATION_ASC", null, 1));
        assertThatThrownBy(
                        () ->
                                catalogue.search(
                                        new CatalogQuery(
                                                "changed",
                                                null,
                                                null,
                                                "DURATION_ASC",
                                                page.nextCursor(),
                                                1)))
                .isInstanceOf(ApiException.class);
        var detail =
                catalogue.find(
                        template.id() == null
                                ? "missing"
                                : scenarios.slug(representative, Long.parseLong(template.id())));
        assertThat(detail.taskOutline()).hasSize(1);
        assertThat(json.writeValueAsString(detail))
                .doesNotContain("modelAnswer", "quizScoringConfig", "Reviewed model answer");
        enrollments.enroll(student, Long.parseLong(template.id()));
        enrollments.enroll(other, Long.parseLong(template.id()));
        var summary =
                scenarios.participation(representative, tenant).stream()
                        .filter(s -> s.scenarioVersionId().equals(template.id()))
                        .findFirst()
                        .orElseThrow();
        assertThat(summary.participants()).isEqualTo(2);
        assertThat(summary.completions()).isZero();
        assertThat(json.writeValueAsString(summary))
                .doesNotContain("studentId", "email", "contentText");
        long foreign =
                Long.parseLong(
                        identity.register(
                                        new IdentityContracts.Registration(
                                                "foreign-" + UUID.randomUUID() + "@example.test",
                                                PASSWORD,
                                                "Foreign",
                                                "ENTERPRISE",
                                                "Foreign Enterprise"))
                                .id());
        assertThatThrownBy(() -> scenarios.participation(foreign, tenant))
                .isInstanceOf(edu.hcmute.careersim.common.exception.NotFoundException.class);
        assertThatThrownBy(() -> scenarios.get(foreign, Long.parseLong(template.id())))
                .isInstanceOf(edu.hcmute.careersim.common.exception.NotFoundException.class);
    }

    @Test
    void runtimeRoleHasDataAccessButCannotOwnSchema() throws Exception {
        jdbc.execute(
                "CREATE ROLE package_a_runtime NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT");
        jdbc.execute("GRANT USAGE ON SCHEMA public TO package_a_runtime");
        jdbc.execute(
                "GRANT SELECT,INSERT,UPDATE ON ALL TABLES IN SCHEMA public TO package_a_runtime");
        jdbc.execute("GRANT USAGE,SELECT ON ALL SEQUENCES IN SCHEMA public TO package_a_runtime");
        try (var connection = PG.createConnection("");
                var statement = connection.createStatement()) {
            statement.execute("SET ROLE package_a_runtime");
            try (var result =
                    statement.executeQuery("SELECT count(*) FROM public.assessment_type")) {
                result.next();
                assertThat(result.getLong(1)).isGreaterThanOrEqualTo(0);
            }
            assertThatThrownBy(
                            () ->
                                    statement.execute(
                                            "CREATE TABLE public.unapproved_runtime_ddl(id bigint)"))
                    .isInstanceOf(java.sql.SQLException.class)
                    .satisfies(
                            error ->
                                    assertThat(((java.sql.SQLException) error).getSQLState())
                                            .isEqualTo("42501"));
        }
    }

    @Test
    void interruptedReviewedCopyRollsBackTargetAndLedgerThenRetries() {
        long source =
                jdbc.queryForObject(
                        "INSERT INTO public.exploration_plans(student_id,title,steps) VALUES (?,'Reviewed history','[\"Practice\"]'::jsonb) RETURNING id",
                        Long.class,
                        student);
        var seed =
                new MigrationContracts.Entry(
                        "exploration_plans",
                        Long.toString(source),
                        null,
                        "PLAN_CONFIRMED",
                        Long.toString(admin),
                        null);
        String hash = importRecords.sourceHash(seed);
        var mapping =
                CoreValues.tree(
                        Map.of(
                                "actorId",
                                Long.toString(student),
                                "confirmation",
                                Map.of("title", "Reviewed history", "steps", List.of("Practice")),
                                "confirmationEvidence",
                                Map.of(
                                        "reference",
                                        "Synthetic confirmed-source fixture",
                                        "studentId",
                                        Long.toString(student),
                                        "confirmedAt",
                                        Instant.now()
                                                .minusSeconds(1)
                                                .truncatedTo(java.time.temporal.ChronoUnit.MICROS)
                                                .toString(),
                                        "sourceHash",
                                        hash)));
        var entry =
                new MigrationContracts.Entry(
                        seed.sourceTable(),
                        seed.sourceKey(),
                        hash,
                        seed.targetKind(),
                        seed.reviewerId(),
                        mapping);
        var manifest =
                new MigrationContracts.Manifest(importRecords.database(), "public", List.of(entry));
        String function = "fixture_import_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.execute(
                "CREATE FUNCTION public."
                        + function
                        + "() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'Synthetic importer interruption'; END $$");
        jdbc.execute(
                "CREATE TRIGGER "
                        + function
                        + " BEFORE INSERT ON public.migration_record FOR EACH ROW WHEN (NEW.source_table='exploration_plans' AND NEW.source_key='"
                        + source
                        + "') EXECUTE FUNCTION public."
                        + function
                        + "()");
        try {
            assertThatThrownBy(() -> imports.run(manifest, false))
                    .isInstanceOf(org.springframework.dao.DataAccessException.class);
            assertThat(planReads.list(student)).isEmpty();
            assertThat(importRecords.record(entry)).isEmpty();
        } finally {
            jdbc.execute("DROP TRIGGER " + function + " ON public.migration_record");
            jdbc.execute("DROP FUNCTION public." + function + "()");
        }
        assertThat(imports.run(manifest, false).outcomes().get(0).state()).isEqualTo("MAPPED");
        assertThat(imports.run(manifest, false).outcomes().get(0).reason()).isEqualTo("UNCHANGED");
        assertThat(planReads.list(student)).hasSize(1);
        var altered = mapping.deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) altered.path("confirmationEvidence"))
                .put("reference", "Different reviewed mapping");
        var changed =
                new MigrationContracts.Entry(
                        seed.sourceTable(),
                        seed.sourceKey(),
                        hash,
                        seed.targetKind(),
                        seed.reviewerId(),
                        altered);
        assertThat(
                        imports.run(
                                        new MigrationContracts.Manifest(
                                                importRecords.database(),
                                                "public",
                                                List.of(changed)),
                                        false)
                                .outcomes()
                                .get(0)
                                .reason())
                .isEqualTo("MAPPING_CHANGED");
        assertThat(planReads.list(student)).hasSize(1);
    }

    @Test
    void controllerRejectsForeignWorkMissingRevisionAndUnknownFields() throws Exception {
        var v = scenario(1, false);
        var e = enrollments.enroll(student, Long.parseLong(v.id()));
        var t = e.tasks().get(0);
        String route = "/v2/enrollments/" + e.id() + "/tasks/" + t.id() + "/submission";
        var principal =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        new AccountPrincipal(student, email), null, List.of());
        var foreign =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        new AccountPrincipal(other, "ignored@example.test"), null, List.of());
        mvc.perform(
                        get(route)
                                .with(
                                        org.springframework.security.test.web.servlet.request
                                                .SecurityMockMvcRequestPostProcessors
                                                .authentication(foreign)))
                .andExpect(status().isNotFound());
        mvc.perform(
                        put(route)
                                .with(
                                        org.springframework.security.test.web.servlet.request
                                                .SecurityMockMvcRequestPostProcessors
                                                .authentication(principal))
                                .with(csrf())
                                .contentType("application/json")
                                .content(json.writeValueAsString(new Items(items(t)))))
                .andExpect(status().isPreconditionRequired());
        mvc.perform(
                        put(route)
                                .with(
                                        org.springframework.security.test.web.servlet.request
                                                .SecurityMockMvcRequestPostProcessors
                                                .authentication(principal))
                                .with(csrf())
                                .header("If-Match", "0")
                                .contentType("application/json")
                                .content("{\"items\":[],\"studentId\":\"999\"}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(
                        put(route)
                                .with(
                                        org.springframework.security.test.web.servlet.request
                                                .SecurityMockMvcRequestPostProcessors
                                                .authentication(principal))
                                .with(csrf())
                                .header("If-Match", "W/\"0\"")
                                .contentType("application/json")
                                .content(json.writeValueAsString(new Items(items(t)))))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/v2/scenarios/" + scenarios.slug(representative, Long.parseLong(v.id()))))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "Reviewed model answer"))));
    }

    @Test
    void sameParentFksAndAllPublishedChildrenAreGuarded() {
        var av = assessment();
        var bv = assessment();
        var a = assessments.start(student, Long.parseLong(av.id()), UUID.randomUUID());
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO public.assessment_response(attempt_id,question_id,assessment_version_id,answer_data) VALUES (?,?,?,'{\"schemaVersion\":1,\"value\":3}'::jsonb)",
                                        Long.parseLong(a.id()),
                                        Long.parseLong(bv.questions().get(0).id()),
                                        Long.parseLong(av.id())))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        var v = scenario(1, false);
        long task = Long.parseLong(v.tasks().get(0).id());
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "UPDATE public.task_deliverable_definition SET name='tampered' WHERE task_id=?",
                                        task))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "UPDATE public.model_answer SET content='tampered' WHERE task_id=?",
                                        task))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO public.task_resource(task_id,position,title,external_url) VALUES (?,1,'tampered','https://example.test')",
                                        task))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        String skill =
                scenarios.skill(
                        admin, new SkillRequest("GUARD_" + UUID.randomUUID(), "Guard fixture"));
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO public.task_skill(task_id,skill_id,skill_name_snapshot) VALUES (?,?,'tampered')",
                                        task,
                                        Long.parseLong(skill)))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        var foreign = scenario(1, false);
        var e = enrollments.enroll(student, Long.parseLong(v.id()));
        var saved =
                submissions.save(
                        student,
                        Long.parseLong(e.id()),
                        task,
                        0,
                        new Items(items(e.tasks().get(0))));
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO public.submitted_deliverable(submission_id,definition_id,task_id,content_text) VALUES (?,?,?,'tampered')",
                                        Long.parseLong(saved.submissionId()),
                                        Long.parseLong(
                                                foreign.tasks().get(0).definitions().get(0).id()),
                                        task))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void parentLockSerializesDirectChildEditAndPublication() throws Exception {
        var original = assessment();
        var request =
                new VersionedAssessmentContracts.VersionDraft(
                        original.evaluatorKey(),
                        original.evaluatorVersion(),
                        original.scoringConfig(),
                        original.questions().stream()
                                .map(
                                        q ->
                                                new VersionedAssessmentContracts.QuestionDraft(
                                                        q.position(),
                                                        q.prompt(),
                                                        q.responseType(),
                                                        q.responseConfig(),
                                                        q.translations(),
                                                        q.required()))
                                .toList());
        var draft =
                assessmentAuthor.createVersion(admin, Long.parseLong(original.typeId()), request);
        var pool = Executors.newSingleThreadExecutor();
        var started = new CountDownLatch(1);
        try (var connection = PG.createConnection("")) {
            connection.setAutoCommit(false);
            try (var statement =
                    connection.prepareStatement(
                            "UPDATE public.assessment_question SET prompt='Reviewed edit before publication' WHERE id=?")) {
                statement.setLong(1, Long.parseLong(draft.questions().get(0).id()));
                statement.executeUpdate();
            }
            var publish =
                    pool.submit(
                            () -> {
                                started.countDown();
                                return assessmentAuthor.publish(
                                        admin, Long.parseLong(draft.id()), draft.revision());
                            });
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> publish.get(50, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            connection.commit();
            var published = publish.get(5, TimeUnit.SECONDS);
            assertThat(published.questions().get(0).prompt())
                    .isEqualTo("Reviewed edit before publication");
            assertThatThrownBy(
                            () ->
                                    jdbc.update(
                                            "UPDATE public.assessment_question SET prompt='late write' WHERE id=?",
                                            Long.parseLong(published.questions().get(0).id())))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void learningPolicyChildEditSerializesWithPublicationAndFreezesAfterward() throws Exception {
        var original = scenario(1, false);
        var draft =
                scenarios.cloneVersion(
                        representative,
                        Long.parseLong(original.scenarioId()),
                        Long.parseLong(original.id()));
        long task = Long.parseLong(draft.tasks().get(0).id());
        var pool = Executors.newSingleThreadExecutor();
        var started = new CountDownLatch(1);
        try (var connection = PG.createConnection("")) {
            connection.setAutoCommit(false);
            try (var statement =
                    connection.prepareStatement(
                            "UPDATE public.task_learning_policy SET task_type='TEXT' WHERE task_id=?")) {
                statement.setLong(1, task);
                assertThat(statement.executeUpdate()).isEqualTo(1);
            }
            var publish =
                    pool.submit(
                            () -> {
                                started.countDown();
                                return scenarios.publish(
                                        representative,
                                        Long.parseLong(draft.id()),
                                        draft.revision());
                            });
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> publish.get(50, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            connection.commit();
            assertThat(publish.get(5, TimeUnit.SECONDS).tasks().get(0).taskType())
                    .isEqualTo("TEXT");
            assertThatThrownBy(
                            () ->
                                    jdbc.update(
                                            "UPDATE public.task_learning_policy SET task_type='PRACTICAL' WHERE task_id=?",
                                            task))
                    .isInstanceOf(org.springframework.dao.DataAccessException.class);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void answerAttachmentsAreAssociationAuthorizedAndReleaseOnlyAfterFinal() {
        var original = scenario(1, false);
        var draft =
                scenarios.cloneVersion(
                        representative,
                        Long.parseLong(original.scenarioId()),
                        Long.parseLong(original.id()));
        var t = draft.tasks().get(0);
        var asset =
                files.upload(
                        representative,
                        "reviewed-answer.txt",
                        "text/plain",
                        "Synthetic reviewed answer".getBytes());
        var resource =
                new Resource(null, 1, "Answer attachment", asset.id(), null, "AFTER_SUBMISSION");
        var changed =
                new Task(
                        t.id(),
                        t.position(),
                        t.title(),
                        t.workplaceContext(),
                        t.instructions(),
                        true,
                        t.translations(),
                        t.definitions(),
                        List.of(resource),
                        t.skillIds(),
                        t.modelAnswer());
        draft =
                scenarios.update(
                        representative,
                        Long.parseLong(draft.id()),
                        draft.revision(),
                        new Draft(draft.metadata(), List.of(changed)));
        var v = scenarios.publish(representative, Long.parseLong(draft.id()), draft.revision());
        var e = enrollments.enroll(student, Long.parseLong(v.id()));
        long use = Long.parseLong(v.tasks().get(0).resources().get(0).id());
        assertThat(e.tasks().get(0).resources()).isEmpty();
        assertThatThrownBy(
                        () ->
                                files.download(
                                        student, Long.parseLong(asset.id()), "TASK_RESOURCE", use))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(
                        () ->
                                files.download(
                                        other, Long.parseLong(asset.id()), "TASK_RESOURCE", use))
                .isInstanceOf(ApiException.class);
        submissions.finalizeTask(
                student,
                Long.parseLong(e.id()),
                Long.parseLong(e.tasks().get(0).id()),
                0,
                new Finalize(UUID.randomUUID(), items(e.tasks().get(0))));
        assertThat(
                        files.download(student, Long.parseLong(asset.id()), "TASK_RESOURCE", use)
                                .bytes())
                .isEqualTo("Synthetic reviewed answer".getBytes());
        assertThatThrownBy(
                        () ->
                                files.download(
                                        other, Long.parseLong(asset.id()), "TASK_RESOURCE", use))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void multipleRepresentativesAndPurposeScopedSubmissionSharing() {
        long second =
                jdbc.queryForObject(
                        "INSERT INTO public.app_users(email,password_hash,display_name,role) VALUES (?,'operator-provisioned-fixture','Second representative','ENTERPRISE') RETURNING id",
                        Long.class,
                        "second-" + UUID.randomUUID() + "@example.test");
        enterprises.member(admin, tenant, second, 0, "ACTIVE", false);
        var v = scenario(1, false);
        assertThat(scenarios.get(second, Long.parseLong(v.id())).enterpriseId())
                .isEqualTo(Long.toString(tenant));
        assertThatThrownBy(() -> scenarios.publish(second, Long.parseLong(v.id()), v.revision()))
                .isInstanceOf(ApiException.class);
        var outsider =
                identity.register(
                        new IdentityContracts.Registration(
                                "outsider-" + UUID.randomUUID() + "@example.test",
                                PASSWORD,
                                "Other enterprise",
                                "ENTERPRISE",
                                "Other organization"));
        assertThatThrownBy(
                        () -> scenarios.get(Long.parseLong(outsider.id()), Long.parseLong(v.id())))
                .isInstanceOf(ApiException.class);
        var e = enrollments.enroll(student, Long.parseLong(v.id()));
        var t = e.tasks().get(0);
        var s =
                submissions.finalizeTask(
                        student,
                        Long.parseLong(e.id()),
                        Long.parseLong(t.id()),
                        0,
                        new Finalize(UUID.randomUUID(), items(t)));
        assertThatThrownBy(
                        () ->
                                sharedSimulation.readSubmission(
                                        second, Long.parseLong(s.submissionId())))
                .isInstanceOf(ApiException.class);
        String grant =
                consent.grant(
                        student,
                        new IdentityContracts.Grant(
                                "SUBMISSION_SHARE",
                                Long.toString(tenant),
                                "enterprise:" + tenant,
                                "careersim-purpose-v1",
                                true));
        assertThat(
                        sharedSimulation
                                .readSubmission(second, Long.parseLong(s.submissionId()))
                                .items())
                .hasSize(1);
        assertThatThrownBy(() -> sharedSimulation.readCompletions(second, student))
                .isInstanceOf(ApiException.class);
        consent.revoke(student, Long.parseLong(grant));
        assertThatThrownBy(
                        () ->
                                sharedSimulation.readSubmission(
                                        second, Long.parseLong(s.submissionId())))
                .isInstanceOf(ApiException.class);
    }

    @org.springframework.boot.test.web.server.LocalServerPort int port;

    @Test
    void realGatewayNoAiWorkflowAndIsolatedBackupRestore() throws Exception {
        var v = orderedQuizScenario();
        var av = assessment();
        org.testcontainers.Testcontainers.exposeHostPorts(port);
        String gateway =
                "server { listen 80; root /usr/share/nginx/html; location /api/ { proxy_pass http://host.testcontainers.internal:"
                        + port
                        + "/api/; proxy_set_header Host $http_host; } location / { try_files $uri /index.html; } }";
        Path bundle = Path.of("../frontend/dist").toAbsolutePath().normalize();
        assertThat(java.nio.file.Files.exists(bundle.resolve("index.html")))
                .as("Build frontend before gateway smoke")
                .isTrue();
        try (var nginx =
                new org.testcontainers.containers.GenericContainer<>("nginx:1.29-alpine")
                        .withExposedPorts(80)
                        .withCopyToContainer(
                                org.testcontainers.images.builder.Transferable.of(gateway),
                                "/etc/nginx/conf.d/default.conf")
                        .withCopyFileToContainer(
                                org.testcontainers.utility.MountableFile.forHostPath(bundle),
                                "/usr/share/nginx/html")) {
            nginx.start();
            String origin = "http://localhost:" + nginx.getMappedPort(80);
            var cookies = new java.net.CookieManager(null, java.net.CookiePolicy.ACCEPT_ALL);
            var http =
                    java.net.http.HttpClient.newBuilder()
                            .cookieHandler(cookies)
                            .connectTimeout(java.time.Duration.ofSeconds(5))
                            .build();
            var index =
                    http.send(
                            java.net.http.HttpRequest.newBuilder(
                                            java.net.URI.create(origin + "/assessment"))
                                    .build(),
                            java.net.http.HttpResponse.BodyHandlers.ofString());
            assertThat(index.statusCode()).isEqualTo(200);
            assertThat(index.body()).contains("<html");
            String base = origin + "/api";
            assertThat(
                            http.send(
                                            java.net.http.HttpRequest.newBuilder(
                                                            java.net.URI.create(base + "/health"))
                                                    .build(),
                                            java.net.http.HttpResponse.BodyHandlers.ofString())
                                    .statusCode())
                    .isEqualTo(200);
            gatewayRequest(
                    http,
                    base,
                    "POST",
                    "/v2/auth/login",
                    new IdentityContracts.Login(email, PASSWORD),
                    null);
            var attempt =
                    gatewayRequest(
                            http,
                            base,
                            "POST",
                            "/v2/assessment-versions/" + av.id() + "/attempts",
                            Map.of("requestId", UUID.randomUUID().toString()),
                            null);
            var read = assessments.get(student, Long.parseLong(attempt.path("id").asText()));
            gatewayRequest(
                    http,
                    base,
                    "POST",
                    "/v2/assessment-attempts/" + read.id() + "/submit",
                    new VersionedAssessmentContracts.Responses(responses(read)),
                    0L);
            var enrolled =
                    gatewayRequest(
                            http,
                            base,
                            "POST",
                            "/v2/scenario-versions/" + v.id() + "/enrollments",
                            Map.of(),
                            null);
            String eid = enrolled.path("id").asText();
            var task = v.tasks().get(0);
            String taskBase = "/v2/enrollments/" + eid + "/tasks/" + task.id();
            var draft =
                    gatewayRequest(
                            http,
                            base,
                            "PUT",
                            taskBase + "/submission",
                            new Items(
                                    List.of(
                                            new Item(
                                                    task.definitions().get(0).id(),
                                                    null,
                                                    null,
                                                    null,
                                                    CoreValues.tree(Map.of("choice", "B"))))),
                            0L);
            assertThat(
                            gatewayRequest(http, base, "GET", taskBase + "/submission", null, null)
                                    .path("items")
                                    .size())
                    .isEqualTo(1);
            var finalWork =
                    gatewayRequest(
                            http,
                            base,
                            "POST",
                            taskBase + "/submission/finalize",
                            new Finalize(
                                    UUID.randomUUID(),
                                    List.of(
                                            new Item(
                                                    task.definitions().get(0).id(),
                                                    null,
                                                    null,
                                                    null,
                                                    CoreValues.tree(Map.of("choice", "B"))))),
                            draft.path("revision").asLong());
            assertThat(finalWork.path("status").asText()).isEqualTo("FINAL");
            assertThat(
                            gatewayRequest(
                                            http,
                                            base,
                                            "GET",
                                            taskBase + "/model-answer",
                                            null,
                                            null)
                                    .path("answer")
                                    .path("translations")
                                    .path("vi")
                                    .path("content")
                                    .asText())
                    .isEqualTo("Đáp án mẫu");
            assertThat(finalWork.path("quizResult").path("percentage").asInt()).isZero();
            assertThat(finalWork.path("completionId").isNull()).isTrue();
            var waiting = gatewayRequest(http, base, "GET", "/v2/enrollments/" + eid, null, null);
            assertThat(waiting.path("progression").get(1).path("accessible").asBoolean()).isFalse();
            assertThat(waiting.path("tasks").get(1).path("translations").toString())
                    .doesNotContain("Restricted");
            gatewayRequest(http, base, "POST", taskBase + "/self-review", Map.of(), null);
            var unlocked = gatewayRequest(http, base, "GET", "/v2/enrollments/" + eid, null, null);
            assertThat(unlocked.path("progression").get(1).path("accessible").asBoolean()).isTrue();
            assertThat(unlocked.path("tasks").get(1).path("translations").toString())
                    .contains("Restricted translated instructions");
            var practical = v.tasks().get(1);
            var finalRequest = new Finalize(UUID.randomUUID(), items(practical));
            String practicalBase = "/v2/enrollments/" + eid + "/tasks/" + practical.id();
            var practicalFinal =
                    gatewayRequest(
                            http,
                            base,
                            "POST",
                            practicalBase + "/submission/finalize",
                            finalRequest,
                            0L);
            assertThat(practicalFinal.path("quizResult").isNull()).isTrue();
            assertThat(practicalFinal.path("reviewedAt").isNull()).isTrue();
            assertThat(
                            gatewayRequest(
                                    http,
                                    base,
                                    "POST",
                                    practicalBase + "/submission/finalize",
                                    finalRequest,
                                    0L))
                    .isEqualTo(practicalFinal);
            String cid = practicalFinal.path("completionId").asText();
            var completion =
                    gatewayRequest(http, base, "GET", "/v2/completions/" + cid, null, null);
            assertThat(completion.path("resumeSnippetText").asText())
                    .contains("not professional competence");
            assertThat(
                            gatewayRequest(http, base, "GET", "/v2/guidance/dashboard", null, null)
                                    .path("completions")
                                    .size())
                    .isEqualTo(1);
            var reply =
                    gatewayRequest(
                            http,
                            base,
                            "POST",
                            "/v1/guidance/syn/messages",
                            new SynMessageRequest(
                                    "Bắt đầu khám phá",
                                    "GET_STARTED",
                                    null,
                                    "VI",
                                    null,
                                    UUID.randomUUID()),
                            null);
            assertThat(reply.path("provenance").asText()).isEqualTo("STANDARD");
            assertThat(reply.path("responseLanguage").asText()).isEqualTo("VI");
            // Backup/restore is limited to this newly created Testcontainers database.
            assertThat(
                            PG.execInContainer(
                                            "pg_dump",
                                            "-U",
                                            PG.getUsername(),
                                            "-d",
                                            PG.getDatabaseName(),
                                            "-Fc",
                                            "-f",
                                            "/tmp/package-a.dump")
                                    .getExitCode())
                    .isZero();
            assertThat(
                            PG.execInContainer(
                                            "createdb",
                                            "-U",
                                            PG.getUsername(),
                                            "package_a_restored")
                                    .getExitCode())
                    .isZero();
            assertThat(
                            PG.execInContainer(
                                            "pg_restore",
                                            "-U",
                                            PG.getUsername(),
                                            "-d",
                                            "package_a_restored",
                                            "/tmp/package-a.dump")
                                    .getExitCode())
                    .isZero();
            String restoredUrl =
                    "jdbc:postgresql://"
                            + PG.getHost()
                            + ":"
                            + PG.getMappedPort(5432)
                            + "/package_a_restored";
            var ds =
                    new org.springframework.jdbc.datasource.DriverManagerDataSource(
                            restoredUrl, PG.getUsername(), PG.getPassword());
            var restored = new JdbcTemplate(ds);
            assertThat(
                            restored.queryForObject(
                                    "SELECT max(version::integer) FROM public.flyway_schema_history WHERE success",
                                    Integer.class))
                    .isEqualTo(20);
            assertThat(
                            restored.queryForObject(
                                    "SELECT resume_snippet_text FROM public.simulation_completion WHERE id=?",
                                    String.class,
                                    Long.parseLong(cid)))
                    .isEqualTo(completion.path("resumeSnippetText").asText());
            assertThat(
                            restored.queryForObject(
                                    "SELECT count(*) FROM public.task_submission WHERE enrollment_id=? AND status='FINAL'",
                                    Integer.class,
                                    Long.parseLong(eid)))
                    .isEqualTo(2);
            long next =
                    restored.queryForObject(
                            "INSERT INTO public.assessment_attempt(student_id,assessment_version_id,start_request_id) VALUES (?,?,?) RETURNING id",
                            Long.class,
                            student,
                            Long.parseLong(av.id()),
                            UUID.randomUUID());
            assertThat(next).isPositive();
            assertThat(
                            restored.queryForObject(
                                    "SELECT count(*) FROM public.assessment_attempt WHERE id=?",
                                    Integer.class,
                                    next))
                    .isEqualTo(1);
            assertThat(
                            restored.queryForObject(
                                    "SELECT count(*) FROM pg_constraint WHERE conname='fk_task_submission_task'",
                                    Integer.class))
                    .isEqualTo(1);
        }
    }

    com.fasterxml.jackson.databind.JsonNode gatewayRequest(
            java.net.http.HttpClient http,
            String base,
            String method,
            String path,
            Object body,
            Long revision)
            throws Exception {
        var request =
                java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + path))
                        .header("Origin", base.substring(0, base.length() - 4))
                        .timeout(java.time.Duration.ofSeconds(30));
        if (!method.equals("GET")) {
            var token =
                    http.send(
                            java.net.http.HttpRequest.newBuilder(
                                            java.net.URI.create(base + "/v2/auth/csrf"))
                                    .build(),
                            java.net.http.HttpResponse.BodyHandlers.ofString());
            request.header(
                    "X-CSRF-TOKEN",
                    json.readTree(token.body()).path("data").path("token").asText());
        }
        if (revision != null) request.header("If-Match", revision.toString());
        request.header("Content-Type", "application/json")
                .method(
                        method,
                        body == null
                                ? java.net.http.HttpRequest.BodyPublishers.noBody()
                                : java.net.http.HttpRequest.BodyPublishers.ofString(
                                        json.writeValueAsString(body)));
        var response =
                http.send(request.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode())
                .as(method + " " + path + " returned " + response.statusCode())
                .isEqualTo(200);
        return json.readTree(response.body()).path("data");
    }

    @Autowired PrivacyAdmission admission;
    @Autowired MigrationPreflightService imports;
    @Autowired MigrationRecordDao importRecords;

    @Test
    void everyDeliverableFormatAndPracticedEvidenceIsFinalOnly() {
        var published = scenario(2, false);
        var draft =
                scenarios.cloneVersion(
                        representative,
                        Long.parseLong(published.scenarioId()),
                        Long.parseLong(published.id()));
        var original = draft.tasks().get(0);
        String skill =
                scenarios.skill(
                        admin,
                        new SkillRequest("SQL_FIXTURE_" + UUID.randomUUID(), "SQL practice"));
        var definitions =
                List.of(
                        new Definition(
                                null,
                                1,
                                "Text",
                                "TEXT",
                                true,
                                CoreValues.tree(
                                        Map.of(
                                                "schemaVersion",
                                                1,
                                                "minLength",
                                                1,
                                                "maxLength",
                                                100))),
                        new Definition(
                                null,
                                2,
                                "Reference",
                                "URL",
                                true,
                                CoreValues.tree(
                                        Map.of(
                                                "schemaVersion",
                                                1,
                                                "schemes",
                                                List.of("https"),
                                                "maxLength",
                                                2000))),
                        new Definition(
                                null,
                                3,
                                "File",
                                "FILE",
                                true,
                                CoreValues.tree(
                                        Map.of(
                                                "schemaVersion",
                                                1,
                                                "maxBytes",
                                                1024,
                                                "allowedMimeTypes",
                                                List.of("text/plain")))),
                        new Definition(
                                null,
                                4,
                                "Structured",
                                "JSON",
                                true,
                                CoreValues.tree(
                                        Map.of(
                                                "schemaVersion",
                                                1,
                                                "jsonSchema",
                                                Map.of(
                                                        "type",
                                                        "object",
                                                        "properties",
                                                        Map.of(
                                                                "choice",
                                                                Map.of(
                                                                        "type",
                                                                        "string",
                                                                        "enum",
                                                                        List.of("A", "B"))),
                                                        "required",
                                                        List.of("choice"),
                                                        "additionalProperties",
                                                        false)))),
                        new Definition(
                                null,
                                5,
                                "Optional",
                                "TEXT",
                                false,
                                CoreValues.tree(
                                        Map.of(
                                                "schemaVersion",
                                                1,
                                                "minLength",
                                                1,
                                                "maxLength",
                                                100))));
        var changed =
                new Task(
                        original.id(),
                        original.position(),
                        original.title(),
                        original.workplaceContext(),
                        original.instructions(),
                        true,
                        original.translations(),
                        definitions,
                        List.of(),
                        List.of(skill),
                        original.modelAnswer());
        draft =
                scenarios.update(
                        representative,
                        Long.parseLong(draft.id()),
                        draft.revision(),
                        new Draft(draft.metadata(), List.of(changed, draft.tasks().get(1))));
        var v = scenarios.publish(representative, Long.parseLong(draft.id()), draft.revision());
        var e = enrollments.enroll(student, Long.parseLong(v.id()));
        var task = e.tasks().get(0);
        long eid = Long.parseLong(e.id()), tid = Long.parseLong(task.id());
        var asset = files.upload(student, "answer.txt", "text/plain", "Actual work".getBytes());
        var d = task.definitions();
        var valid =
                List.of(
                        new Item(d.get(0).id(), "Work", null, null, null),
                        new Item(d.get(1).id(), null, "https://example.test/reference", null, null),
                        new Item(d.get(2).id(), null, null, asset.id(), null),
                        new Item(
                                d.get(3).id(),
                                null,
                                null,
                                null,
                                CoreValues.tree(Map.of("choice", "A"))));
        assertThat(((Map<?, ?>) evidence.dashboard(student)).get("participation"))
                .isEqualTo(List.of());
        var saved = submissions.save(student, eid, tid, 0, new Items(valid));
        assertThat(((Map<?, ?>) evidence.dashboard(student)).get("participation"))
                .isEqualTo(List.of());
        var invalid = new ArrayList<>(valid);
        invalid.add(new Item(d.get(4).id(), "", null, null, null));
        assertThatThrownBy(
                        () ->
                                submissions.finalizeTask(
                                        student,
                                        eid,
                                        tid,
                                        saved.revision(),
                                        new Finalize(UUID.randomUUID(), invalid)))
                .isInstanceOf(ApiException.class);
        var wrongUrl = new ArrayList<>(valid);
        wrongUrl.set(1, new Item(d.get(1).id(), null, "http://example.test/unsafe", null, null));
        assertThatThrownBy(
                        () ->
                                submissions.finalizeTask(
                                        student,
                                        eid,
                                        tid,
                                        saved.revision(),
                                        new Finalize(UUID.randomUUID(), wrongUrl)))
                .isInstanceOf(ApiException.class);
        var wrongJson = new ArrayList<>(valid);
        wrongJson.set(
                3,
                new Item(d.get(3).id(), null, null, null, CoreValues.tree(Map.of("choice", "C"))));
        assertThatThrownBy(
                        () ->
                                submissions.finalizeTask(
                                        student,
                                        eid,
                                        tid,
                                        saved.revision(),
                                        new Finalize(UUID.randomUUID(), wrongJson)))
                .isInstanceOf(ApiException.class);
        var result =
                submissions.finalizeTask(
                        student,
                        eid,
                        tid,
                        saved.revision(),
                        new Finalize(UUID.randomUUID(), valid));
        assertThat(result.items()).hasSize(4);
        assertThat(enrollments.read(student, eid).status()).isEqualTo("IN_PROGRESS");
        assertThat(enrollments.read(student, eid).completionId()).isNull();
        var practiced =
                (List<Participation>)
                        ((Map<?, ?>) evidence.dashboard(student)).get("participation");
        assertThat(practiced).hasSize(1);
        assertThat(practiced.get(0).submissionId()).isEqualTo(result.submissionId());
        assertThat(practiced.get(0).skillNameSnapshot()).isEqualTo("SQL practice");
    }

    @Test
    void failuresBetweenWritesRollbackAssessmentAndCompletion() {
        var version = assessment();
        var a = assessments.start(student, Long.parseLong(version.id()), UUID.randomUUID());
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String function = "fixture_fail_" + suffix;
        jdbc.execute(
                "CREATE FUNCTION public."
                        + function
                        + "() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'Synthetic rollback checkpoint'; END $$");
        jdbc.execute(
                "CREATE TRIGGER "
                        + function
                        + " BEFORE INSERT ON public.assessment_result FOR EACH ROW WHEN (NEW.attempt_id="
                        + a.id()
                        + ") EXECUTE FUNCTION public."
                        + function
                        + "()");
        try {
            assertThatThrownBy(
                            () ->
                                    assessments.submit(
                                            student,
                                            Long.parseLong(a.id()),
                                            0,
                                            new VersionedAssessmentContracts.Responses(
                                                    responses(a))))
                    .isInstanceOf(org.springframework.dao.DataAccessException.class);
            assertThat(assessments.get(student, Long.parseLong(a.id())).responses()).isEmpty();
            assertThat(assessments.get(student, Long.parseLong(a.id())).revision()).isZero();
        } finally {
            jdbc.execute("DROP TRIGGER " + function + " ON public.assessment_result");
        }
        var v = scenario(1, false);
        var e = enrollments.enroll(student, Long.parseLong(v.id()));
        var task = e.tasks().get(0);
        long eid = Long.parseLong(e.id()), tid = Long.parseLong(task.id());
        jdbc.execute(
                "CREATE TRIGGER "
                        + function
                        + " BEFORE INSERT ON public.simulation_completion FOR EACH ROW WHEN (NEW.enrollment_id="
                        + eid
                        + ") EXECUTE FUNCTION public."
                        + function
                        + "()");
        try {
            assertThatThrownBy(
                            () ->
                                    submissions.finalizeTask(
                                            student,
                                            eid,
                                            tid,
                                            0,
                                            new Finalize(UUID.randomUUID(), items(task))))
                    .isInstanceOf(org.springframework.dao.DataAccessException.class);
            assertThat(submissions.read(student, eid, tid).submissionId()).isNull();
            assertThat(enrollments.read(student, eid).status()).isEqualTo("ENROLLED");
            assertThat(enrollments.read(student, eid).completionId()).isNull();
        } finally {
            jdbc.execute("DROP TRIGGER " + function + " ON public.simulation_completion");
            jdbc.execute("DROP FUNCTION public." + function + "()");
        }
        assertThat(
                        submissions
                                .finalizeTask(
                                        student,
                                        eid,
                                        tid,
                                        0,
                                        new Finalize(UUID.randomUUID(), items(task)))
                                .status())
                .isEqualTo("FINAL");
    }

    @Test
    void revocationAndDispatchAdmissionSerializeAndLateReplyCannotCommit() throws Exception {
        long grant =
                Long.parseLong(
                        consent.grant(
                                student,
                                new IdentityContracts.Grant(
                                        "EXTERNAL_AI",
                                        null,
                                        "provider:gemini",
                                        "careersim-purpose-v1",
                                        true)));
        long privacy = actors.requireId(student).privacyVersion();
        var turn =
                lifecycle.begin(
                        student,
                        new SynMessageRequest(
                                "Fixture", "GET_STARTED", null, "EN", null, UUID.randomUUID()));
        var admitted = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var revokeStarted = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        try {
            var dispatch =
                    pool.submit(
                            () -> {
                                try (var scope =
                                        SynExecutionScope.open(
                                                java.time.Duration.ofSeconds(20),
                                                () -> true,
                                                p ->
                                                        lifecycle.providerAllowed(
                                                                email, student, privacy, p),
                                                admission,
                                                student)) {
                                    return SynExecutionScope.admit(
                                            "gemini",
                                            () -> {
                                                calls.incrementAndGet();
                                                admitted.countDown();
                                                try {
                                                    if (!release.await(5, TimeUnit.SECONDS))
                                                        throw new IllegalStateException(
                                                                "Barrier timeout");
                                                } catch (InterruptedException ex) {
                                                    Thread.currentThread().interrupt();
                                                    throw new IllegalStateException(ex);
                                                }
                                                return "admitted";
                                            });
                                }
                            });
            assertThat(admitted.await(5, TimeUnit.SECONDS)).isTrue();
            var revoke =
                    pool.submit(
                            () -> {
                                revokeStarted.countDown();
                                consent.revoke(student, grant);
                            });
            assertThat(revokeStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> revoke.get(50, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            release.countDown();
            assertThat(dispatch.get(5, TimeUnit.SECONDS)).isEqualTo("admitted");
            revoke.get(5, TimeUnit.SECONDS);
            assertThat(calls.get()).isEqualTo(1);
            try (var scope =
                    SynExecutionScope.open(
                            java.time.Duration.ofSeconds(20),
                            () -> true,
                            p -> lifecycle.providerAllowed(email, student, privacy, p),
                            admission,
                            student)) {
                assertThatThrownBy(
                                () ->
                                        SynExecutionScope.admit(
                                                "gemini", () -> calls.incrementAndGet()))
                        .isInstanceOf(SynGuidanceProvider.ProviderException.class);
            }
            assertThat(calls.get()).isEqualTo(1);
            assertThat(lifecycle.get(student, turn.id()).state()).isEqualTo("CANCELLED");
            assertThat(lifecycle.get(student, turn.id()).response()).isNull();
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void reviewedImporterIsDryByDefaultResumableAndRejectsChangedSource() {
        long source =
                jdbc.queryForObject(
                        "INSERT INTO public.exploration_plans(student_id,title,steps) VALUES (?,'Verified old plan','[\"Practice\"]'::jsonb) RETURNING id",
                        Long.class,
                        student);
        var base =
                new MigrationContracts.Entry(
                        "exploration_plans",
                        Long.toString(source),
                        null,
                        "PLAN_CONFIRMED",
                        Long.toString(admin),
                        null);
        String hash = importRecords.sourceHash(base);
        String at =
                Instant.now()
                        .minusSeconds(1)
                        .truncatedTo(java.time.temporal.ChronoUnit.MICROS)
                        .toString();
        var mapping =
                CoreValues.tree(
                        Map.of(
                                "actorId",
                                Long.toString(student),
                                "confirmation",
                                Map.of("title", "Verified old plan", "steps", List.of("Practice")),
                                "confirmationEvidence",
                                Map.of(
                                        "reference",
                                        "Synthetic human-reviewed confirmation record",
                                        "studentId",
                                        Long.toString(student),
                                        "confirmedAt",
                                        at,
                                        "sourceHash",
                                        hash)));
        var entry =
                new MigrationContracts.Entry(
                        base.sourceTable(),
                        base.sourceKey(),
                        hash,
                        base.targetKind(),
                        base.reviewerId(),
                        mapping);
        var manifest =
                new MigrationContracts.Manifest(importRecords.database(), "public", List.of(entry));
        assertThat(imports.run(manifest, true).outcomes().get(0).state()).isEqualTo("PENDING");
        assertThat(planReads.list(student)).isEmpty();
        assertThat(imports.run(manifest, false).outcomes().get(0).state()).isEqualTo("MAPPED");
        assertThat(imports.run(manifest, false).outcomes().get(0).reason()).isEqualTo("UNCHANGED");
        assertThat(planReads.list(student)).hasSize(1);
        assertThat(planReads.current(student).orElseThrow().confirmedAt())
                .isEqualTo(Instant.parse(at));
        jdbc.update(
                "UPDATE public.exploration_plans SET title='Changed source' WHERE id=?", source);
        assertThat(imports.run(manifest, false).outcomes().get(0).state()).isEqualTo("CONFLICT");
        assertThat(planReads.current(student).orElseThrow().title()).isEqualTo("Verified old plan");
        assertThatThrownBy(
                        () ->
                                imports.run(
                                        new MigrationContracts.Manifest(
                                                "different-database", "public", List.of(entry)),
                                        false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Container
    static final PostgreSQLContainer<?> PG =
            new PostgreSQLContainer<>(
                    DockerImageName.parse("pgvector/pgvector:0.8.2-pg17")
                            .asCompatibleSubstituteFor("postgres"));

    @TempDir static Path storage;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
        r.add("app.storage.root", () -> storage.toString());
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired IdentityService identity;
    @Autowired ConsentService consent;
    @Autowired ConsentAccess grants;
    @Autowired ActorAccess actors;
    @Autowired EnterpriseService enterprises;
    @Autowired EnterpriseAccess memberships;
    @Autowired SharedProfileAccess sharing;
    @Autowired edu.hcmute.careersim.simulation.access.SharedSimulationAccess sharedSimulation;
    @Autowired AssessmentAuthoringService assessmentAuthor;
    @Autowired VersionedAssessmentService assessments;
    @Autowired ScenarioAuthoringService scenarios;
    @Autowired EnrollmentService enrollments;
    @Autowired TaskSubmissionService submissions;
    @Autowired ModelAnswerAccess answers;
    @Autowired CompletionService completions;
    @Autowired FileAssetService files;
    @Autowired PlanCommands plans;
    @Autowired ExplorationPlanService planReads;
    @Autowired GuidanceEvidenceQuery evidence;
    @Autowired GuidanceService syn;
    @Autowired SynTurnLifecycleService lifecycle;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    long admin, student, other, representative, tenant;
    String email;
    static final String PASSWORD = "synthetic-fixture-password-2026";

    @BeforeEach
    void fixture() {
        String suffix = UUID.randomUUID().toString();
        admin =
                jdbc.queryForObject(
                        "INSERT INTO public.app_users(email,password_hash,display_name,role) VALUES (?,'test-only','Fixture Admin','ADMINISTRATOR') RETURNING id",
                        Long.class,
                        "admin-" + suffix + "@example.test");
        email = "student-" + suffix + "@example.test";
        student =
                Long.parseLong(
                        identity.register(
                                        new IdentityContracts.Registration(
                                                email,
                                                PASSWORD,
                                                "Fixture Student",
                                                "STUDENT",
                                                null))
                                .id());
        other =
                Long.parseLong(
                        identity.register(
                                        new IdentityContracts.Registration(
                                                "other-" + suffix + "@example.test",
                                                PASSWORD,
                                                "Other Student",
                                                "STUDENT",
                                                null))
                                .id());
        representative =
                Long.parseLong(
                        identity.register(
                                        new IdentityContracts.Registration(
                                                "rep-" + suffix + "@example.test",
                                                PASSWORD,
                                                "Fixture Representative",
                                                "ENTERPRISE",
                                                "Synthetic content organization"))
                                .id());
        var m = memberships.mine(representative);
        tenant = Long.parseLong(m.enterpriseId());
        enterprises.verification(admin, tenant, 0, "VERIFIED");
        enterprises.publishing(admin, tenant, 1, "ENABLED");
        enterprises.member(admin, tenant, representative, 0, "ACTIVE", true);
    }

    VersionedAssessmentContracts.Version assessment() {
        var config =
                CoreValues.tree(
                        Map.of(
                                "schemaVersion",
                                1,
                                "dimensions",
                                RiasecV1Evaluator.DIMENSIONS,
                                "aggregation",
                                "SUM",
                                "minAnswer",
                                1,
                                "maxAnswer",
                                5,
                                "tieOrder",
                                RiasecV1Evaluator.DIMENSIONS));
        var questions = new ArrayList<VersionedAssessmentContracts.QuestionDraft>();
        int i = 0;
        for (String d : RiasecV1Evaluator.DIMENSIONS)
            questions.add(
                    new VersionedAssessmentContracts.QuestionDraft(
                            ++i,
                            "Synthetic interest " + d,
                            "LIKERT",
                            CoreValues.tree(
                                    Map.of("schemaVersion", 1, "min", 1, "max", 5, "dimension", d)),
                            CoreValues.tree(Map.of()),
                            true));
        String type =
                assessmentAuthor.createType(
                        admin,
                        new VersionedAssessmentContracts.TypeRequest(
                                "SYNTHETIC_" + UUID.randomUUID(), "Fixture assessment"));
        var v =
                assessmentAuthor.createVersion(
                        admin,
                        Long.parseLong(type),
                        new VersionedAssessmentContracts.VersionDraft(
                                "riasec-sum", "1", config, questions));
        return assessmentAuthor.publish(admin, Long.parseLong(v.id()), v.revision());
    }

    Version scenario(int required, boolean optional) {
        var metadata =
                new Metadata(
                        "Synthetic work",
                        "Developer",
                        "BACKEND_DEVELOPMENT",
                        "Fixture only",
                        "Synthetic introduction",
                        "Practice an original fixture task",
                        "INTRODUCTORY",
                        10,
                        CoreValues.tree(Map.of("vi", Map.of("title", "Công việc giả lập"))),
                        CoreValues.tree(
                                Map.of(
                                        "schemaVersion",
                                        1,
                                        "origin",
                                        "ORIGINAL",
                                        "license",
                                        "Original synthetic test fixture",
                                        "reviewedByUserId",
                                        Long.toString(representative),
                                        "reviewedAt",
                                        Instant.now().toString())));
        List<Task> tasks = new ArrayList<>();
        for (int i = 1; i <= required + (optional ? 1 : 0); i++)
            tasks.add(
                    new Task(
                            null,
                            i,
                            "Task " + i,
                            "Synthetic workplace",
                            "Write a response",
                            i <= required,
                            CoreValues.tree(Map.of()),
                            List.of(
                                    new Definition(
                                            null,
                                            1,
                                            "Response",
                                            "TEXT",
                                            true,
                                            CoreValues.tree(
                                                    Map.of(
                                                            "schemaVersion",
                                                            1,
                                                            "minLength",
                                                            1,
                                                            "maxLength",
                                                            10000)))),
                            List.of(),
                            List.of(),
                            new Answer(
                                    "Reviewed model answer",
                                    CoreValues.tree(
                                            Map.of("vi", Map.of("content", "Đáp án mẫu"))))));
        var v =
                scenarios.create(
                        representative,
                        new Create("fixture-" + UUID.randomUUID(), new Draft(metadata, tasks)));
        return scenarios.publish(representative, Long.parseLong(v.id()), v.revision());
    }

    List<Item> items(Task task) {
        return List.of(
                new Item(
                        task.definitions().get(0).id(), "Actual synthetic work", null, null, null));
    }

    List<VersionedAssessmentContracts.Answer> responses(VersionedAssessmentContracts.Attempt a) {
        return a.questions().stream()
                .map(
                        q ->
                                new VersionedAssessmentContracts.Answer(
                                        q.id(),
                                        CoreValues.tree(Map.of("schemaVersion", 1, "value", 3))))
                .toList();
    }

    <T> List<T> parallel(Callable<T> task) throws Exception {
        var gate = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            Callable<T> wrapped =
                    () -> {
                        gate.await();
                        return task.call();
                    };
            var a = pool.submit(wrapped);
            var b = pool.submit(wrapped);
            gate.countDown();
            return List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void sessionCsrfStatusAndLegacyDeprecation() throws Exception {
        mvc.perform(get("/v2/me")).andExpect(status().isUnauthorized());
        mvc.perform(
                        post("/v2/auth/login")
                                .contentType("application/json")
                                .content(
                                        json.writeValueAsString(
                                                new IdentityContracts.Login(email, PASSWORD))))
                .andExpect(status().isForbidden());
        var initial = mvc.perform(get("/v2/auth/csrf")).andExpect(status().isOk()).andReturn();
        var session = (MockHttpSession) initial.getRequest().getSession(false);
        String oldId = session.getId();
        String token =
                json.readTree(initial.getResponse().getContentAsString())
                        .path("data")
                        .path("token")
                        .asText();
        var login =
                mvc.perform(
                                post("/v2/auth/login")
                                        .session(session)
                                        .header("X-CSRF-TOKEN", token)
                                        .contentType("application/json")
                                        .content(
                                                json.writeValueAsString(
                                                        new IdentityContracts.Login(
                                                                email, PASSWORD))))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.id").value(Long.toString(student)))
                        .andReturn();
        var credential = login.getResponse().getCookie("CAREERSIM_AUTH");
        assertThat(credential).isNotNull();
        assertThat(session.getId()).isNotEqualTo(oldId);
        mvc.perform(get("/v2/me").session(session)).andExpect(status().isUnauthorized());
        mvc.perform(get("/v2/me").session(session).cookie(credential)).andExpect(status().isOk());
        mvc.perform(
                        post("/v1/assessments/attempts")
                                .session(session)
                                .cookie(credential)
                                .with(csrf())
                                .header("X-Student-Id", other))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/v1/assessments/attempts")
                                .session(session)
                                .cookie(credential)
                                .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORKFLOW_UPGRADE_REQUIRED"));
        mvc.perform(
                        post("/v2/auth/logout")
                                .session(session)
                                .cookie(credential)
                                .header("X-CSRF-TOKEN", token))
                .andExpect(status().isForbidden());
        identity.status(admin, student, actors.requireId(student).revision(), "LOCKED");
        mvc.perform(get("/v2/me").session(session).cookie(credential))
                .andExpect(status().isForbidden());
    }

    @Test
    void assessmentPinsVersionRetakesAndRejectsBadInputs() {
        var v = assessment();
        var request = UUID.randomUUID();
        var a = assessments.start(student, Long.parseLong(v.id()), request);
        assertThat(assessments.start(student, Long.parseLong(v.id()), request).id())
                .isEqualTo(a.id());
        assertThat(assessments.start(student, Long.parseLong(v.id()), UUID.randomUUID()).id())
                .isNotEqualTo(a.id());
        var otherVersion = assessment();
        assertThatThrownBy(
                        () ->
                                assessments.start(
                                        student, Long.parseLong(otherVersion.id()), request))
                .isInstanceOf(ApiException.class);
        var valid = responses(a);
        var duplicate = new ArrayList<>(valid);
        duplicate.set(1, valid.get(0));
        assertThatThrownBy(
                        () ->
                                assessments.submit(
                                        student,
                                        Long.parseLong(a.id()),
                                        0,
                                        new VersionedAssessmentContracts.Responses(duplicate)))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(
                        () ->
                                assessments.submit(
                                        student,
                                        Long.parseLong(a.id()),
                                        0,
                                        new VersionedAssessmentContracts.Responses(
                                                valid.subList(0, 5))))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> assessments.get(other, Long.parseLong(a.id())))
                .isInstanceOf(ApiException.class);
        var result =
                assessments.submit(
                        student,
                        Long.parseLong(a.id()),
                        0,
                        new VersionedAssessmentContracts.Responses(valid));
        assertThat(result.resultData().path("topDimensions"))
                .isEqualTo(CoreValues.tree(List.of("R", "I", "A")));
        assertThat(
                        assessments.submit(
                                student,
                                Long.parseLong(a.id()),
                                0,
                                new VersionedAssessmentContracts.Responses(valid)))
                .isEqualTo(result);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "UPDATE public.assessment_question SET prompt='tampered' WHERE id=?",
                                        Long.parseLong(a.questions().get(0).id())))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "DELETE FROM public.assessment_result WHERE attempt_id=?",
                                        Long.parseLong(a.id())))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void parallelAssessmentSubmitCreatesOneResult() throws Exception {
        var v = assessment();
        var a = assessments.start(student, Long.parseLong(v.id()), UUID.randomUUID());
        var r = new VersionedAssessmentContracts.Responses(responses(a));
        var result = parallel(() -> assessments.submit(student, Long.parseLong(a.id()), 0, r));
        assertThat(result.get(0)).isEqualTo(result.get(1));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM public.assessment_result WHERE attempt_id=?",
                                Integer.class,
                                Long.parseLong(a.id())))
                .isEqualTo(1);
    }

    @Test
    void parallelVersionsEnrollmentFinalizationAndFixedCompletion() throws Exception {
        var v = scenario(2, false);
        var enrollment = parallel(() -> enrollments.enroll(student, Long.parseLong(v.id())));
        assertThat(enrollment.get(0).id()).isEqualTo(enrollment.get(1).id());
        var e = enrollment.get(0);
        long eid = Long.parseLong(e.id());
        assertThat(e.tasks()).allMatch(t -> t.modelAnswer() == null);
        assertThatThrownBy(
                        () ->
                                answers.readAnswer(
                                        student, eid, Long.parseLong(e.tasks().get(0).id())))
                .isInstanceOf(ApiException.class);
        var clone =
                scenarios.cloneVersion(
                        representative, Long.parseLong(v.scenarioId()), Long.parseLong(v.id()));
        scenarios.publish(representative, Long.parseLong(clone.id()), clone.revision());
        assertThat(enrollments.enroll(student, Long.parseLong(clone.id())).id())
                .isNotEqualTo(e.id());
        assertThat(enrollments.read(student, eid).scenarioVersionId()).isEqualTo(v.id());
        assertThatThrownBy(
                        () -> submissions.read(student, eid, Long.parseLong(e.tasks().get(1).id())))
                .isInstanceOf(ApiException.class);
        for (var task : v.tasks()) {
            var request = new Finalize(UUID.randomUUID(), items(task));
            var finals =
                    parallel(
                            () ->
                                    submissions.finalizeTask(
                                            student, eid, Long.parseLong(task.id()), 0, request));
            assertThat(finals.get(0)).isEqualTo(finals.get(1));
            submissions.selfReview(student, eid, Long.parseLong(task.id()));
        }
        var finished = enrollments.read(student, eid);
        assertThat(finished.status()).isEqualTo("COMPLETED");
        var c = completions.read(student, Long.parseLong(finished.completionId()));
        assertThat(c.resumeSnippetText()).contains("not professional competence");
        assertThat(
                        answers.readAnswer(student, eid, Long.parseLong(e.tasks().get(0).id()))
                                .answer()
                                .content())
                .isEqualTo("Reviewed model answer");
        jdbc.update("UPDATE public.app_users SET display_name='Changed' WHERE id=?", student);
        submissions.selfReview(student, eid, Long.parseLong(e.tasks().get(1).id()));
        assertThat(completions.read(student, Long.parseLong(c.id()))).isEqualTo(c);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "UPDATE public.simulation_completion SET resume_snippet_text='changed' WHERE id=?",
                                        Long.parseLong(c.id())))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void finalRetryIsStableAndPayloadChildrenFreeze() {
        var v = scenario(1, false);
        var e = enrollments.enroll(student, Long.parseLong(v.id()));
        var task = e.tasks().get(0);
        long eid = Long.parseLong(e.id()), tid = Long.parseLong(task.id());
        var request = new Finalize(UUID.randomUUID(), items(task));
        var first = submissions.finalizeTask(student, eid, tid, 0, request);
        assertThat(submissions.finalizeTask(student, eid, tid, 0, request)).isEqualTo(first);
        assertThatThrownBy(
                        () ->
                                submissions.finalizeTask(
                                        student,
                                        eid,
                                        tid,
                                        first.revision(),
                                        new Finalize(UUID.randomUUID(), items(task))))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(
                        () ->
                                submissions.save(
                                        student,
                                        eid,
                                        tid,
                                        first.revision(),
                                        new Items(items(task))))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "UPDATE public.submitted_deliverable SET content_text='changed' WHERE submission_id=?",
                                        Long.parseLong(first.submissionId())))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> submissions.read(other, eid, tid))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void draftValidationAndVersionFks() {
        var v = scenario(1, false);
        var e = enrollments.enroll(student, Long.parseLong(v.id()));
        var t = e.tasks().get(0);
        long eid = Long.parseLong(e.id()), tid = Long.parseLong(t.id());
        assertThatThrownBy(
                        () ->
                                submissions.finalizeTask(
                                        student,
                                        eid,
                                        tid,
                                        0,
                                        new Finalize(UUID.randomUUID(), List.of())))
                .isInstanceOf(ApiException.class);
        assertThat(submissions.read(student, eid, tid).submissionId()).isNull();
        var s = submissions.save(student, eid, tid, 0, new Items(items(t)));
        assertThat(s.status()).isEqualTo("DRAFT");
        assertThatThrownBy(() -> submissions.save(student, eid, tid, 0, new Items(items(t))))
                .isInstanceOf(ApiException.class);
        var foreign = scenario(1, false);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO public.task_submission(enrollment_id,task_id,scenario_version_id) VALUES (?,?,?)",
                                        eid,
                                        Long.parseLong(foreign.tasks().get(0).id()),
                                        Long.parseLong(v.id())))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "UPDATE public.simulation_task SET instructions='changed' WHERE id=?",
                                        tid))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void consentIsExplicitIndependentAndLegacyTimestampIsNotAuthority() {
        jdbc.update(
                "UPDATE public.student_profiles SET consented_to_ai_at=CURRENT_TIMESTAMP WHERE user_id=?",
                student);
        assertThat(grants.effective(student, "EXTERNAL_AI", "provider:gemini")).isFalse();
        assertThatThrownBy(
                        () ->
                                consent.grant(
                                        student,
                                        new IdentityContracts.Grant(
                                                "EXTERNAL_AI",
                                                null,
                                                "provider:gemini",
                                                "careersim-purpose-v1",
                                                false)))
                .isInstanceOf(ApiException.class);
        String id =
                consent.grant(
                        student,
                        new IdentityContracts.Grant(
                                "EXTERNAL_AI",
                                null,
                                "provider:gemini",
                                "careersim-purpose-v1",
                                true));
        assertThat(grants.effective(student, "EXTERNAL_AI", "provider:gemini")).isTrue();
        assertThat(grants.effective(student, "EXTERNAL_AI", "provider:openai")).isFalse();
        assertThatThrownBy(() -> sharing.readShared(representative, student))
                .isInstanceOf(ApiException.class);
        consent.grant(
                student,
                new IdentityContracts.Grant(
                        "PROFILE_SHARE",
                        Long.toString(tenant),
                        "enterprise:" + tenant,
                        "careersim-purpose-v1",
                        true));
        assertThat(sharing.readShared(representative, student)).isNotNull();
        assertThatThrownBy(() -> sharedSimulation.readCompletions(representative, student))
                .isInstanceOf(ApiException.class);
        consent.revoke(student, Long.parseLong(id));
        assertThat(grants.effective(student, "EXTERNAL_AI", "provider:gemini")).isFalse();
    }

    @Test
    void plansArchiveAndSynFallbackRemainAuthorized() {
        var first =
                plans.confirm(
                        student,
                        new PlanContracts.Confirm(null, null, "First", List.of("Practice")));
        var next =
                plans.confirm(
                        student,
                        new PlanContracts.Confirm(
                                first.id(), first.revision(), "Second", List.of("Reflect")));
        assertThat(planReads.list(student)).hasSize(2);
        assertThatThrownBy(
                        () ->
                                plans.confirm(
                                        student,
                                        new PlanContracts.Confirm(
                                                first.id(),
                                                first.revision(),
                                                "Stale",
                                                List.of("Do"))))
                .isInstanceOf(ApiException.class);
        assertThat(next.status()).isEqualTo("ACTIVE");
        var reply =
                syn.sendMessage(
                        email,
                        new SynMessageRequest(
                                "Start exploring",
                                "GET_STARTED",
                                null,
                                "VI",
                                null,
                                UUID.randomUUID()));
        assertThat(reply.provenance()).isEqualTo("STANDARD");
        assertThat(reply.responseLanguage()).isEqualTo("VI");
        assertThat(syn.getTurn(email, UUID.fromString(reply.id())).state()).isEqualTo("COMPLETED");
    }

    @Test
    void fileOwnershipAndTypeChecks() {
        var file =
                files.upload(
                        student,
                        "work.txt",
                        "text/plain",
                        "Synthetic text".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(files.requireReady(student, Long.parseLong(file.id()), null)).isEqualTo(file);
        assertThatThrownBy(() -> files.requireReady(other, Long.parseLong(file.id()), null))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(
                        () ->
                                files.upload(
                                        student,
                                        "bad.pdf",
                                        "application/pdf",
                                        "not pdf".getBytes()))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(
                        () ->
                                files.download(
                                        student,
                                        Long.parseLong(file.id()),
                                        "TASK_RESOURCE",
                                        999999))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void enterpriseRegistrationAndPublisherAreSeparate() {
        var v = scenario(1, false);
        enterprises.member(
                admin,
                tenant,
                representative,
                memberships.mine(representative).membershipRevision(),
                "ACTIVE",
                false);
        assertThatThrownBy(
                        () ->
                                scenarios.publish(
                                        representative, Long.parseLong(v.id()), v.revision()))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> scenarios.get(student, Long.parseLong(v.id())))
                .isInstanceOf(ApiException.class);
    }
}
