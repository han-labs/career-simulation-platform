package edu.hcmute.careersim.assessment.service;

import edu.hcmute.careersim.assessment.dao.AssessmentAnswerRepository;
import edu.hcmute.careersim.assessment.dao.AssessmentAttemptRepository;
import edu.hcmute.careersim.assessment.dao.AssessmentScoreRepository;
import edu.hcmute.careersim.assessment.dao.CareerQuizAnswerRepository;
import edu.hcmute.careersim.assessment.dao.CareerQuizQuestionRepository;
import edu.hcmute.careersim.assessment.dao.CareerQuizScoreRepository;
import edu.hcmute.careersim.assessment.dao.RiasecQuestionRepository;
import edu.hcmute.careersim.assessment.domain.CareerQuizScoringPolicy;
import edu.hcmute.careersim.assessment.domain.CareerQuizSubmissionPolicy;
import edu.hcmute.careersim.assessment.dto.AnswerItemDto;
import edu.hcmute.careersim.assessment.dto.AssessmentAttemptDto;
import edu.hcmute.careersim.assessment.dto.AssessmentResultDto;
import edu.hcmute.careersim.assessment.dto.CareerQuizQuestionDto;
import edu.hcmute.careersim.assessment.dto.CareerQuizResultDto;
import edu.hcmute.careersim.assessment.dto.CareerQuizScoreDto;
import edu.hcmute.careersim.assessment.dto.DimensionScoreDto;
import edu.hcmute.careersim.assessment.dto.RiasecQuestionDto;
import edu.hcmute.careersim.assessment.dto.SubmitAnswersRequest;
import edu.hcmute.careersim.assessment.entity.AssessmentAnswer;
import edu.hcmute.careersim.assessment.entity.AssessmentAnswerId;
import edu.hcmute.careersim.assessment.entity.AssessmentAttempt;
import edu.hcmute.careersim.assessment.entity.AssessmentScore;
import edu.hcmute.careersim.assessment.entity.AssessmentScoreId;
import edu.hcmute.careersim.assessment.entity.CareerQuizAnswer;
import edu.hcmute.careersim.assessment.entity.CareerQuizAnswerId;
import edu.hcmute.careersim.assessment.entity.CareerQuizQuestion;
import edu.hcmute.careersim.assessment.entity.CareerQuizScore;
import edu.hcmute.careersim.assessment.entity.CareerQuizScoreId;
import edu.hcmute.careersim.assessment.entity.RiasecQuestion;
import edu.hcmute.careersim.assessment.mapper.AssessmentMapper;
import edu.hcmute.careersim.common.exception.ApiException;
import edu.hcmute.careersim.common.exception.ErrorCode;
import edu.hcmute.careersim.common.exception.NotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
    private final CareerQuizQuestionRepository quizQuestionRepository;
    private final CareerQuizAnswerRepository quizAnswerRepository;
    private final CareerQuizScoreRepository quizScoreRepository;
    private final CareerQuizScoringPolicy quizScoringPolicy;
    private final CareerQuizSubmissionPolicy quizSubmissionPolicy;

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
        attempt.setAssessmentType("RIASEC");
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

        if (!"RIASEC".equals(attempt.getAssessmentType())) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Attempt does not belong to RIASEC.");
        }

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

        if (!"RIASEC".equals(attempt.getAssessmentType())) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Attempt does not belong to RIASEC.");
        }

        if (!"COMPLETED".equals(attempt.getStatus())) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Attempt is not completed yet.");
        }

        List<AssessmentScore> scores = scoreRepository.findByAttemptId(attemptId);
        return buildResultDto(attemptId, scores);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CareerQuizQuestionDto> getQuizQuestions(String quizType) {
        String type = requireQuizType(quizType);
        List<CareerQuizQuestion> questions =
                quizQuestionRepository.findAllByQuizTypeAndActiveTrueOrderByDisplayOrderAsc(type);
        quizSubmissionPolicy.requireValidQuestions(type, questions);
        return questions.stream().map(mapper::toDto).toList();
    }

    @Override
    @Transactional
    public AssessmentAttemptDto startQuizAttempt(String quizType, Long studentId) {
        String type = requireQuizType(quizType);
        quizSubmissionPolicy.requireValidQuestions(
                type,
                quizQuestionRepository.findAllByQuizTypeAndActiveTrueOrderByDisplayOrderAsc(type));
        AssessmentAttempt attempt = new AssessmentAttempt();
        attempt.setStudentId(studentId);
        attempt.setAssessmentType(type);
        attempt.setStatus("IN_PROGRESS");
        return mapper.toDto(attemptRepository.save(attempt));
    }

    @Override
    @Transactional
    public CareerQuizResultDto submitQuizAnswers(
            Long attemptId, Long studentId, SubmitAnswersRequest request) {
        AssessmentAttempt attempt = findOwnedQuizAttempt(attemptId, studentId);
        if ("COMPLETED".equals(attempt.getStatus()))
            return getQuizAttemptResult(attemptId, studentId);

        List<CareerQuizQuestion> questions =
                quizQuestionRepository.findAllByQuizTypeAndActiveTrueOrderByDisplayOrderAsc(
                        attempt.getAssessmentType());
        quizSubmissionPolicy.requireValidQuestions(attempt.getAssessmentType(), questions);
        Map<Long, Short> answerMap =
                quizSubmissionPolicy.validateAnswers(questions, request.answers());

        for (CareerQuizQuestion question : questions) {
            CareerQuizAnswer answer = new CareerQuizAnswer();
            answer.setId(new CareerQuizAnswerId(attemptId, question.getId()));
            answer.setAttempt(attempt);
            answer.setQuestion(question);
            answer.setScore(answerMap.get(question.getId()));
            quizAnswerRepository.save(answer);
        }
        Map<String, Integer> calculated = quizScoringPolicy.score(questions, answerMap);
        List<CareerQuizScore> scores = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : calculated.entrySet()) {
            CareerQuizScore score = new CareerQuizScore();
            score.setId(new CareerQuizScoreId(attemptId, entry.getKey()));
            score.setAttempt(attempt);
            score.setScore(entry.getValue());
            scores.add(quizScoreRepository.save(score));
        }
        attempt.setStatus("COMPLETED");
        attempt.setCompletedAt(Instant.now());
        attemptRepository.save(attempt);
        return buildQuizResult(attempt, scores);
    }

    @Override
    @Transactional(readOnly = true)
    public CareerQuizResultDto getQuizAttemptResult(Long attemptId, Long studentId) {
        AssessmentAttempt attempt = findOwnedQuizAttempt(attemptId, studentId);
        if (!"COMPLETED".equals(attempt.getStatus())) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Attempt is not completed yet.");
        }
        return buildQuizResult(attempt, quizScoreRepository.findByIdAttemptId(attemptId));
    }

    private AssessmentAttempt findOwnedQuizAttempt(Long attemptId, Long studentId) {
        AssessmentAttempt attempt =
                attemptRepository
                        .findByIdAndStudentId(attemptId, studentId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                "Attempt not found or you don't have access."));
        if (!Set.of("INTERESTS", "ABILITIES", "WORK_ACTIVITIES")
                .contains(attempt.getAssessmentType())) {
            throw new ApiException(
                    ErrorCode.BAD_REQUEST, "Attempt does not belong to a career quiz.");
        }
        return attempt;
    }

    private String requireQuizType(String quizType) {
        if (quizType == null)
            throw new ApiException(ErrorCode.BAD_REQUEST, "Unsupported assessment type.");
        String normalized = quizType.toUpperCase(Locale.ROOT);
        if (!Set.of("INTERESTS", "ABILITIES", "WORK_ACTIVITIES").contains(normalized)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Unsupported assessment type.");
        }
        return normalized;
    }

    private CareerQuizResultDto buildQuizResult(
            AssessmentAttempt attempt, List<CareerQuizScore> scores) {
        List<CareerQuizScoreDto> scoreDtos =
                scores.stream()
                        .map(
                                score ->
                                        new CareerQuizScoreDto(
                                                score.getId().getCategory(), score.getScore()))
                        .sorted(Comparator.comparing(CareerQuizScoreDto::category))
                        .toList();
        int highest = scoreDtos.stream().mapToInt(CareerQuizScoreDto::score).max().orElse(0);
        List<String> top =
                scoreDtos.stream()
                        .filter(score -> score.score() == highest)
                        .map(CareerQuizScoreDto::category)
                        .toList();
        return new CareerQuizResultDto(
                attempt.getId(), attempt.getAssessmentType(), scoreDtos, top);
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
