package edu.hcmute.careersim.assessment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.hcmute.careersim.assessment.dto.AnswerItemDto;
import edu.hcmute.careersim.assessment.entity.CareerQuizQuestion;
import edu.hcmute.careersim.common.exception.ApiException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CareerQuizSubmissionPolicyTest {
    private final CareerQuizSubmissionPolicy policy = new CareerQuizSubmissionPolicy();

    @Test
    void acceptsOnlyCompleteFourCategoryQuestionSetsWithThreeItemsEach() {
        assertThatThrownBy(() -> policy.requireValidQuestions("INTERESTS", quizQuestions(true)))
                .isInstanceOf(ApiException.class)
                .hasMessage("This assessment is not available.");
        policy.requireValidQuestions("INTERESTS", quizQuestions(false));
    }

    @Test
    void acceptsWorkActivitiesConfigurationWithThreeQuestionsForEachPreferenceArea() {
        policy.requireValidQuestions("WORK_ACTIVITIES", workActivityQuestions(false));
        assertThatThrownBy(
                        () ->
                                policy.requireValidQuestions(
                                        "WORK_ACTIVITIES", workActivityQuestions(true)))
                .isInstanceOf(ApiException.class)
                .hasMessage("This assessment is not available.");
    }

    @Test
    void requiresEveryQuestionExactlyOnceAndEnforcesOneToFiveScale() {
        var questions = quizQuestions(false);
        var incomplete = answers(questions.size() - 1, 3);
        assertThatThrownBy(() -> policy.validateAnswers(questions, incomplete))
                .isInstanceOf(ApiException.class)
                .hasMessage("You must answer all active questions.");

        var duplicate = new ArrayList<>(answers(questions.size(), 3));
        duplicate.set(duplicate.size() - 1, new AnswerItemDto(1L, (short) 3));
        assertThatThrownBy(() -> policy.validateAnswers(questions, duplicate))
                .isInstanceOf(ApiException.class)
                .hasMessage("Duplicate question response: 1");

        assertThatThrownBy(() -> policy.validateAnswers(questions, answers(12, 6)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Each response must be between 1 and 5.");

        assertThat(policy.validateAnswers(questions, answers(12, 4))).hasSize(12);
    }

    @Test
    void acceptsCompleteWorkActivitiesAnswersAndRequiresAllNineQuestions() {
        var questions = workActivityQuestions(false);
        assertThatThrownBy(() -> policy.validateAnswers(questions, answers(8, 3)))
                .isInstanceOf(ApiException.class)
                .hasMessage("You must answer all active questions.");
        assertThat(policy.validateAnswers(questions, answers(9, 5))).hasSize(9);
    }

    @Test
    void rejectsQuestionIdsFromAnotherQuiz() {
        var questions = quizQuestions(false);
        var submitted = answers(12, 4);
        submitted.set(0, new AnswerItemDto(999L, (short) 4));
        assertThatThrownBy(() -> policy.validateAnswers(questions, submitted))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid question ID: 999");
    }

    private List<CareerQuizQuestion> quizQuestions(boolean invalid) {
        List<String> categories = List.of("ANALYTICAL", "CREATIVE", "COLLABORATIVE", "ORGANIZING");
        var result = new ArrayList<CareerQuizQuestion>();
        long id = 1;
        for (int category = 0; category < categories.size(); category++) {
            int itemCount = invalid && category == 0 ? 2 : 3;
            for (int item = 0; item < itemCount; item++) {
                var question = new CareerQuizQuestion();
                question.setId(id++);
                question.setCategory(categories.get(category));
                result.add(question);
            }
        }
        return result;
    }

    private List<CareerQuizQuestion> workActivityQuestions(boolean invalid) {
        List<String> categories = List.of("INFORMATION", "PEOPLE", "THINGS");
        var result = new ArrayList<CareerQuizQuestion>();
        long id = 1;
        for (String category : categories) {
            int itemCount = invalid && "THINGS".equals(category) ? 2 : 3;
            for (int item = 0; item < itemCount; item++) {
                var question = new CareerQuizQuestion();
                question.setId(id++);
                question.setCategory(category);
                result.add(question);
            }
        }
        return result;
    }

    private ArrayList<AnswerItemDto> answers(int count, int score) {
        var answers = new ArrayList<AnswerItemDto>();
        for (int id = 1; id <= count; id++)
            answers.add(new AnswerItemDto((long) id, (short) score));
        return answers;
    }
}
