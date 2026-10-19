package edu.hcmute.careersim.simulation.domain;

import edu.hcmute.careersim.simulation.dto.ScenarioContracts.TaskProgress;
import java.util.*;

/** Pure policy: FINAL and explicit review admit the next ordered task; score is irrelevant. */
public final class TaskProgressionPolicy {
    private TaskProgressionPolicy() {}

    public static List<TaskProgress> project(List<TaskProgress> tasks, boolean ordered) {
        boolean available = true;
        var result = new ArrayList<TaskProgress>();
        for (var task : tasks) {
            result.add(
                    new TaskProgress(
                            task.taskId(),
                            !ordered || available,
                            task.finalized(),
                            task.reviewed()));
            available = available && task.finalized() && task.reviewed();
        }
        return List.copyOf(result);
    }
}
