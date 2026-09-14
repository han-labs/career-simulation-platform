package edu.hcmute.careersim.assessment.service;

import edu.hcmute.careersim.assessment.dao.AssessmentAnswerRepository;
import edu.hcmute.careersim.assessment.dao.AssessmentAttemptRepository;
import edu.hcmute.careersim.assessment.dao.AssessmentScoreRepository;
import edu.hcmute.careersim.assessment.dao.RiasecQuestionRepository;
import edu.hcmute.careersim.assessment.dto.AnswerItemDto;
import edu.hcmute.careersim.assessment.dto.AssessmentAttemptDto;
import edu.hcmute.careersim.assessment.dto.AssessmentResultDto;
import edu.hcmute.careersim.assessment.dto.DimensionScoreDto;
import edu.hcmute.careersim.assessment.dto.RiasecQuestionDto;
import edu.hcmute.careersim.assessment.dto.SubmitAnswersRequest;
import edu.hcmute.careersim.assessment.entity.AssessmentAnswer;
import edu.hcmute.careersim.assessment.entity.AssessmentAnswerId;
import edu.hcmute.careersim.assessment.entity.AssessmentAttempt;
import edu.hcmute.careersim.assessment.entity.AssessmentScore;
import edu.hcmute.careersim.assessment.entity.AssessmentScoreId;
import edu.hcmute.careersim.assessment.entity.RiasecQuestion;
import edu.hcmute.careersim.assessment.mapper.AssessmentMapper;
import edu.hcmute.careersim.common.exception.ApiException;
import edu.hcmute.careersim.common.exception.ErrorCode;
import edu.hcmute.careersim.common.exception.NotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssessmentServiceImpl implements AssessmentService {

    private final RiasecQuestionRepository questionRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final AssessmentAnswerRepository answerRepository;
    private final AssessmentScoreRepository scoreRepository;
    private final AssessmentMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<RiasecQuestionDto> getActiveQuestions() {
        return questionRepository.findAllByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AssessmentAttemptDto startAttempt(Long studentId) {
        AssessmentAttempt attempt = new AssessmentAttempt();
        attempt.setStudentId(studentId);
        attempt.setStatus("IN_PROGRESS");
        attempt = attemptRepository.save(attempt);
        return mapper.toDto(attempt);
    }

    @Override
    @Transactional
    public AssessmentResultDto submitAnswers(
            Long attemptId, Long studentId, SubmitAnswersRequest request) {
        AssessmentAttempt attempt =
                attemptRepository
                        .findByIdAndStudentId(attemptId, studentId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                "Attempt not found or you don't have access."));

        if ("COMPLETED".equals(attempt.getStatus())) {
            return getAttemptResult(attemptId, studentId);
        }

        List<RiasecQuestion> activeQuestions =
                questionRepository.findAllByActiveTrueOrderByDisplayOrderAsc();
        Map<Long, RiasecQuestion> questionMap =
                activeQuestions.stream().collect(Collectors.toMap(RiasecQuestion::getId, q -> q));

        if (request.answers().size() != activeQuestions.size()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR, "You must answer all active questions.");
        }

        Map<String, Integer> scoresByDimension = new HashMap<>();
        for (String dim : new String[] {"R", "I", "A", "S", "E", "C"}) {
            scoresByDimension.put(dim, 0);
        }

        for (AnswerItemDto item : request.answers()) {
            RiasecQuestion question = questionMap.get(item.questionId());
            if (question == null) {
                throw new ApiException(
                        ErrorCode.VALIDATION_ERROR, "Invalid question ID: " + item.questionId());
            }

            AssessmentAnswer answer = new AssessmentAnswer();
            answer.setId(new AssessmentAnswerId(attemptId, question.getId()));
            answer.setAttempt(attempt);
            answer.setQuestion(question);
            answer.setScore(item.score());
            answerRepository.save(answer);

            String dimension = question.getDimension();
            scoresByDimension.put(dimension, scoresByDimension.get(dimension) + item.score());
        }

        List<AssessmentScore> savedScores = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : scoresByDimension.entrySet()) {
            AssessmentScore score = new AssessmentScore();
            score.setId(new AssessmentScoreId(attemptId, entry.getKey()));
            score.setAttempt(attempt);
            score.setScore(entry.getValue());
            savedScores.add(scoreRepository.save(score));
        }

        attempt.setStatus("COMPLETED");
        attempt.setCompletedAt(Instant.now());
        attemptRepository.save(attempt);

        return buildResultDto(attemptId, savedScores);
    }

    @Override
    @Transactional(readOnly = true)
    public AssessmentResultDto getAttemptResult(Long attemptId, Long studentId) {
        AssessmentAttempt attempt =
                attemptRepository
                        .findByIdAndStudentId(attemptId, studentId)
                        .orElseThrow(() -> new NotFoundException("Attempt not found."));

        if (!"COMPLETED".equals(attempt.getStatus())) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Attempt is not completed yet.");
        }

        List<AssessmentScore> scores = scoreRepository.findByAttemptId(attemptId);
        return buildResultDto(attemptId, scores);
    }

    private AssessmentResultDto buildResultDto(Long attemptId, List<AssessmentScore> scores) {
        List<DimensionScoreDto> scoreDtos =
                scores.stream()
                        .map(s -> new DimensionScoreDto(s.getId().getDimension(), s.getScore()))
                        .collect(Collectors.toList());

        List<String> topDimensions =
                scores.stream()
                        .sorted((s1, s2) -> Integer.compare(s2.getScore(), s1.getScore()))
                        .limit(3)
                        .map(s -> s.getId().getDimension())
                        .collect(Collectors.toList());

        return new AssessmentResultDto(attemptId, scoreDtos, topDimensions);
    }
}
