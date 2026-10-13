package edu.hcmute.careersim.simulation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.hcmute.careersim.simulation.dto.SubmitAttemptRequest;
import edu.hcmute.careersim.simulation.dto.SubmitAttemptResponse;
import edu.hcmute.careersim.simulation.dto.TaskOutcomeDto;
import edu.hcmute.careersim.simulation.mapper.SimulationResultLocaleMapper;
import edu.hcmute.careersim.simulation.service.SimulationAttemptService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SimulationAttemptLocaleControllerTest {
    @Test
    void preservesSubmissionContractAndAddsVietnameseOnlyToEvaluatedResponse() throws Exception {
        var service = mock(SimulationAttemptService.class);
        var result =
                new SubmitAttemptResponse(
                        4L,
                        2,
                        3,
                        67,
                        List.of(
                                new TaskOutcomeDto(
                                        15L,
                                        "Handle duplicate records",
                                        "b",
                                        false,
                                        "A documented rule using a stable key and timestamp can be reviewed and repeated.")));
        when(service.submitAttempt(
                        eq("data-quality-investigation"), eq(1L), any(SubmitAttemptRequest.class)))
                .thenReturn(result);
        var mvc =
                MockMvcBuilders.standaloneSetup(
                                new SimulationAttemptController(
                                        service, new SimulationResultLocaleMapper()))
                        .build();
        mvc.perform(
                        post("/v1/simulations/data-quality-investigation/attempts")
                                .header("X-Student-Id", "1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"answers\":{\"15\":\"b\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.percentage").value(67))
                .andExpect(jsonPath("$.data.outcomes[0].isCorrect").value(false))
                .andExpect(jsonPath("$.data.outcomes[0].selectedOption").value("b"))
                .andExpect(
                        jsonPath("$.data.outcomes[0].explanation")
                                .value(result.outcomes().get(0).explanation()))
                .andExpect(jsonPath("$.data.outcomes[0].explanationVi").isNotEmpty());
        verify(service)
                .submitAttempt(
                        eq("data-quality-investigation"), eq(1L), any(SubmitAttemptRequest.class));
    }
}
