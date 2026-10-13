package edu.hcmute.careersim.simulation.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.hcmute.careersim.simulation.dto.OptionDto;
import edu.hcmute.careersim.simulation.dto.SimulationTaskDto;
import edu.hcmute.careersim.simulation.dto.SubmitAttemptResponse;
import edu.hcmute.careersim.simulation.dto.TaskOutcomeDto;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class SimulationResultLocaleMapperTest {
    private final SimulationResultLocaleMapper mapper = new SimulationResultLocaleMapper();

    @Test
    void hasReviewedTranslationsForAllFifteenCurrentEvaluatedExplanations() throws Exception {
        var explanations = new ArrayList<String>();
        try (var stream =
                getClass()
                        .getResourceAsStream(
                                "/db/migration/V5__align_backend_simulation_seed.sql")) {
            var sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            var matches = Pattern.compile("'explanation', '([^']+)'").matcher(sql);
            while (matches.find()) explanations.add(matches.group(1));
        }
        try (var stream =
                getClass().getResourceAsStream("/db/migration/V8__seed_complete_local_demo.sql")) {
            var sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            var matches = Pattern.compile("(?m)^\\s{12}'([^'\\r\\n]+)',?\\r?$").matcher(sql);
            var copy = new ArrayList<String>();
            while (matches.find()) {
                var value = matches.group(1);
                if (value.contains(" ") && !value.startsWith("[")) copy.add(value);
            }
            assertThat(copy).hasSize(36);
            for (int index = 2; index < copy.size(); index += 3) explanations.add(copy.get(index));
        }
        assertThat(explanations).hasSize(15);
        for (var explanation : explanations) {
            var result =
                    new SubmitAttemptResponse(
                            1L,
                            1,
                            1,
                            100,
                            List.of(
                                    new TaskOutcomeDto(
                                            1L, "Evaluated task", "a", true, explanation)));
            assertThat(mapper.localize(result).outcomes().get(0).explanationVi()).isNotBlank();
        }
    }

    @Test
    void addsReviewedTextWithoutChangingObjectiveResultsOrPersistedRepresentation()
            throws Exception {
        var original =
                new TaskOutcomeDto(
                        15L,
                        "Handle duplicate records",
                        "b",
                        false,
                        "A documented rule using a stable key and timestamp can be reviewed and repeated.");
        var result = new SubmitAttemptResponse(4L, 2, 3, 67, List.of(original));
        var localized = mapper.localize(result);
        assertThat(localized.attemptId()).isEqualTo(result.attemptId());
        assertThat(localized.correctCount()).isEqualTo(2);
        assertThat(localized.totalTasks()).isEqualTo(3);
        assertThat(localized.percentage()).isEqualTo(67);
        var outcome = localized.outcomes().get(0);
        assertThat(outcome.taskId()).isEqualTo(15L);
        assertThat(outcome.title()).isEqualTo(original.title());
        assertThat(outcome.selectedOption()).isEqualTo("b");
        assertThat(outcome.isCorrect()).isFalse();
        assertThat(outcome.explanation()).isEqualTo(original.explanation());
        assertThat(outcome.explanationVi()).contains("Quy tắc được ghi lại");
        var json = new ObjectMapper();
        assertThat(json.writeValueAsString(original)).doesNotContain("explanationVi");
        assertThat(json.writeValueAsString(outcome)).contains("explanationVi");
    }

    @Test
    void keepsUnknownAndNullExplanationsWithoutInventingTranslations() {
        var unknown =
                new TaskOutcomeDto(1L, "Future task", "a", true, "Future original explanation");
        var empty = new TaskOutcomeDto(2L, "No explanation", null, false, null);
        var localized =
                mapper.localize(new SubmitAttemptResponse(1L, 1, 2, 50, List.of(unknown, empty)));
        assertThat(localized.outcomes()).containsExactly(unknown, empty);
    }

    @Test
    void publicTaskRepresentationStillContainsNeitherSolutionNorTranslatedExplanation()
            throws Exception {
        var publicTask =
                new SimulationTaskDto(
                        1L,
                        1,
                        "Title",
                        "Instructions",
                        "MULTIPLE_CHOICE",
                        List.of(new OptionDto("a", "First option")));
        var json = new ObjectMapper().writeValueAsString(publicTask);
        assertThat(json).doesNotContain("explanation", "correctOption", "isCorrect");
    }
}
