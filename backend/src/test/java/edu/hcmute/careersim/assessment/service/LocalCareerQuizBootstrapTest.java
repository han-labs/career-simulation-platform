package edu.hcmute.careersim.assessment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.hcmute.careersim.assessment.dao.*;
import edu.hcmute.careersim.assessment.domain.RiasecV1Evaluator;
import edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts.*;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.identity.dao.IdentityDao;
import edu.hcmute.careersim.identity.dto.IdentityContracts.*;
import edu.hcmute.careersim.identity.service.IdentityService;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.transaction.support.TransactionTemplate;

/** Explicit operator-invoked local fixture. Never packaged in the application JAR. */
@SpringBootTest(
        properties = {
            "app.core-v2-enabled=true",
            "app.legacy-write-enabled=false",
            "app.ai.provider=disabled",
            "app.syn.cleanup-enabled=false",
            "app.auth.jwt.allow-ephemeral-key=true"
        })
@ActiveProfiles("local-us03")
@EnabledIfEnvironmentVariable(named = "RUN_LOCAL_US03_BOOTSTRAP", matches = "true")
class LocalCareerQuizBootstrapTest {
    @DynamicPropertySource
    static void localDatabase(DynamicPropertyRegistry r) {
        String url = System.getenv("LOCAL_US03_JDBC_URL");
        LocalQuizBootstrapGuard.require(
                url,
                System.getenv("LOCAL_US03_PROFILE"),
                System.getenv("RUN_LOCAL_US03_BOOTSTRAP"));
        r.add("spring.datasource.url", () -> url);
        r.add("spring.datasource.username", () -> System.getenv("LOCAL_US03_DB_USER"));
        r.add("spring.datasource.password", () -> System.getenv("LOCAL_US03_DB_PASSWORD"));
        r.add("spring.flyway.user", () -> System.getenv("LOCAL_US03_DB_USER"));
        r.add("spring.flyway.password", () -> System.getenv("LOCAL_US03_DB_PASSWORD"));
    }

    @Autowired AssessmentAuthoringService authoring;
    @Autowired RiasecQuestionRepository riasec;
    @Autowired CareerQuizQuestionRepository profiles;
    @Autowired IdentityDao identities;
    @Autowired IdentityService identity;
    @Autowired TransactionTemplate transaction;
    @Autowired JdbcTemplate jdbc;

