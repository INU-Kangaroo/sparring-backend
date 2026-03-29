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

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
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
    private final Clock kstClock;

    @Transactional
    public MealRecommendationResponse recommend(User user, HealthProfile profile, MealTime mealTime, int topN) {
        LocalDate today = currentDate();
        List<FoodRecord> todayFoodLogs = recordReadService.getTodayFoodRecords(user.getId(), today);

        List<BloodSugarRecord> recentBs = recordReadService.getRecentBloodSugarRecords(user.getId(), RECENT_BLOOD_SUGAR_COUNT);
        List<BloodPressureRecord> recentBp = recordReadService.getRecentBloodPressureRecords(user.getId(), RECENT_BLOOD_PRESSURE_COUNT);

        List<FoodRecord> historyLogs = recordReadService.getRecentFoodRecords(
                user.getId(), today.minusDays(HISTORY_DAYS), today.minusDays(1)
        );

        List<UserFoodFeedback> feedbacks = userFoodFeedbackRepository.findByUser_IdAndIsDeletedFalse(user.getId());

        Map<String, Object> requestBody = buildRequestBody(
                user, profile, mealTime, topN,
                todayFoodLogs, recentBs, recentBp, historyLogs, feedbacks
        );

        MealRecommendationAiClient.AiRecommendResult result = aiClient.recommend(requestBody);
        List<MealRecommendationResponse.RecommendedFoodDto> recommendations = enrichWithFoodId(result);
        return buildAndPersistResponse(user, mealTime, result, recommendations);
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

        body.put("allergies", parseAllergies(profile.getAllergies()));

        // 식품 선호 카테고리 (JSON 배열 문자열 → 리스트)
        body.put("food_preference", profile.getFoodPreference());

        return body;
    }

    private MealRecommendationResponse buildAndPersistResponse(
            User user,
            MealTime mealTime,
            MealRecommendationAiClient.AiRecommendResult result,
            List<MealRecommendationResponse.RecommendedFoodDto> recommendations
    ) {
        saveLog(user, mealTime, result);
        return MealRecommendationResponse.builder()
                .mealTime(mealTime.name())
                .recommendations(recommendations)
                .build();
    }

    private List<String> parseAllergies(String allergiesJson) {
        if (allergiesJson == null || allergiesJson.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(allergiesJson, new TypeReference<List<String>>() {});
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<MealRecommendationResponse.RecommendedFoodDto> enrichWithFoodId(
            MealRecommendationAiClient.AiRecommendResult result
    ) {
        List<String> foodCodes = result.foodCodes();
        List<MealRecommendationResponse.RecommendedFoodDto> recs = result.recommendations();
        List<String> nonNullFoodCodes = foodCodes.stream()
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .toList();

        // food_code -> Food 엔티티 (영양정보 포함) — 단건 쿼리
        Map<String, com.kangaroo.sparring.domain.catalog.entity.Food> codeToFood = new HashMap<>();
        if (!nonNullFoodCodes.isEmpty()) {
            foodRepository.findActiveByFoodCodeIn(nonNullFoodCodes)
                    .forEach(food -> codeToFood.put(food.getFoodCode(), food));
        }

        // food_code -> recommend_food (portionAmount 폴백용)
        Map<String, RecommendationFood> codeToRec = new HashMap<>();
        if (!nonNullFoodCodes.isEmpty()) {
            recommendationFoodRepository.findByFoodCodeIn(nonNullFoodCodes)
                    .forEach(rf -> codeToRec.put(rf.getFoodCode(), rf));
        }

        // foodId/영양정보 주입 (food 테이블 우선, 없으면 AI 응답 폴백)
        List<MealRecommendationResponse.RecommendedFoodDto> enriched = new ArrayList<>();
        for (int i = 0; i < recs.size(); i++) {
            MealRecommendationResponse.RecommendedFoodDto rec = recs.get(i);
            String code = i < foodCodes.size() ? foodCodes.get(i) : null;
            com.kangaroo.sparring.domain.catalog.entity.Food food = code != null ? codeToFood.get(code) : null;
            RecommendationFood rf = code != null ? codeToRec.get(code) : null;

            enriched.add(MealRecommendationResponse.RecommendedFoodDto.builder()
                    .foodId(food != null ? food.getId() : null)
                    .foodName(food != null ? food.getName() : rec.getFoodName())
                    .foodOrigin(food != null ? food.getFoodOrigin() : null)
                    .categoryLarge(food != null ? food.getCategoryLarge() : null)
                    .categoryMedium(food != null ? food.getCategoryMedium() : null)
                    .refIntakeAmount(food != null ? food.getRefIntakeAmount() : null)
                    .foodWeight(food != null ? food.getFoodWeight() : (rf != null ? rf.getFoodWeight() : null))
                    .calories(food != null ? food.getCalories() : rec.getCalories())
                    .carbs(food != null ? food.getCarbs() : rec.getCarbs())
                    .sugar(food != null ? food.getSugar() : null)
                    .fiber(food != null ? food.getFiber() : rec.getFiber())
                    .protein(food != null ? food.getProtein() : rec.getProtein())
                    .fat(food != null ? food.getFat() : rec.getFat())
                    .saturatedFat(food != null ? food.getSaturatedFat() : null)
                    .transFat(food != null ? food.getTransFat() : null)
                    .cholesterol(food != null ? food.getCholesterol() : null)
                    .sodium(food != null ? food.getSodium() : rec.getSodium())
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
        return Math.max(0, Period.between(profile.getBirthDate(), currentDate()).getYears());
    }

    @Transactional
    protected void saveLog(User user, MealTime mealTime, MealRecommendationAiClient.AiRecommendResult result) {
        try {
            FoodRecommendation entity = FoodRecommendation.builder()
                    .user(user)
                    .recommendedAt(LocalDateTime.now(kstClock))
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

    private LocalDate currentDate() {
        return LocalDate.now(kstClock);
    }
}
