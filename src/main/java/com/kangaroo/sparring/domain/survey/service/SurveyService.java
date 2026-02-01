package com.kangaroo.sparring.domain.survey.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangaroo.sparring.domain.survey.dto.req.SurveySubmitRequest;
import com.kangaroo.sparring.domain.survey.dto.req.UpdateAnswerRequest;
import com.kangaroo.sparring.domain.survey.dto.res.OptionItem;
import com.kangaroo.sparring.domain.survey.dto.res.AnswerResponse;
import com.kangaroo.sparring.domain.survey.dto.res.SurveyAnswersResponse;
import com.kangaroo.sparring.domain.survey.dto.res.SurveyQuestionsResponse;
import com.kangaroo.sparring.domain.survey.dto.res.SurveySubmitResponse;

import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.healthprofile.repository.HealthProfileRepository;
import com.kangaroo.sparring.domain.survey.entity.Answer;
import com.kangaroo.sparring.domain.survey.entity.Question;
import com.kangaroo.sparring.domain.survey.entity.QuestionType;
import com.kangaroo.sparring.domain.survey.entity.Survey;
import com.kangaroo.sparring.domain.survey.entity.SurveyType;
import com.kangaroo.sparring.domain.survey.repository.AnswerRepository;
import com.kangaroo.sparring.domain.survey.repository.QuestionRepository;
import com.kangaroo.sparring.domain.survey.repository.SurveyRepository;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SurveyService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SurveyRepository surveyRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final HealthProfileRepository healthProfileRepository;
    private final UserRepository userRepository;

    /**
     * 설문 문항 조회
     */
    public SurveyQuestionsResponse getSurveyQuestions(SurveyType surveyType) {
        Survey survey = surveyRepository.findBySurveyType(surveyType)
                .orElseThrow(() -> new CustomException(ErrorCode.SURVEY_NOT_FOUND));
        validateHealthProfileFieldMappings(survey.getQuestions());

        return SurveyQuestionsResponse.from(survey);
    }

    /**
     * 설문 응답 제출
     */
    @Transactional
    public SurveySubmitResponse submitSurvey(Long userId, SurveySubmitRequest request) {
        // 이미 완료한 설문인지 확인
        if (answerRepository.existsByUserIdAndSurveyType(userId, request.getSurveyType())) {
            throw new CustomException(ErrorCode.SURVEY_ALREADY_COMPLETED);
        }

        // Survey 존재 여부 확인
        surveyRepository.findBySurveyType(request.getSurveyType())
                .orElseThrow(() -> new CustomException(ErrorCode.SURVEY_NOT_FOUND));

        List<Question> questions = questionRepository.findBySurveyType(request.getSurveyType());
        validateHealthProfileFieldMappings(questions);

        Map<String, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getQuestionKey, question -> question));
        validateSubmittedAnswers(request.getAnswers(), questionMap);

        // User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 답변 저장
        List<Answer> answers = request.getAnswers().stream()
                .map(answerItem -> {
                    Question question = questionMap.get(answerItem.getQuestionKey());
                    return Answer.builder()
                            .user(user)
                            .question(question)
                            .answerText(toStoredAnswer(question, answerItem.getValue()))
                            .build();
                })
                .collect(Collectors.toList());

        try {
            answerRepository.saveAll(answers);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.SURVEY_ALREADY_COMPLETED);
        }

        // HealthProfile 생성 또는 업데이트
        updateHealthProfile(userId, request.getSurveyType(), answers);

        return SurveySubmitResponse.of(request.getSurveyType());
    }

    /**
     * 설문 응답 수정
     */
    @Transactional
    public AnswerResponse updateAnswer(Long userId, UpdateAnswerRequest request) {
        Question question = questionRepository.findByQuestionKey(request.getQuestionKey())
                .orElseThrow(() -> new CustomException(ErrorCode.QUESTION_NOT_FOUND));

        Answer answer = answerRepository.findByUserIdAndQuestionId(userId, question.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.ANSWER_NOT_FOUND));

        answer.updateAnswer(toStoredAnswer(question, request.getValue()));

        // HealthProfile 업데이트
        updateHealthProfileFromSingleAnswer(userId, answer);

        return AnswerResponse.from(answer);
    }

    private void validateHealthProfileFieldMappings(List<Question> questions) {
        List<String> invalidFields = new ArrayList<>();
        for (Question question : questions) {
            String fieldName = question.getHealthProfileField();
            if (fieldName == null || fieldName.isBlank()) {
                continue;
            }
            if (!HealthProfile.isSupportedField(fieldName)) {
                invalidFields.add(question.getQuestionKey() + ":" + fieldName);
            }
        }
        if (!invalidFields.isEmpty()) {
            log.error("Invalid healthProfileField mappings: {}", String.join(", ", invalidFields));
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private void validateSubmittedAnswers(
            List<SurveySubmitRequest.AnswerItem> submittedAnswers,
            Map<String, Question> questionMap
    ) {
        Set<String> submittedQuestionKeys = new HashSet<>();
        for (SurveySubmitRequest.AnswerItem answerItem : submittedAnswers) {
            if (!submittedQuestionKeys.add(answerItem.getQuestionKey())) {
                throw new CustomException(ErrorCode.INVALID_INPUT, "중복된 questionKey: " + answerItem.getQuestionKey());
            }
        }

        Set<String> allowedQuestionKeys = questionMap.keySet();
        Set<String> unknownQuestionKeys = submittedQuestionKeys.stream()
                .filter(key -> !allowedQuestionKeys.contains(key))
                .collect(Collectors.toSet());
        if (!unknownQuestionKeys.isEmpty()) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "유효하지 않은 questionKey: " + String.join(", ", unknownQuestionKeys)
            );
        }

        Set<String> requiredQuestionKeys = questionMap.values().stream()
                .filter(question -> Boolean.TRUE.equals(question.getIsRequired()))
                .map(Question::getQuestionKey)
                .collect(Collectors.toSet());
        Set<String> missingRequiredKeys = requiredQuestionKeys.stream()
                .filter(key -> !submittedQuestionKeys.contains(key))
                .collect(Collectors.toSet());
        if (!missingRequiredKeys.isEmpty()) {
            throw new CustomException(
                    ErrorCode.SURVEY_INCOMPLETE,
                    "필수 문항 누락: " + String.join(", ", missingRequiredKeys)
            );
        }

        for (SurveySubmitRequest.AnswerItem answerItem : submittedAnswers) {
            Question question = questionMap.get(answerItem.getQuestionKey());
            toStoredAnswer(question, answerItem.getValue());
        }
    }

    private String toStoredAnswer(Question question, JsonNode value) {
        if (value == null || value.isNull()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "답변 값은 null일 수 없습니다.");
        }

        QuestionType questionType = question.getQuestionType();
        Set<String> optionCodes = parseOptionCodes(question);
        return switch (questionType) {
            case TEXT -> toScalarString(value, question.getQuestionKey(), questionType);
            case SINGLE_CHOICE -> toSingleChoiceCode(value, question.getQuestionKey(), optionCodes);
            case NUMBER -> toNumberLikeString(value, question.getQuestionKey());
            case MULTIPLE_CHOICE -> toJsonStringArrayOfCodes(value, question.getQuestionKey(), optionCodes);
        };
    }

    private Set<String> parseOptionCodes(Question question) {
        String options = question.getOptions();
        if (options == null || options.isBlank()) {
            return Set.of();
        }
        try {
            List<OptionItem> optionItems = OBJECT_MAPPER.readValue(options, new TypeReference<List<OptionItem>>() {});
            return optionItems.stream()
                    .map(OptionItem::getCode)
                    .filter(code -> code != null && !code.isBlank())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "문항(" + question.getQuestionKey() + ") 옵션 포맷이 올바르지 않습니다.");
        }
    }

    private String toScalarString(JsonNode value, String questionKey, QuestionType questionType) {
        if (value.isArray() || value.isObject()) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "문항(" + questionKey + ")의 " + questionType + " 답변은 단일 값이어야 합니다."
            );
        }
        String text = value.asText().trim();
        if (text.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "문항(" + questionKey + ") 답변 값이 비어있습니다.");
        }
        return text;
    }

    private String toSingleChoiceCode(JsonNode value, String questionKey, Set<String> optionCodes) {
        String code = toScalarString(value, questionKey, QuestionType.SINGLE_CHOICE);
        if (!optionCodes.isEmpty() && !optionCodes.contains(code)) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "문항(" + questionKey + ")의 SINGLE_CHOICE 값이 옵션 code에 없습니다: " + code
            );
        }
        return code;
    }

    private String toNumberLikeString(JsonNode value, String questionKey) {
        if (!(value.isNumber() || value.isTextual())) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "문항(" + questionKey + ")의 NUMBER 답변은 숫자 또는 숫자 문자열이어야 합니다."
            );
        }
        String text = value.asText().trim();
        if (text.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "문항(" + questionKey + ") 답변 값이 비어있습니다.");
        }
        return text;
    }

    private String toJsonStringArrayOfCodes(JsonNode value, String questionKey, Set<String> optionCodes) {
        if (!value.isArray()) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "문항(" + questionKey + ")의 MULTIPLE_CHOICE 값은 code 문자열 배열이어야 합니다."
            );
        }

        List<String> codes = new ArrayList<>();
        for (JsonNode node : value) {
            if (!node.isTextual()) {
                throw new CustomException(
                        ErrorCode.INVALID_INPUT,
                        "문항(" + questionKey + ")의 MULTIPLE_CHOICE 항목은 문자열이어야 합니다."
                );
            }
            String code = node.asText().trim();
            if (code.isBlank()) {
                throw new CustomException(
                        ErrorCode.INVALID_INPUT,
                        "문항(" + questionKey + ")의 MULTIPLE_CHOICE 항목은 비어있을 수 없습니다."
                );
            }
            if (!optionCodes.isEmpty() && !optionCodes.contains(code)) {
                throw new CustomException(
                        ErrorCode.INVALID_INPUT,
                        "문항(" + questionKey + ")의 MULTIPLE_CHOICE 값이 옵션 code에 없습니다: " + code
                );
            }
            codes.add(code);
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(codes);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "문항(" + questionKey + ") 배열 직렬화에 실패했습니다.");
        }
    }

    /**
     * 설문 응답 조회
     */
    public SurveyAnswersResponse getSurveyAnswers(Long userId, SurveyType surveyType) {
        List<Answer> answers = answerRepository.findByUserIdAndSurveyType(userId, surveyType);

        if (answers.isEmpty()) {
            throw new CustomException(ErrorCode.SURVEY_NOT_COMPLETED);
        }

        List<AnswerResponse> answerResponses = answers.stream()
                .map(AnswerResponse::from)
                .collect(Collectors.toList());

        return SurveyAnswersResponse.of(surveyType, answerResponses);
    }

    /**
     * 설문 완료 여부 확인
     */
    public Boolean checkSurveyCompleted(Long userId, SurveyType surveyType) {
        return answerRepository.existsByUserIdAndSurveyType(userId, surveyType);
    }

    /**
     * HealthProfile 생성 또는 업데이트
     */
    private void updateHealthProfile(Long userId, SurveyType surveyType, List<Answer> answers) {
        HealthProfile healthProfile = healthProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
                    return HealthProfile.builder()
                            .user(user)
                            .build();
                });

        // 설문 타입에 따라 HealthProfile 업데이트
        if (surveyType == SurveyType.BASIC || surveyType == SurveyType.DETAILED) {
            applyHealthProfileUpdates(healthProfile, answers);
        }

        try {
            healthProfileRepository.save(healthProfile);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "건강 프로필 값 형식 또는 범위 오류");
        }
    }

    /**
     * 단일 답변으로 HealthProfile 업데이트 (수정 시)
     */
    private void updateHealthProfileFromSingleAnswer(Long userId, Answer answer) {
        HealthProfile healthProfile = healthProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.HEALTH_PROFILE_NOT_FOUND));

        String fieldName = answer.getQuestion().getHealthProfileField();
        if (fieldName == null || fieldName.isBlank()) {
            return;
        }

        if (!HealthProfile.isSupportedField(fieldName)) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "지원하지 않는 healthProfileField: " + fieldName);
        }

        boolean applied = healthProfile.applySurveyField(fieldName, answer.getAnswerText());
        if (!applied) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "healthProfileField 값 형식 오류: " + fieldName);
        }
        try {
            healthProfileRepository.save(healthProfile);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "건강 프로필 값 형식 또는 범위 오류");
        }
    }

    private void applyHealthProfileUpdates(HealthProfile healthProfile, List<Answer> answers) {
        List<String> invalidFields = new ArrayList<>();
        for (Answer answer : answers) {
            String fieldName = answer.getQuestion().getHealthProfileField();
            if (fieldName == null || fieldName.isBlank()) {
                continue;
            }
            if (!HealthProfile.isSupportedField(fieldName)) {
                invalidFields.add(fieldName);
                continue;
            }
            boolean applied = healthProfile.applySurveyField(fieldName, answer.getAnswerText());
            if (!applied) {
                invalidFields.add(fieldName);
            }
        }

        if (!invalidFields.isEmpty()) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "healthProfileField 검증 실패: " + String.join(", ", invalidFields)
            );
        }
    }
}
