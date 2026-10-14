package edu.hcmute.careersim.system.migration;

import edu.hcmute.careersim.system.migration.MigrationContracts.*;

public interface ReviewedImportTarget {
    void review(Entry entry);

    Target copy(Entry entry);

    void validate(Target target, Entry entry);
}
