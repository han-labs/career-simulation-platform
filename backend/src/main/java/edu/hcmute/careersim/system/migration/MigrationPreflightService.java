package edu.hcmute.careersim.system.migration;

import static edu.hcmute.careersim.system.migration.MigrationContracts.*;

import edu.hcmute.careersim.common.exception.*;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.identity.access.ActorAccess;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class MigrationPreflightService {
    private final MigrationRecordDao dao;
    private final ReviewedImportTarget targets;
    private final ActorAccess actors;
    private final TransactionTemplate tx;

    public MigrationPreflightService(
            MigrationRecordDao dao,
            ReviewedImportTarget targets,
            ActorAccess actors,
            PlatformTransactionManager tm) {
        this.dao = dao;
        this.targets = targets;
        this.actors = actors;
        tx = new TransactionTemplate(tm);
    }

    public Report run(Manifest manifest, boolean dryRun) {
        if (manifest == null
                || !dao.database().equals(manifest.expectedDatabase())
                || !"public".equals(manifest.expectedSchema())
                || !"public".equals(dao.schema()))
            throw new IllegalArgumentException("Intended database/schema must match exactly");
        if (!Set.of("20", "21").contains(dao.head()))
            throw new IllegalStateException(
                    "This release's reviewed import adapters require Core V20 or the additive V21 platform expansion");
        if (manifest.entries() == null || manifest.entries().size() > 100)
            throw new IllegalArgumentException("Batch limit is 100 reviewed records");
        var outcomes = new ArrayList<Outcome>();
        for (var e : manifest.entries())
            outcomes.add(
                    tx.execute(
                            s -> {
                                if (e.sourceHash() == null
                                        || !e.sourceHash().matches("[a-f0-9]{64}"))
                                    throw new IllegalArgumentException("Source hash required");
                                long reviewer = CoreValues.id(e.reviewerId());
                                ActorAccess.requireRole(actors.lock(reviewer), "ADMIN");
                                String current = dao.sourceHash(e);
                                var old = dao.record(e);
                                if (!current.equals(e.sourceHash())
                                        || (old.isPresent()
                                                && !current.equals(old.get().get("source_hash")))) {
                                    if (!dryRun) dao.conflict(e, "SOURCE_CHANGED");
                                    return new Outcome(
                                            e.sourceTable(),
                                            e.sourceKey(),
                                            "CONFLICT",
                                            "SOURCE_CHANGED");
                                }
                                targets.review(e);
                                if (old.isPresent() && old.get().get("target_key") != null) {
                                    if (!CoreValues.hash(e.reviewedMapping())
                                                    .equals(old.get().get("mapping_hash"))
                                            || !e.targetKind().equals(old.get().get("target_kind"))
                                            || !"MAPPED".equals(old.get().get("state"))) {
                                        if (!dryRun) dao.conflict(e, "MAPPING_CHANGED");
                                        return new Outcome(
                                                e.sourceTable(),
                                                e.sourceKey(),
                                                "CONFLICT",
                                                "MAPPING_CHANGED");
                                    }
                                    targets.validate(
                                            new Target(
                                                    (String) old.get().get("target_table"),
                                                    (String) old.get().get("target_key")),
                                            e);
                                    return new Outcome(
                                            e.sourceTable(), e.sourceKey(), "MAPPED", "UNCHANGED");
                                }
                                if (dryRun)
                                    return new Outcome(
                                            e.sourceTable(),
                                            e.sourceKey(),
                                            "PENDING",
                                            "REVIEWED_COPY_NOT_EXECUTED");
                                if (old.isPresent())
                                    throw new ApiException(
                                            ErrorCode.CONFLICT,
                                            "Existing conflict requires explicit review before retry.");
                                Target target = targets.copy(e);
                                targets.validate(target, e);
                                dao.mapped(e, target, reviewer);
                                return new Outcome(
                                        e.sourceTable(), e.sourceKey(), "MAPPED", "REVIEWED_COPY");
                            }));
        return new Report(
                dao.database(), dao.schema(), dao.counts(), dao.conflicts(), outcomes, dryRun);
    }
}
