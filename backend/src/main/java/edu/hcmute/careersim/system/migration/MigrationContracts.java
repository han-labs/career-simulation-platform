package edu.hcmute.careersim.system.migration;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;

public final class MigrationContracts {
    private MigrationContracts() {}

    public record Manifest(String expectedDatabase, String expectedSchema, List<Entry> entries) {}

    public record Entry(
            String sourceTable,
            String sourceKey,
            String sourceHash,
            String targetKind,
            String reviewerId,
            JsonNode reviewedMapping) {}

    public record Target(String table, String key) {}

    public record Outcome(String sourceTable, String sourceKey, String state, String reason) {}

    public record Report(
            String database,
            String schema,
            Map<String, Long> counts,
            Map<String, Long> conflicts,
            List<Outcome> outcomes,
            boolean dryRun) {}
}
