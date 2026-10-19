package edu.hcmute.careersim.simulation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.hcmute.careersim.catalog.dao.CareerSimulationDao;
import edu.hcmute.careersim.catalog.entity.CareerSimulation;
import edu.hcmute.careersim.catalog.enumeration.SimulationStatus;
import edu.hcmute.careersim.common.exception.ApiException;
import edu.hcmute.careersim.identity.access.ActorAccess;
import edu.hcmute.careersim.simulation.dao.*;
import edu.hcmute.careersim.simulation.dto.SubmitAttemptRequest;
import edu.hcmute.careersim.simulation.entity.*;
import edu.hcmute.careersim.simulation.service.impl.SimulationAttemptServiceImpl;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

class LegacySimulationAuthorizationTest {
    ActorAccess actors = mock(ActorAccess.class);
    CareerSimulationDao simulations = mock(CareerSimulationDao.class);
    SimulationTaskDao tasks = mock(SimulationTaskDao.class);
    SimulationAttemptDao attempts = mock(SimulationAttemptDao.class);
    TaskSubmissionDao submissions = mock(TaskSubmissionDao.class);
    EvaluationResultDao results = mock(EvaluationResultDao.class);
    SimulationAttemptServiceImpl service =
            new SimulationAttemptServiceImpl(
                    actors, simulations, tasks, attempts, submissions, results, new ObjectMapper());

    @BeforeEach
    void fixture() {
        when(actors.requireId(7))
                .thenReturn(
                        new ActorAccess.Actor(
                                7, "fixture@example.test", "Fixture", "STUDENT", 0, 0));
        ReflectionTestUtils.setField(service, "legacyWrites", true);
        var simulation = mock(CareerSimulation.class);
        when(simulation.getId()).thenReturn(10L);
        when(simulations.findBySlugAndStatus("selected", SimulationStatus.PUBLISHED))
                .thenReturn(Optional.of(simulation));
        var first = mock(SimulationTask.class);
        when(first.getId()).thenReturn(11L);
        when(first.getEvaluationRule()).thenReturn("{\"correctOption\":\"A\"}");
        var second = mock(SimulationTask.class);
        when(second.getId()).thenReturn(12L);
        when(second.getEvaluationRule()).thenReturn("{\"correctOption\":\"B\"}");
        when(tasks.findBySimulationIdOrderByDisplayOrderAsc(10L))
                .thenReturn(List.of(first, second));
    }

    @Test
    void rejectsForeignAndUnknownTaskIdsBeforeAnyEvidenceWrites() {
        for (long foreign : List.of(21L, 999999L))
            assertThatThrownBy(
                            () ->
                                    service.submitAttempt(
                                            "selected",
                                            7L,
                                            new SubmitAttemptRequest(Map.of(foreign, "A"))))
                    .isInstanceOf(ApiException.class);
        verifyNoInteractions(attempts, submissions, results);
    }

    @Test
    void retainsAuthorizedPartialLegacyScoringWithoutInventingMissingAnswers() {
        var attempt = mock(SimulationAttempt.class);
        when(attempt.getId()).thenReturn(501L);
        when(attempts.save(any())).thenReturn(attempt);
        var result =
                service.submitAttempt("selected", 7L, new SubmitAttemptRequest(Map.of(11L, "A")));
        assertThat(result.correctCount()).isEqualTo(1);
        assertThat(result.totalTasks()).isEqualTo(2);
        verify(submissions)
                .saveAll(
                        argThat(
                                items -> {
                                    var saved = new ArrayList<TaskSubmission>();
                                    items.forEach(saved::add);
                                    return saved.size() == 1
                                            && saved.get(0).getTaskId().equals(11L);
                                }));
    }

    @Test
    void disabledWriterCannotBeBypassedAtServiceBoundary() {
        ReflectionTestUtils.setField(service, "legacyWrites", false);
        assertThatThrownBy(
                        () ->
                                service.submitAttempt(
                                        "selected", 7L, new SubmitAttemptRequest(Map.of(11L, "A"))))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("/api/v2");
        verifyNoInteractions(attempts, submissions, results);
    }
}
