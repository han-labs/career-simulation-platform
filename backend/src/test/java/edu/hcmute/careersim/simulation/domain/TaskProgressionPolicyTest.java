package edu.hcmute.careersim.simulation.domain;

import static org.assertj.core.api.Assertions.*;

import edu.hcmute.careersim.simulation.dto.ScenarioContracts.TaskProgress;
import java.util.List;
import org.junit.jupiter.api.Test;

class TaskProgressionPolicyTest {
    @Test
    void finalWithoutReviewStillLocksEverySuccessor() {
        var tasks =
                List.of(
                        new TaskProgress("1", true, true, false),
                        new TaskProgress("2", true, false, false),
                        new TaskProgress("3", true, true, true));
        assertThat(TaskProgressionPolicy.project(tasks, true))
                .extracting(TaskProgress::accessible)
                .containsExactly(true, false, false);
    }

    @Test
    void explicitReviewAdmitsOnlyNextAndLegacyKeepsArbitraryTraversal() {
        var tasks =
                List.of(
                        new TaskProgress("1", true, true, true),
                        new TaskProgress("2", true, false, false),
                        new TaskProgress("3", true, false, false));
        assertThat(TaskProgressionPolicy.project(tasks, true))
                .extracting(TaskProgress::accessible)
                .containsExactly(true, true, false);
        assertThat(TaskProgressionPolicy.project(tasks, false)).allMatch(TaskProgress::accessible);
    }
}
