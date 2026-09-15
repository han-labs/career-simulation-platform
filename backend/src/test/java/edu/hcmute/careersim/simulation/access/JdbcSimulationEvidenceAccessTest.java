package edu.hcmute.careersim.simulation.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcSimulationEvidenceAccessTest {

    private final JdbcSimulationEvidenceAccess access =
            new JdbcSimulationEvidenceAccess(mock(JdbcTemplate.class), new ObjectMapper());

    @Test
    void mapsPersistedEvaluationOutcomesIntoGuidanceEvidence() {
        var outcomes =
                access.parseOutcomes(
                        """
                        [
                          {"title":"Interpret an API response","isCorrect":true},
                          {"title":"Identify a backend error","isCorrect":false}
                        ]
                        """);

        assertThat(outcomes)
                .containsExactly(
                        new SimulationEvidenceAccess.TaskOutcome(
                                "Interpret an API response", "OBSERVED_STRENGTH"),
                        new SimulationEvidenceAccess.TaskOutcome(
                                "Identify a backend error", "NEEDS_MORE_EVIDENCE"));
    }

    @Test
    void preservesExplicitGuidanceStatusesAndRejectsMalformedEvidence() {
        var outcomes =
                access.parseOutcomes(
                        """
                        {"outcomes":[{"label":"SQL querying","status":"PRACTISED"}]}
                        """);

        assertThat(outcomes)
                .containsExactly(
                        new SimulationEvidenceAccess.TaskOutcome("SQL querying", "PRACTISED"));
        assertThat(access.parseOutcomes("not-json")).isEmpty();
    }
}
