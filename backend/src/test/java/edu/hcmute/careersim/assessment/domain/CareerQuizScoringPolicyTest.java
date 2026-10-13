package edu.hcmute.careersim.assessment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import edu.hcmute.careersim.assessment.entity.CareerQuizQuestion;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CareerQuizScoringPolicyTest {
    private final CareerQuizScoringPolicy policy = new CareerQuizScoringPolicy();

    @Test
    void sumsRatingsByBackendQuestionCategoryAndKeepsDifferentCategoriesSeparate() {
        var questions =
                List.of(
                        question(1L, "ANALYTICAL"),
                        question(2L, "CREATIVE"),
                        question(3L, "ANALYTICAL"));

        assertThat(policy.score(questions, Map.of(1L, (short) 5, 2L, (short) 2, 3L, (short) 4)))
                .containsExactlyInAnyOrderEntriesOf(Map.of("ANALYTICAL", 9, "CREATIVE", 2));
    }

    @Test
    void scoresWorkActivityPreferencesIndependentlyForInformationPeopleAndThings() {
        var questions =
                List.of(
                        question(1L, "INFORMATION"),
                        question(2L, "INFORMATION"),
                        question(3L, "INFORMATION"),
                        question(4L, "PEOPLE"),
                        question(5L, "PEOPLE"),
                        question(6L, "PEOPLE"),
                        question(7L, "THINGS"),
                        question(8L, "THINGS"),
                        question(9L, "THINGS"));

        assertThat(
                        policy.score(
                                questions,
                                Map.of(
                                        1L, (short) 5,
                                        2L, (short) 4,
                                        3L, (short) 3,
                                        4L, (short) 4,
                                        5L, (short) 4,
                                        6L, (short) 4,
                                        7L, (short) 2,
                                        8L, (short) 2,
                                        9L, (short) 2)))
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of("INFORMATION", 12, "PEOPLE", 12, "THINGS", 6));
    }

    private CareerQuizQuestion question(Long id, String category) {
        CareerQuizQuestion question = new CareerQuizQuestion();
        question.setId(id);
        question.setCategory(category);
        return question;
    }
}
