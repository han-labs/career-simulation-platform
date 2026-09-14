// Loads published simulations while keeping evaluation rules private.
package edu.hcmute.careersim.simulation.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.hcmute.careersim.catalog.dao.CareerSimulationDao;
import edu.hcmute.careersim.catalog.enumeration.SimulationStatus;
import edu.hcmute.careersim.common.exception.ApiException;
import edu.hcmute.careersim.common.exception.ErrorCode;
import edu.hcmute.careersim.common.exception.NotFoundException;
import edu.hcmute.careersim.simulation.dao.SimulationTaskDao;
import edu.hcmute.careersim.simulation.dto.OptionDto;
import edu.hcmute.careersim.simulation.dto.SimulationDetailResponse;
import edu.hcmute.careersim.simulation.dto.SimulationTaskDto;
import edu.hcmute.careersim.simulation.entity.SimulationTask;
import edu.hcmute.careersim.simulation.service.SimulationQueryService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SimulationQueryServiceImpl implements SimulationQueryService {

    private final CareerSimulationDao simulationDao;
    private final SimulationTaskDao taskDao;
    private final ObjectMapper objectMapper;

    @Override
    public SimulationDetailResponse getSimulationWithTasks(String slug) {
        var simulation = simulationDao.findBySlugAndStatus(slug, SimulationStatus.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Published simulation was not found."));
        List<SimulationTaskDto> tasks = taskDao
                .findBySimulationIdOrderByDisplayOrderAsc(simulation.getId())
                .stream()
                .map(this::toPublicTask)
                .toList();

        return new SimulationDetailResponse(
                simulation.getSlug(),
                simulation.getTitle(),
                simulation.getSummary(),
                simulation.getDifficulty().name(),
                simulation.getEstimatedMinutes(),
                tasks);
    }

    private SimulationTaskDto toPublicTask(SimulationTask task) {
        Map<String, Object> rule = parseRule(task.getEvaluationRule());
        return new SimulationTaskDto(
                task.getId(),
                task.getDisplayOrder(),
                task.getTitle(),
                task.getInstructions(),
                task.getTaskType().name(),
                extractOptions(rule));
    }

    private Map<String, Object> parseRule(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException exception) {
            throw new ApiException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Simulation evaluation data is invalid.");
        }
    }

    private List<OptionDto> extractOptions(Map<String, Object> rule) {
        Object rawOptions = rule.get("options");
        if (!(rawOptions instanceof List<?> options)) {
            return Collections.emptyList();
        }
        return options.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(option -> new OptionDto(
                        String.valueOf(option.get("id")),
                        String.valueOf(option.get("label"))))
                .toList();
    }
}
