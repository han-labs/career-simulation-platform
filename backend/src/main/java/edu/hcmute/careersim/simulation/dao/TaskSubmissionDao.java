// Provides persistence operations for submitted simulation task answers.
package edu.hcmute.careersim.simulation.dao;

import edu.hcmute.careersim.simulation.entity.TaskSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskSubmissionDao extends JpaRepository<TaskSubmission, Long> {}
