package edu.hcmute.careersim.simulation.service.impl;

import edu.hcmute.careersim.common.audit.AuditWriter;
import edu.hcmute.careersim.common.exception.*;
import edu.hcmute.careersim.common.storage.FileAssetService;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.enterprise.access.EnterpriseAccess;
import edu.hcmute.careersim.identity.access.ActorAccess;
import edu.hcmute.careersim.simulation.access.ScenarioCatalogAccess;
import edu.hcmute.careersim.simulation.dao.ScenarioDao;
import edu.hcmute.careersim.simulation.domain.DeliverableValidator;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.*;
import edu.hcmute.careersim.simulation.service.ScenarioAuthoringService;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScenarioAuthoringServiceImpl
        implements ScenarioAuthoringService, ScenarioCatalogAccess {
    @Transactional(readOnly = true)
    public String slug(long actor, long version) {
        var v = owned(actor, version, false);
        return dao.root(CoreValues.id(v.scenarioId()), false).slug();
    }

    private final ScenarioDao dao;
    private final edu.hcmute.careersim.simulation.dao.ScenarioReadDao readDao;
    private final edu.hcmute.careersim.simulation.domain.QuizTaskEvaluator quiz;
    private final ActorAccess actors;
    private final EnterpriseAccess enterprise;
    private final DeliverableValidator validator;
    private final FileAssetService files;
    private final AuditWriter audit;

    private Version owned(long actor, long version, boolean lock) {
        ActorAccess.requireRole(actors.requireId(actor), "ENTERPRISE");
        var membership = enterprise.mine(actor);
        enterprise.requireMember(actor, CoreValues.id(membership.enterpriseId()));
        var v = dao.ownedVersion(version, CoreValues.id(membership.enterpriseId()), false);
        if (lock) {
            dao.root(CoreValues.id(v.scenarioId()), true);
            return dao.version(version, true);
        }
        return v;
    }

    private void draft(Version v) {
        if (!"DRAFT".equals(v.status()))
            throw new ApiException(
                    ErrorCode.CONFLICT, "Published versions are immutable. Clone a new draft.");
    }

    @Transactional
    public Version create(long actor, Create r) {
        actors.lock(actor);
        var m = enterprise.mine(actor);
        enterprise.requireMember(actor, CoreValues.id(m.enterpriseId()));
        if (r.slug() == null
                || !r.slug().matches("[a-z0-9]+(?:-[a-z0-9]+)*")
                || r.slug().length() > 120) CoreValues.invalid("Invalid slug.");
        validate(actor, CoreValues.id(m.enterpriseId()), r.draft(), null, false);
        long root = dao.create(CoreValues.id(m.enterpriseId()), r.slug());
        long id = dao.version(root, r.draft().metadata());
        dao.replaceTasks(id, r.draft().tasks());
        audit.record(actor, "SCENARIO_DRAFT_CREATED", "scenario_version", id, Map.of());
        return dao.version(id, false);
    }

    @Transactional
    public Version cloneVersion(long actor, long scenario, Long source) {
        actors.lock(actor);
        var root = dao.root(scenario, false);
        enterprise.requireMember(actor, root.enterprise());
        dao.root(scenario, true);
        Long selected = source == null ? root.current() : source;
        if (selected == null)
            throw new ApiException(ErrorCode.CONFLICT, "Select an existing source version.");
        var v = owned(actor, selected, false);
        if (!v.scenarioId().equals(Long.toString(scenario)))
            throw new NotFoundException("Source version not found.");
        long id = dao.version(scenario, v.metadata());
        var tasks =
                v.tasks().stream()
                        .map(
                                t ->
                                        new Task(
                                                null,
                                                t.position(),
                                                t.title(),
                                                t.workplaceContext(),
                                                t.instructions(),
                                                t.required(),
                                                t.translations(),
                                                t.definitions().stream()
                                                        .map(
                                                                d ->
                                                                        new Definition(
                                                                                null,
                                                                                d.position(),
                                                                                d.name(),
                                                                                d.format(),
                                                                                d.required(),
                                                                                d
                                                                                        .validationConfig()))
                                                        .toList(),
                                                t.resources().stream()
                                                        .map(
                                                                r ->
                                                                        new Resource(
                                                                                null,
                                                                                r.position(),
                                                                                r.title(),
                                                                                r.fileAssetId(),
                                                                                r.externalUrl(),
                                                                                r.releasePolicy()))
                                                        .toList(),
                                                t.skillIds(),
                                                t.modelAnswer(),
                                                t.taskType() == null ? "PRACTICAL" : t.taskType(),
                                                t.rubric()))
                        .toList();
        dao.replaceTasks(id, tasks);
        return dao.version(id, false);
    }

    @Transactional(readOnly = true)
    public Version get(long actor, long version) {
        actors.requireId(actor);
        return owned(actor, version, false);
    }

    @Transactional
    public Version update(long actor, long version, long revision, Draft r) {
        actors.lock(actor);
        var v = owned(actor, version, true);
        draft(v);
        CoreValues.match(revision, v.revision());
        validate(actor, CoreValues.id(v.enterpriseId()), r, v, false);
        dao.metadata(version, r.metadata());
        dao.replaceTasks(version, r.tasks());
        audit.record(
                actor,
                "SCENARIO_DRAFT_UPDATED",
                "scenario_version",
                version,
                Map.of("revision", revision + 1));
        return dao.version(version, false);
    }

    @Transactional
    public Version publish(long actor, long version, long revision) {
        actors.lock(actor);
        var pre = owned(actor, version, false);
        enterprise.requirePublisher(actor, CoreValues.id(pre.enterpriseId()));
        var v = owned(actor, version, true);
        draft(v);
        CoreValues.match(revision, v.revision());
        var r = new Draft(v.metadata(), v.tasks());
        validate(actor, CoreValues.id(v.enterpriseId()), r, v, true);
        if (r.tasks().stream().anyMatch(t -> !t.required()))
            CoreValues.invalid(
                    "New ordered versions require all tasks; optional skip is not supported.");
        String hash =
                CoreValues.hash(
                        Map.of(
                                "draft",
                                r,
                                "policyVersion",
                                "ordered-review/1",
                                "scoringSchemaVersion",
                                "1"));
        dao.publish(version, CoreValues.id(v.scenarioId()), hash);
        audit.record(
                actor, "SCENARIO_PUBLISHED", "scenario_version", version, Map.of("hash", hash));
        return dao.version(version, false);
    }

    @Transactional
    public void availability(long actor, long scenario, long revision, String state) {
        actors.lock(actor);
        var root = dao.root(scenario, false);
        enterprise.requireMember(actor, root.enterprise());
        if (!Set.of("AVAILABLE", "ARCHIVED", "SUSPENDED").contains(state))
            CoreValues.invalid("Invalid availability.");
        if ("AVAILABLE".equals(state)) {
            enterprise.requirePublisher(actor, root.enterprise());
            if (root.current() == null) CoreValues.invalid("Published version required.");
        }
        CoreValues.match(revision, dao.root(scenario, true).revision());
        dao.availability(scenario, state);
        audit.record(
                actor,
                "SCENARIO_AVAILABILITY_CHANGED",
                "scenario",
                scenario,
                Map.of("status", state));
    }

    @Transactional(readOnly = true)
    public Classification classification(long actor, long scenario) {
        ActorAccess.requireRole(actors.requireId(actor), "ENTERPRISE");
        var root = dao.root(scenario, false);
        enterprise.requireMember(actor, root.enterprise());
        return new Classification(
                Long.toString(root.id()), root.industryCode(), root.revision());
    }

    @Transactional
    public Classification classify(
            long actor, long scenario, long revision, String industryCode) {
        ActorAccess.requireRole(actors.lock(actor), "ENTERPRISE");
        var root = dao.root(scenario, false);
        enterprise.requireMember(actor, root.enterprise());
        if (industryCode != null && !"IT".equals(industryCode))
            CoreValues.invalid("Invalid industry classification.");
        root = dao.root(scenario, true);
        CoreValues.match(revision, root.revision());
        dao.classification(scenario, industryCode);
        audit.record(
                actor,
                "SCENARIO_CLASSIFICATION_CHANGED",
                "scenario",
                scenario,
                industryCode == null ? Map.of() : Map.of("industryCode", industryCode));
        var updated = dao.root(scenario, false);
        return new Classification(
                Long.toString(updated.id()), updated.industryCode(), updated.revision());
    }

    @Transactional(readOnly = true)
    public List<Skill> skills(long actor) {
        actors.requireId(actor);
        return dao.skills();
    }

    @Transactional
    public String skill(long actor, SkillRequest r) {
        ActorAccess.requireRole(actors.lock(actor), "ADMIN");
        CoreValues.text(r.code(), 80);
        CoreValues.text(r.name(), 160);
        return dao.skill(r);
    }

    private void validate(long actor, long tenant, Draft r, Version previous, boolean publish) {
        if (r == null || r.metadata() == null || r.tasks() == null || r.tasks().size() > 100)
            CoreValues.invalid("Invalid draft.");
        var m = r.metadata();
        CoreValues.text(m.title(), 180);
        CoreValues.text(m.targetRole(), 120);
        CoreValues.text(m.careerTrack(), 60);
        CoreValues.text(m.summary(), 600);
        CoreValues.text(m.introduction(), 30000);
        CoreValues.text(m.learningObjectives(), 10000);
        if (m.estimatedMinutes() < 1
                || !Set.of("INTRODUCTORY", "INTERMEDIATE", "ADVANCED").contains(m.difficulty()))
            CoreValues.invalid("Invalid metadata.");
        translations(
                m.translations(),
                Set.of("title", "targetRole", "summary", "introduction", "learningObjectives"));
        CoreValues.object(
                m.provenance(),
                Set.of(
                        "schemaVersion",
                        "origin",
                        "sourceUrl",
                        "license",
                        "reviewedByUserId",
                        "reviewedAt",
                        "notes"));
        if (publish) {
            var p = m.provenance();
            if (!CoreValues.schemaOne(p)
                    || !Set.of("ORIGINAL", "LICENSED").contains(p.path("origin").asText())
                    || !Long.toString(actor).equals(p.path("reviewedByUserId").asText()))
                CoreValues.invalid("Publishing actor must explicitly review provenance.");
            CoreValues.text(p.path("license").asText(), 240);
            try {
                var time = java.time.Instant.parse(p.path("reviewedAt").asText());
                if (time.isAfter(java.time.Instant.now()))
                    CoreValues.invalid("Review cannot be in the future.");
            } catch (java.time.format.DateTimeParseException e) {
                CoreValues.invalid("Review timestamp required.");
            }
            if ("LICENSED".equals(p.path("origin").asText()))
                CoreValues.https(p.path("sourceUrl").asText());
            if (r.tasks().stream().filter(Objects::nonNull).noneMatch(Task::required))
                CoreValues.invalid("At least one required task is necessary.");
        }
        Set<Integer> positions = new HashSet<>();
        Set<String> taskIds = new HashSet<>();
        var vocabulary = dao.skills();
        for (var t : r.tasks()) {
            quiz.publication(t);
            if (t == null || t.position() < 1 || !positions.add(t.position()))
                CoreValues.invalid("Duplicate/invalid task position.");
            CoreValues.text(t.title(), 180);
            CoreValues.text(t.workplaceContext(), 10000);
            CoreValues.text(t.instructions(), 30000);
            translations(t.translations(), Set.of("title", "workplaceContext", "instructions"));
            Task old = null;
            if (t.id() != null) {
                if (!taskIds.add(t.id()) || previous == null)
                    CoreValues.invalid("Invalid existing task ID.");
                old =
                        previous.tasks().stream()
                                .filter(x -> x.id().equals(t.id()))
                                .findFirst()
                                .orElseThrow(
                                        () ->
                                                new ApiException(
                                                        ErrorCode.DOMAIN_VALIDATION,
                                                        "Foreign task ID."));
            }
            if (t.definitions() == null
                    || t.definitions().size() > 100
                    || t.resources() == null
                    || t.resources().size() > 100
                    || t.skillIds() == null
                    || t.skillIds().size() > 50) CoreValues.invalid("Invalid task children.");
            if (publish
                    && t.definitions().stream()
                            .filter(Objects::nonNull)
                            .noneMatch(Definition::required))
                CoreValues.invalid("Every task requires a required deliverable.");
            Set<Integer> dp = new HashSet<>();
            Set<String> di = new HashSet<>();
            for (var d : t.definitions()) {
                validator.definition(d);
                if (!dp.add(d.position())) CoreValues.invalid("Duplicate definition position.");
                if (d.id() != null
                        && (old == null
                                || !di.add(d.id())
                                || old.definitions().stream()
                                        .noneMatch(x -> x.id().equals(d.id()))))
                    CoreValues.invalid("Foreign definition ID.");
            }
            Set<Integer> rp = new HashSet<>();
            Set<String> ri = new HashSet<>();
            for (var resource : t.resources()) {
                if (resource == null) CoreValues.invalid("Resource required.");
                CoreValues.text(resource.title(), 180);
                if (resource.position() < 1
                        || !rp.add(resource.position())
                        || !Set.of("TASK_ACCESS", "AFTER_SUBMISSION")
                                .contains(resource.releasePolicy())
                        || (resource.fileAssetId() == null) == (resource.externalUrl() == null))
                    CoreValues.invalid("Invalid resource.");
                if (resource.id() != null
                        && (old == null
                                || !ri.add(resource.id())
                                || old.resources().stream()
                                        .noneMatch(x -> x.id().equals(resource.id()))))
                    CoreValues.invalid("Foreign resource ID.");
                if (resource.fileAssetId() != null)
                    files.requireReady(actor, CoreValues.id(resource.fileAssetId()), tenant);
                else {
                    CoreValues.https(resource.externalUrl());
                    if ("AFTER_SUBMISSION".equals(resource.releasePolicy()))
                        CoreValues.invalid("Answer attachments must use private assets.");
                }
            }
            if (new HashSet<>(t.skillIds()).size() != t.skillIds().size())
                CoreValues.invalid("Duplicate skill.");
            for (var id : t.skillIds())
                if (vocabulary.stream()
                        .noneMatch(s -> s.id().equals(id) && "ACTIVE".equals(s.status())))
                    CoreValues.invalid("Unknown active skill.");
            if (t.modelAnswer() != null) {
                CoreValues.text(t.modelAnswer().content(), 30000);
                translations(t.modelAnswer().translations(), Set.of("content"));
            }
        }
    }

    private void translations(com.fasterxml.jackson.databind.JsonNode n, Set<String> fields) {
        CoreValues.object(n, Set.of("en", "vi"));
        n.forEach(
                v -> {
                    CoreValues.object(v, fields);
                    v.forEach(
                            t -> {
                                if (!t.isTextual()) CoreValues.invalid("Translation must be text.");
                                CoreValues.text(t.asText(), 30000);
                            });
                });
    }

    @Transactional(readOnly = true)
    public CatalogPage search(CatalogQuery query) {
        return readDao.search(
                new CatalogQuery(
                        query.q(),
                        query.careerTrack(),
                        query.difficulty(),
                        query.sort(),
                        query.cursor(),
                        query.limit(),
                        industry(query.industry())));
    }

    @Transactional(readOnly = true)
    public List<Version> versions(long actor) {
        var membership = enterprise.mine(actor);
        enterprise.requireMember(actor, CoreValues.id(membership.enterpriseId()));
        return readDao.versions(CoreValues.id(membership.enterpriseId()));
    }

    @Transactional(readOnly = true)
    public List<ParticipationSummary> participation(long actor, long tenant) {
        enterprise.requireMember(actor, tenant);
        return readDao.participation(tenant);
    }

    @Transactional(readOnly = true)
    public List<Summary> list(Long after, int limit, String industry) {
        return dao.catalog(
                after, Math.max(1, Math.min(limit, 100)), industry(industry));
    }

    private String industry(String value) {
        String industry = value == null ? "" : value.strip();
        if (!industry.isEmpty() && !"IT".equals(industry))
            CoreValues.invalid("Invalid industry classification.");
        return industry;
    }

    @Transactional(readOnly = true)
    public Summary find(String slug) {
        long v =
                dao.bySlug(slug)
                        .orElseThrow(() -> new NotFoundException("Published scenario not found."));
        var version = dao.version(v, false);
        var s =
                dao.catalog(CoreValues.id(version.scenarioId()) - 1, 1).stream()
                        .filter(x -> x.slug().equals(slug))
                        .findFirst()
                        .orElseThrow(() -> new NotFoundException("Published scenario not found."));
        return new Summary(
                s.id(),
                s.slug(),
                s.scenarioVersionId(),
                s.enterpriseId(),
                s.enterpriseName(),
                s.industryCode(),
                s.metadata(),
                s.media(),
                version.tasks().stream()
                        .map(
                                t ->
                                        new TaskOutline(
                                                t.id(),
                                                t.position(),
                                                t.title(),
                                                t.workplaceContext(),
                                                t.required(),
                                                t.taskType()))
                        .toList(),
                dao.publicSkillNames(v));
    }
}
