package com.kangaroo.sparring.domain.recommendation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangaroo.sparring.domain.catalog.repository.FoodRepository;
import com.kangaroo.sparring.domain.common.type.MealTime;
import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.recommendation.dto.res.MealRecommendationResponse;
import com.kangaroo.sparring.domain.recommendation.entity.FoodRecommendation;
import com.kangaroo.sparring.domain.recommendation.entity.RecommendationFood;
import com.kangaroo.sparring.domain.recommendation.entity.UserFoodFeedback;
import com.kangaroo.sparring.domain.recommendation.repository.FoodRecommendationRepository;
import com.kangaroo.sparring.domain.recommendation.repository.RecommendationFoodRepository;
import com.kangaroo.sparring.domain.recommendation.repository.UserFoodFeedbackRepository;
import com.kangaroo.sparring.domain.record.common.read.BloodPressureRecord;
import com.kangaroo.sparring.domain.record.common.read.BloodSugarRecord;
import com.kangaroo.sparring.domain.record.common.read.FoodRecord;
import com.kangaroo.sparring.domain.record.common.read.RecordReadService;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.type.Gender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MealRecommendationService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final int RECENT_BLOOD_SUGAR_COUNT = 1;
    private static final int RECENT_BLOOD_PRESSURE_COUNT = 1;
    private static final int HISTORY_DAYS = 7;

    private final RecordReadService recordReadService;
    private final UserFoodFeedbackRepository userFoodFeedbackRepository;
    private final FoodRecommendationRepository foodRecommendationRepository;
    private final RecommendationFoodRepository recommendationFoodRepository;
    private final MealRecommendationAiClient aiClient;
    private final FoodRepository foodRepository;

    @Transactional
    public MealRecommendationResponse recommend(User user, HealthProfile profile, MealTime mealTime, int topN) {

        // 1. 오늘 식사 기록 조회
        LocalDate today = LocalDate.now();
        List<FoodRecord> todayFoodLogs = recordReadService.getTodayFoodRecords(user.getId(), today);

        // 2. 최근 혈당/혈압 조회
        List<BloodSugarRecord> recentBs = recordReadService.getRecentBloodSugarRecords(user.getId(), RECENT_BLOOD_SUGAR_COUNT);
        List<BloodPressureRecord> recentBp = recordReadService.getRecentBloodPressureRecords(user.getId(), RECENT_BLOOD_PRESSURE_COUNT);

        // 3. 최근 7일 식사 히스토리 (오늘 제외)
        List<FoodRecord> historyLogs = recordReadService.getRecentFoodRecords(
                user.getId(), today.minusDays(HISTORY_DAYS), today.minusDays(1)
        );

        // 4. 개인 피드백 가중치 조회
        List<UserFoodFeedback> feedbacks = userFoodFeedbackRepository.findByUser_IdAndIsDeletedFalse(user.getId());

        // 5. AI 서버 요청 바디 조립
        Map<String, Object> requestBody = buildRequestBody(
                user, profile, mealTime, topN,
                todayFoodLogs, recentBs, recentBp, historyLogs, feedbacks
        );

        // 6. AI 서버 호출
        MealRecommendationAiClient.AiRecommendResult result = aiClient.recommend(requestBody);

        // 7. food_code → food.id 매핑
        List<MealRecommendationResponse.RecommendedFoodDto> recommendations = enrichWithFoodId(result);

        // 8. 추천 로그 저장
        saveLog(user, mealTime, result);

        // 9. 응답 반환
        return MealRecommendationResponse.builder()
                .mealTime(mealTime.name())
                .recommendations(recommendations)
                .build();
    }

    private Map<String, Object> buildRequestBody(
            User user, HealthProfile profile, MealTime mealTime, int topN,
            List<FoodRecord> todayLogs, List<BloodSugarRecord> bsLogs,
            List<BloodPressureRecord> bpLogs, List<FoodRecord> historyLogs,
            List<UserFoodFeedback> feedbacks
    ) {
        Map<String, Object> body = new HashMap<>();

        // 사용자 기본 정보
        body.put("sex", resolveSex(profile.getGender()));
        body.put("age_years", resolveAge(profile));
        body.put("height_cm", profile.getHeight() != null ? profile.getHeight().doubleValue() : 170.0);
        body.put("weight_kg", profile.getWeight() != null ? profile.getWeight().doubleValue() : 65.0);

        // 최근 혈당/혈압
        body.put("blood_glucose", bsLogs.isEmpty() ? 100.0 : (double) bsLogs.get(0).getGlucoseLevel());
        body.put("sbp", bpLogs.isEmpty() ? 120 : bpLogs.get(0).getSystolic());
        body.put("dbp", bpLogs.isEmpty() ? 80 : bpLogs.get(0).getDiastolic());

        // 식사 시간대 + 추천 개수
        body.put("meal_time", mealTime.getLabel());
        body.put("top_n", topN);

        // 오늘 섭취 집계
        body.put("eaten_foods", todayLogs.stream()
                .map(FoodRecord::getFoodName)
                .filter(n -> n != null && !n.isBlank())
                .toList());
        body.put("eaten_kcal", sumNutrition(todayLogs, "calories"));
        body.put("eaten_carb_g", sumNutrition(todayLogs, "carbs"));
        body.put("eaten_protein_g", sumNutrition(todayLogs, "protein"));
        body.put("eaten_fat_g", sumNutrition(todayLogs, "fat"));
        body.put("eaten_sugar_g", sumNutrition(todayLogs, "sugar"));
        body.put("eaten_fiber_g", sumNutrition(todayLogs, "fiber"));
        body.put("eaten_sodium_mg", sumNutrition(todayLogs, "sodium"));

        // 최근 7일 히스토리
        body.put("history", historyLogs.stream()
                .map(FoodRecord::getFoodName)
                .filter(n -> n != null && !n.isBlank())
                .distinct()
                .toList());

        // 개인 피드백 가중치 {food_code: weight}
        Map<String, Double> feedbackMap = new HashMap<>();
        feedbacks.forEach(f -> feedbackMap.put(f.getFoodCode(), f.getFeedbackWeight()));
        body.put("feedback_weights", feedbackMap);

        // 알레르기 (JSON 배열 문자열 -> 리스트)
        List<String> allergies = new ArrayList<>();
        if (profile.getAllergies() != null && !profile.getAllergies().isBlank()) {
            try {
                allergies = OBJECT_MAPPER.readValue(
                        profile.getAllergies(),
                        new TypeReference<List<String>>() {}
                );
            } catch (Exception ignored) {
                allergies = List.of();
            }
        }
        body.put("allergies", allergies);

        // 식품 선호 카테고리 (JSON 배열 문자열 → 리스트)
        body.put("food_preference", profile.getFoodPreference());

        return body;
    }

    private List<MealRecommendationResponse.RecommendedFoodDto> enrichWithFoodId(
            MealRecommendationAiClient.AiRecommendResult result
    ) {
        List<String> foodCodes = result.foodCodes();
        List<MealRecommendationResponse.RecommendedFoodDto> recs = result.recommendations();

        // food_code → food.id 캐시
        Map<String, Long> codeToId = new HashMap<>();
        for (String code : foodCodes) {
            foodRepository.findIdByFoodCode(code).ifPresent(id -> codeToId.put(code, id));
        }

        // food_code -> recommend_food (portionAmount용)
        Map<String, RecommendationFood> codeToRec = new HashMap<>();
        recommendationFoodRepository.findByFoodCodeIn(foodCodes)
                .forEach(rf -> codeToRec.put(rf.getFoodCode(), rf));

        // foodId/portion 정보 주입 (index 기준으로 food_code와 매핑)
        List<MealRecommendationResponse.RecommendedFoodDto> enriched = new ArrayList<>();
        for (int i = 0; i < recs.size(); i++) {
            MealRecommendationResponse.RecommendedFoodDto rec = recs.get(i);
            String code = i < foodCodes.size() ? foodCodes.get(i) : null;
            Long foodId = code != null ? codeToId.get(code) : null;
            RecommendationFood rf = code != null ? codeToRec.get(code) : null;

            String portionAmount = rf != null ? rf.getFoodWeight() : null;

            enriched.add(MealRecommendationResponse.RecommendedFoodDto.builder()
                    .foodId(foodId)
                    .foodName(rec.getFoodName())
                    .calories(rec.getCalories())
                    .portionLabel("1인분")
                    .portionAmount(portionAmount)
                    .carbs(rec.getCarbs())
                    .protein(rec.getProtein())
                    .fat(rec.getFat())
                    .fiber(rec.getFiber())
                    .sodium(rec.getSodium())
                    .reasons(rec.getReasons())
                    .build());
        }
        return enriched;
    }

    private double sumNutrition(List<FoodRecord> logs, String field) {
        return logs.stream()
                .mapToDouble(log -> {
                    Double val = switch (field) {
                        case "calories" -> log.getCalories();
                        case "carbs"    -> log.getCarbs();
                        case "protein"  -> log.getProtein();
                        case "fat"      -> log.getFat();
                        case "sugar"    -> log.getSugar();
                        case "fiber"    -> log.getFiber();
                        case "sodium"   -> log.getSodium();
                        default         -> null;
                    };
                    return val != null ? val : 0.0;
                })
                .sum();
    }

    private String resolveSex(Gender gender) {
        if (gender == null) return "남";
        return gender == Gender.MALE ? "남" : "여";
    }

    private int resolveAge(HealthProfile profile) {
        if (profile.getBirthDate() == null) return 30;
        return LocalDate.now().getYear() - profile.getBirthDate().getYear();
    }

    @Transactional
    protected void saveLog(User user, MealTime mealTime, MealRecommendationAiClient.AiRecommendResult result) {
        try {
            FoodRecommendation entity = FoodRecommendation.builder()
                    .user(user)
                    .recommendedAt(LocalDateTime.now())
                    .mealTime(mealTime.name())
                    .fallbackLevel(result.fallbackLevel())
                    .appliedConstraints(result.appliedConstraints())
                    .featureContrib(result.featureContrib())
                    .reasonCodes(result.reasonCodes())
                    .foodCodes(result.foodCodesJson())
                    .build();
            foodRecommendationRepository.save(entity);
        } catch (Exception e) {
            log.warn("식단 추천 로그 저장 실패 (무시)", e);
        }
    }
}
