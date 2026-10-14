package edu.hcmute.careersim.system.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.migration.manifest")
public class LegacyBackfillCommand implements ApplicationRunner {
    private final String file;
    private final boolean dry;
    private final ObjectMapper json;
    private final MigrationPreflightService migration;

    public LegacyBackfillCommand(
            @Value("${app.migration.manifest}") String file,
            @Value("${app.migration.dry-run:true}") boolean dry,
            ObjectMapper json,
            MigrationPreflightService migration) {
        this.file = file;
        this.dry = dry;
        this.json = json;
        this.migration = migration;
    }

    public void run(ApplicationArguments args) throws Exception {
        var path = Path.of(file);
        if (Files.size(path) > 2000000) throw new IllegalArgumentException("Manifest too large");
        var manifest = json.readValue(Files.readString(path), MigrationContracts.Manifest.class);
        var report = migration.run(manifest, dry);
        org.slf4j.LoggerFactory.getLogger(getClass())
                .info(
                        "Reviewed migration dryRun={} outcomes={} conflicts={}",
                        dry,
                        report.outcomes().size(),
                        report.conflicts());
    }
}
