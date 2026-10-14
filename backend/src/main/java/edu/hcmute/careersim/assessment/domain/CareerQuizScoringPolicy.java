package edu.hcmute.careersim.assessment.domain;

import edu.hcmute.careersim.assessment.entity.CareerQuizQuestion;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Sums required 1–5 self-ratings using category assignments stored with backend questions. */
@Component
public class CareerQuizScoringPolicy {
    public Map<String, Integer> score(
            List<CareerQuizQuestion> questions, Map<Long, Short> answers) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (CareerQuizQuestion question : questions) {
            Short answer = answers.get(question.getId());
            scores.merge(question.getCategory(), answer.intValue(), Integer::sum);
        }
        return scores;
    }
}