    @Test
    void bootstrapReviewedLocalContent() throws Exception {
        Map<String, String> translations =
                new ObjectMapper()
                        .readValue(
                                Files.readString(Path.of(System.getenv("LOCAL_US03_TRANSLATIONS"))),
                                new TypeReference<>() {});
        String password = System.getenv("LOCAL_US03_STUDENT_PASSWORD");
        if (password == null || password.length() < 20)
            throw new IllegalStateException("Generated local Student password required.");
        transaction.executeWithoutResult(
                status -> {
                    jdbc.execute(
                            "SELECT pg_advisory_xact_lock(hashtext('careersim-local-us03-bootstrap'))");
                    String adminEmail = "us03-bootstrap@local.example.test",
                            studentEmail = "us03-student@local.example.test";
                    var admin =
                            identities
                                    .credential(adminEmail)
                                    .orElseGet(
                                            () -> {
                                                identities.register(
                                                        new Registration(
                                                                adminEmail,
                                                                null,
                                                                "Local US-03 non-login publisher",
                                                                "ADMINISTRATOR",
                                                                null),
                                                        null);
                                                return identities
                                                        .credential(adminEmail)
                                                        .orElseThrow();
                                            });
                    if (!"ADMINISTRATOR".equals(admin.role())
                            || !"ACTIVE".equals(admin.status())
                            || admin.hash() != null)
                        throw new IllegalStateException(
                                "Synthetic publisher identity collision; no account changed.");
                    if (identities.credential(studentEmail).isEmpty())
                        identity.register(
                                new Registration(
                                        studentEmail,
                                        password,
                                        "Local US-03 Student",
                                        "STUDENT",
                                        null));
                    var student = identity.authenticate(new Login(studentEmail, password));
                    if (!"STUDENT".equals(identities.credential(studentEmail).orElseThrow().role()))
                        throw new IllegalStateException("Student identity collision.");
                    String otherEmail = "us03-other@local.example.test";
                    if (identities.credential(otherEmail).isEmpty())
                        identity.register(
                                new Registration(
                                        otherEmail,
                                        password,
                                        "Local US-03 ownership fixture",
                                        "STUDENT",
                                        null));
                    identity.authenticate(new Login(otherEmail, password));
                    if (!"STUDENT".equals(identities.credential(otherEmail).orElseThrow().role()))
                        throw new IllegalStateException("Ownership fixture identity collision.");
                    for (String code :
                            List.of("RIASEC", "INTERESTS", "ABILITIES", "WORK_ACTIVITIES")) {
                        var questions = new ArrayList<QuestionDraft>();
                        if (code.equals("RIASEC"))
                            riasec.findAllByActiveTrueOrderByDisplayOrderAsc()
                                    .forEach(
                                            q ->
                                                    questions.add(
                                                            question(
                                                                    q.getDisplayOrder(),
                                                                    q.getPrompt(),
                                                                    q.getDimension(),
                                                                    translations)));
                        else
                            profiles.findAllByQuizTypeAndActiveTrueOrderByDisplayOrderAsc(code)
                                    .forEach(
                                            q ->
                                                    questions.add(
                                                            question(
                                                                    q.getDisplayOrder(),
                                                                    q.getPrompt(),
                                                                    q.getCategory(),
                                                                    translations)));
                        var dimensions =
                                switch (code) {
                                    case "RIASEC" -> RiasecV1Evaluator.DIMENSIONS;
                                    case "INTERESTS" ->
                                            List.of(
                                                    "ANALYTICAL",
                                                    "CREATIVE",
                                                    "COLLABORATIVE",
                                                    "ORGANIZING");
                                    case "ABILITIES" ->
                                            List.of(
                                                    "PROBLEM_SOLVING",
                                                    "COMMUNICATION",
                                                    "ORGANIZATION",
                                                    "TECHNICAL_LEARNING");
                                    default -> List.of("INFORMATION", "PEOPLE", "THINGS");
                                };
                        int perCategory = code.equals("RIASEC") ? 7 : 3;
                        if (questions.size() != dimensions.size() * perCategory
                                || dimensions.stream()
                                        .anyMatch(
                                                d ->
                                                        questions.stream()
                                                                        .filter(
                                                                                q ->
                                                                                        d.equals(
                                                                                                q.responseConfig()
                                                                                                        .path(
                                                                                                                "dimension")
                                                                                                        .asText()))
                                                                        .count()
                                                                != perCategory))
                            throw new IllegalStateException(
                                    "Legacy question/category inventory changed: " + code);
                        var draft =
                                new VersionDraft(
                                        code.equals("RIASEC") ? "riasec-sum" : "profile-sum",
                                        "1",
                                        CoreValues.tree(
                                                Map.of(
                                                        "schemaVersion",
                                                        1,
                                                        "dimensions",
                                                        dimensions,
                                                        "aggregation",
                                                        "SUM",
                                                        "minAnswer",
                                                        1,
                                                        "maxAnswer",
                                                        5,
                                                        "tieOrder",
                                                        dimensions)),
                                        questions);
                        var type =
                                authoring.administrationTypes(admin.id()).stream()
                                        .filter(t -> t.code().equals(code))
                                        .findFirst();
                        long typeId =
                                type.isPresent()
                                        ? Long.parseLong(type.get().id())
                                        : Long.parseLong(
                                                authoring.createType(
                                                        admin.id(),
                                                        new TypeRequest(
                                                                code,
                                                                switch (code) {
                                                                    case "RIASEC" -> "RIASEC Test";
                                                                    case "INTERESTS" ->
                                                                            "Interests Quiz";
                                                                    case "ABILITIES" ->
                                                                            "Abilities Quiz";
                                                                    default ->
                                                                            "Work Activities Test";
                                                                })));
                        var versions = authoring.versions(admin.id(), typeId);
                        if (versions.isEmpty()) {
                            var v = authoring.createVersion(admin.id(), typeId, draft);
                            authoring.publish(admin.id(), Long.parseLong(v.id()), v.revision());
                        } else if (versions.size() != 1
                                || !"PUBLISHED".equals(versions.get(0).status())
                                || !CoreValues.hash(draft).equals(versions.get(0).hash())
                                || !versions.get(0)
                                        .id()
                                        .equals(type.orElseThrow().activeVersionId()))
                            throw new IllegalStateException(
                                    "Existing content differs; refusing to alter versions: "
                                            + code);
                    }
                    System.out.println(
                            "Local US-03 bootstrap validated four published versions and synthetic Student; no credentials logged.");
                });
    }

    private QuestionDraft question(
            int position, String prompt, String dimension, Map<String, String> translations) {
        String vi = translations.get(prompt);
        if (vi == null || vi.isBlank())
            throw new IllegalStateException("Reviewed Vietnamese translation missing: " + position);
        return new QuestionDraft(
                position,
                prompt,
                "LIKERT",
                CoreValues.tree(
                        Map.of("schemaVersion", 1, "min", 1, "max", 5, "dimension", dimension)),
                CoreValues.tree(Map.of("en", Map.of("prompt", prompt), "vi", Map.of("prompt", vi))),
                true);
    }
}
