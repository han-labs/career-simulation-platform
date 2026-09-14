// Evaluates submitted simulation answers server-side and persists the result.
package edu.hcmute.careersim.simulation.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.hcmute.careersim.catalog.dao.CareerSimulationDao;
import edu.hcmute.careersim.catalog.enumeration.SimulationStatus;
import edu.hcmute.careersim.common.exception.ApiException;
import edu.hcmute.careersim.common.exception.ErrorCode;
import edu.hcmute.careersim.common.exception.NotFoundException;
import edu.hcmute.careersim.simulation.dao.EvaluationResultDao;
import edu.hcmute.careersim.simulation.dao.SimulationAttemptDao;
import edu.hcmute.careersim.simulation.dao.SimulationTaskDao;
import edu.hcmute.careersim.simulation.dao.TaskSubmissionDao;
import edu.hcmute.careersim.simulation.dto.SubmitAttemptRequest;
import edu.hcmute.careersim.simulation.dto.SubmitAttemptResponse;
import edu.hcmute.careersim.simulation.dto.TaskOutcomeDto;
import edu.hcmute.careersim.simulation.entity.EvaluationResult;
import edu.hcmute.careersim.simulation.entity.SimulationAttempt;
import edu.hcmute.careersim.simulation.entity.SimulationTask;
import edu.hcmute.careersim.simulation.entity.TaskSubmission;
import edu.hcmute.careersim.simulation.service.SimulationAttemptService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationAttemptServiceImpl implements SimulationAttemptService {

    private final CareerSimulationDao simulationDao;
    private final SimulationTaskDao taskDao;
    private final SimulationAttemptDao attemptDao;
    private final TaskSubmissionDao submissionDao;
    private final EvaluationResultDao resultDao;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public SubmitAttemptResponse submitAttempt(
            String slug, Long studentId, SubmitAttemptRequest request) {
        validateRequest(request);
        log.info(
                "Submit attempt: slug={}, studentId={}, answersCount={}",
                slug,
                studentId,
                request.answers().size());
        // TODO(auth): replace header-based studentId with JWT claim when identity module is ready
        // Tracking: .agent/reports/ai/2026-09-11-us-02-frontend.md
        var simulation =
                simulationDao
                        .findBySlugAndStatus(slug, SimulationStatus.PUBLISHED)
                        .orElseThrow(
                                () -> new NotFoundException("Published simulation was not found."));
        List<SimulationTask> tasks =
                taskDao.findBySimulationIdOrderByDisplayOrderAsc(simulation.getId());
        List<TaskOutcomeDto> outcomes =
                tasks.stream().map(task -> evaluateTask(task, request.answers())).toList();
        int correctCount = (int) outcomes.stream().filter(TaskOutcomeDto::isCorrect).count();

        SimulationAttempt attempt =
                attemptDao.save(SimulationAttempt.createEvaluated(simulation.getId(), studentId));
        saveSubmissions(attempt.getId(), request.answers());
        saveResult(attempt.getId(), correctCount, tasks.size(), outcomes);

        int totalTasks = tasks.size();
        int percentage = totalTasks > 0 ? (correctCount * 100) / totalTasks : 0;
        log.info(
                "Scoring done: attemptId={}, correct={}/{}, percentage={}",
                attempt.getId(),
                correctCount,
                totalTasks,
                percentage);
        return new SubmitAttemptResponse(
                attempt.getId(), correctCount, totalTasks, percentage, outcomes);
    }

    private void validateRequest(SubmitAttemptRequest request) {
        if (request == null || request.answers() == null || request.answers().isEmpty()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Answers must not be empty.");
        }
    }

    private TaskOutcomeDto evaluateTask(SimulationTask task, Map<Long, String> answers) {
        Map<String, Object> rule = parseRule(task.getEvaluationRule());
        String correctOption = stringValue(rule.get("correctOption"));
        String explanation = stringValue(rule.get("explanation"));
        String selectedOption = answers.get(task.getId());
        boolean isCorrect = selectedOption != null && selectedOption.equals(correctOption);
        return new TaskOutcomeDto(
                task.getId(), task.getTitle(), selectedOption, isCorrect, explanation);
    }

    private void saveSubmissions(Long attemptId, Map<Long, String> answers) {
        List<TaskSubmission> submissions =
                answers.entrySet().stream()
                        .map(
                                entry ->
                                        TaskSubmission.create(
                                                attemptId,
                                                entry.getKey(),
                                                writeJson(
                                                        Map.of(
                                                                "selectedOption",
                                                                entry.getValue()))))
                        .toList();
        submissionDao.saveAll(submissions);
    }

    private void saveResult(
            Long attemptId, int correctCount, int totalTasks, List<TaskOutcomeDto> outcomes) {
        resultDao.save(
                EvaluationResult.create(
                        attemptId,
                        BigDecimal.valueOf(correctCount),
                        BigDecimal.valueOf(totalTasks),
                        writeJson(outcomes)));
    }

    private Map<String, Object> parseRule(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException exception) {
            throw new ApiException(
                    ErrorCode.INTERNAL_SERVER_ERROR, "Simulation evaluation data is invalid.");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new ApiException(
                    ErrorCode.INTERNAL_SERVER_ERROR, "Simulation result could not be saved.");
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
