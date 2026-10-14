package edu.hcmute.careersim.system.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class MigrationInventoryTest {
    @Test
    void allVersionedClasspathMigrationsHaveUniqueFlywayVersions() throws Exception {
        var resources =
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath*:db/migration/V*__*.sql");
        var names = new TreeMap<MigrationVersion, ArrayList<String>>();
        var pattern = Pattern.compile("^V(.+)__.+\\.sql$");
        for (var resource : resources) {
            var match = pattern.matcher(resource.getFilename());
            assertThat(match.matches()).as("Versioned migration filename").isTrue();
            names.computeIfAbsent(
                            MigrationVersion.fromVersion(match.group(1)),
                            ignored -> new ArrayList<>())
                    .add(resource.getFilename());
        }
        assertThat(names).isNotEmpty();
        assertThat(
                        names.entrySet().stream()
                                .filter(entry -> entry.getValue().size() > 1)
                                .map(Map.Entry::getValue)
                                .toList())
                .as("Duplicate Flyway versions")
                .isEmpty();
    }
}
