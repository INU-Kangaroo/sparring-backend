package com.kangaroo.sparring.domain.recommendation.service.usecase;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangaroo.sparring.domain.common.type.MealTime;
import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.recommendation.dto.ml.MealRecommendationMlRequest;
import com.kangaroo.sparring.domain.recommendation.dto.res.MealRecommendationResponse;
import com.kangaroo.sparring.domain.recommendation.entity.MealRecommendation;
import com.kangaroo.sparring.domain.recommendation.entity.MealRecommendationItem;
import com.kangaroo.sparring.domain.recommendation.repository.MealRecommendationRepository;
import com.kangaroo.sparring.domain.recommendation.service.client.MealRecommendationAiClient;
import com.kangaroo.sparring.domain.recommendation.service.support.MealRecommendationMlRequestFactory;
import com.kangaroo.sparring.domain.record.common.read.BloodPressureRecord;
import com.kangaroo.sparring.domain.record.common.read.BloodSugarRecord;
import com.kangaroo.sparring.domain.record.common.read.FoodRecord;
import com.kangaroo.sparring.domain.record.common.read.RecordReadService;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MealRecommendationService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int RECENT_BLOOD_SUGAR_COUNT = 1;
    private static final int RECENT_BLOOD_PRESSURE_COUNT = 1;
    private static final LocalTime BREAKFAST_END = LocalTime.of(11, 0);
    private static final LocalTime LUNCH_END = LocalTime.of(15, 0);
    private static final LocalTime DINNER_END = LocalTime.of(21, 0);

    private final RecordReadService recordReadService;
    private final MealRecommendationAiClient aiClient;
    private final MealRecommendationMlRequestFactory mlRequestFactory;
    private final MealRecommendationRepository mealRecommendationRepository;
    private final Clock kstClock;

    /**
     * 식단 추천 조회 — DB 캐시 있으면 반환, 없으면 FastAPI 호출
     */
    @Transactional
    public MealRecommendationResponse recommend(User user, HealthProfile profile, MealTime mealTime) {
        LocalDate today = LocalDate.now(kstClock);
        LocalDateTime now = LocalDateTime.now(kstClock);

        return mealRecommendationRepository
                .findTopByUser_IdAndMealTypeOrderByRecommendedAtDesc(user.getId(), mealTime.name())
                .map(cachedRecommendation -> toCachedResponseOrRefresh(
                        user, profile, mealTime, today, now, cachedRecommendation
                ))
                .orElseGet(() -> {
                    log.info("식단 추천 캐시 미스, 신규 생성: userId={}, mealType={}", user.getId(), mealTime.name());
                    return refresh(user, profile, mealTime);
                });
    }

    private MealRecommendationResponse toCachedResponseOrRefresh(
            User user,
            HealthProfile profile,
            MealTime mealTime,
            LocalDate today,
            LocalDateTime now,
            MealRecommendation cachedRecommendation
    ) {
        CacheInvalidationReason invalidationReason = resolveCacheInvalidationReason(
                user.getId(), profile, mealTime, today, now, cachedRecommendation
        );
        if (invalidationReason != CacheInvalidationReason.NONE) {
            logCacheInvalidation(invalidationReason, user, profile, mealTime, today, cachedRecommendation);
            return refresh(user, profile, mealTime);
        }

        try {
            log.info("식단 추천 캐시 반환: userId={}, mealType={}", user.getId(), mealTime.name());
            return toResponse(cachedRecommendation);
        } catch (Exception e) {
            log.warn("식단 추천 캐시 변환 실패, 재생성 시도 userId={}, mealType={}", user.getId(), mealTime.name(), e);
            return refresh(user, profile, mealTime);
        }
    }

    private CacheInvalidationReason resolveCacheInvalidationReason(
            Long userId,
            HealthProfile profile,
            MealTime mealTime,
            LocalDate today,
            LocalDateTime now,
            MealRecommendation cachedRecommendation
    ) {
        LocalDateTime recommendedAt = cachedRecommendation.getRecommendedAt();
        LocalDate cachedDate = recommendedAt != null ? recommendedAt.toLocalDate() : null;
        if (cachedDate == null || !cachedDate.isEqual(today)) {
            return CacheInvalidationReason.EXPIRED_DATE;
        }
        if (isProfileChangedSinceRecommendation(profile, recommendedAt)) {
            return CacheInvalidationReason.PROFILE_CHANGED;
        }
        if (hasFoodInputChangedAffectingMealType(userId, mealTime, recommendedAt)) {
            return CacheInvalidationReason.FOOD_INPUT_CHANGED;
        }
        if (isRemainingMealType(mealTime, now) && hasVitalsInputChangedSince(userId, recommendedAt)) {
            return CacheInvalidationReason.VITALS_INPUT_CHANGED;
        }
        return CacheInvalidationReason.NONE;
    }

    private void logCacheInvalidation(
            CacheInvalidationReason reason,
            User user,
            HealthProfile profile,
            MealTime mealTime,
            LocalDate today,
            MealRecommendation cachedRecommendation
    ) {
        LocalDateTime recommendedAt = cachedRecommendation.getRecommendedAt();
        switch (reason) {
            case EXPIRED_DATE -> {
                LocalDate cachedDate = recommendedAt != null ? recommendedAt.toLocalDate() : null;
                log.info("식단 추천 캐시 만료(당일 아님), 신규 생성: userId={}, mealType={}, cachedDate={}, today={}",
                        user.getId(), mealTime.name(), cachedDate, today);
            }
            case PROFILE_CHANGED -> log.info("식단 추천 캐시 무효화(프로필 변경): userId={}, mealType={}, recommendedAt={}, profileUpdatedAt={}",
                    user.getId(), mealTime.name(), recommendedAt, profile != null ? profile.getUpdatedAt() : null);
            case FOOD_INPUT_CHANGED -> log.info("식단 추천 캐시 무효화(식사 로그 변경): userId={}, mealType={}, recommendedAt={}",
                    user.getId(), mealTime.name(), recommendedAt);
            case VITALS_INPUT_CHANGED -> log.info("식단 추천 캐시 무효화(혈당/혈압 로그 변경): userId={}, mealType={}, recommendedAt={}",
                    user.getId(), mealTime.name(), recommendedAt);
            case NONE -> {
            }
        }
    }

    private boolean isProfileChangedSinceRecommendation(HealthProfile profile, LocalDateTime recommendedAt) {
        return profile != null
                && profile.getUpdatedAt() != null
                && recommendedAt != null
                && profile.getUpdatedAt().isAfter(recommendedAt);
    }

    private boolean hasFoodInputChangedAffectingMealType(Long userId, MealTime mealTime, LocalDateTime recommendedAt) {
        List<MealTime> affectingMealTimes = getAffectingFoodMealTimes(mealTime);
        return recordReadService.hasFoodInputChangedSince(userId, affectingMealTimes, recommendedAt);
    }

    private boolean hasVitalsInputChangedSince(Long userId, LocalDateTime recommendedAt) {
        return recordReadService.hasBloodSugarInputChangedSince(userId, recommendedAt)
                || recordReadService.hasBloodPressureInputChangedSince(userId, recommendedAt);
    }

    private boolean isRemainingMealType(MealTime targetMealType, LocalDateTime now) {
        MealTime currentMealSlot = resolveCurrentMealSlot(now.toLocalTime());
        return targetMealType.ordinal() >= currentMealSlot.ordinal();
    }

    private MealTime resolveCurrentMealSlot(LocalTime now) {
        if (now.isBefore(BREAKFAST_END)) {
            return MealTime.BREAKFAST;
        }
        if (now.isBefore(LUNCH_END)) {
            return MealTime.LUNCH;
        }
        if (now.isBefore(DINNER_END)) {
            return MealTime.DINNER;
        }
        return MealTime.SNACK;
    }

    private List<MealTime> getAffectingFoodMealTimes(MealTime targetMealType) {
        return Arrays.stream(MealTime.values())
                .filter(mealTime -> mealTime.ordinal() < targetMealType.ordinal())
                .toList();
    }

    /**
     * 식단 추천 새로고침 — FastAPI 호출 후 DB 저장
     */
    @Transactional
    public MealRecommendationResponse refresh(User user, HealthProfile profile, MealTime mealTime) {
        long startedAt = System.currentTimeMillis();
        log.info("식단 추천 강제 새로고침/생성 요청: userId={}, mealType={}", user.getId(), mealTime.name());
        LocalDate today = LocalDate.now(kstClock);

        List<BloodSugarRecord> recentBs = recordReadService.getRecentBloodSugarRecords(user.getId(), RECENT_BLOOD_SUGAR_COUNT);
        List<BloodPressureRecord> recentBp = recordReadService.getRecentBloodPressureRecords(user.getId(), RECENT_BLOOD_PRESSURE_COUNT);
        List<FoodRecord> recentFoods = recordReadService.getRecentFoodRecords(user.getId(), today.minusDays(3), today);

        log.info("식단 추천 AI 요청 시작: userId={}, mealType={}, bloodSugarLogs={}, bloodPressureLogs={}, recentFoods={}",
                user.getId(), mealTime.name(), recentBs.size(), recentBp.size(), recentFoods.size());
        MealRecommendationMlRequest requestBody = mlRequestFactory.create(user, profile, mealTime, recentBs, recentBp, recentFoods);
        MealRecommendationAiClient.AiRecommendResult result = aiClient.recommend(requestBody);
        log.info("식단 추천 AI 응답 수신: userId={}, mealType={}, cards={}",
                user.getId(), mealTime.name(), result.cards() == null ? 0 : result.cards().size());

        MealRecommendation saved = saveRecommendation(user, mealTime, result);
        log.info("식단 추천 저장 완료: userId={}, mealType={}, recommendationId={}, elapsedMs={}",
                user.getId(), mealTime.name(), saved.getId(), System.currentTimeMillis() - startedAt);
        return toResponse(saved);
    }

    private MealRecommendation saveRecommendation(
            User user, MealTime mealTime, MealRecommendationAiClient.AiRecommendResult result
    ) {
        if (result.cards() == null || result.cards().isEmpty()) {
            throw new CustomException(ErrorCode.AI_RECOMMENDATION_FAILED, "추천 카드가 비어 있습니다.");
        }

        MealRecommendation recommendation = MealRecommendation.builder()
                .user(user)
                .mealType(mealTime.name())
                .mealTargetKcal(BigDecimal.valueOf(result.mealTargetKcal()))
                .recommendedAt(LocalDateTime.now(kstClock))
                .build();

        for (MealRecommendationResponse.RecommendationCardDto card : result.cards()) {
            if (card.getNutrients() == null) {
                throw new CustomException(ErrorCode.AI_RECOMMENDATION_FAILED, "추천 카드 영양 정보가 누락되었습니다.");
            }

            try {
                MealRecommendationItem item = MealRecommendationItem.builder()
                        .mealRecommendation(recommendation)
                        .rankOrder(card.getRank())
                        .title(card.getTitle())
                        .totalKcal(BigDecimal.valueOf(card.getNutrients().getKcal()))
                        .totalCarbs(BigDecimal.valueOf(card.getNutrients().getCarbs()))
                        .totalProtein(BigDecimal.valueOf(card.getNutrients().getProtein()))
                        .totalFat(BigDecimal.valueOf(card.getNutrients().getFat()))
                        .totalSodium(card.getNutrients().getSodium() != null
                                ? BigDecimal.valueOf(card.getNutrients().getSodium()) : null)
                        .reasonsJson(OBJECT_MAPPER.writeValueAsString(card.getReasons()))
                        .menusJson(OBJECT_MAPPER.writeValueAsString(card.getMenus()))
                        .build();
                recommendation.getItems().add(item);
            } catch (CustomException e) {
                throw e;
            } catch (Exception e) {
                throw new CustomException(ErrorCode.AI_RECOMMENDATION_FAILED, "추천 카드 저장 중 오류가 발생했습니다.");
            }
        }

        if (recommendation.getItems().isEmpty()) {
            throw new CustomException(ErrorCode.AI_RECOMMENDATION_FAILED, "저장 가능한 추천 카드가 없습니다.");
        }

        return mealRecommendationRepository.save(recommendation);
    }

    private MealRecommendationResponse toResponse(MealRecommendation rec) {
        if (rec.getItems() == null || rec.getItems().isEmpty()) {
            throw new CustomException(ErrorCode.AI_RECOMMENDATION_FAILED, "캐시된 추천 카드가 없습니다.");
        }

        List<MealRecommendationResponse.RecommendationCardDto> cards = rec.getItems().stream()
                .sorted((a, b) -> Integer.compare(a.getRankOrder(), b.getRankOrder()))
                .map(item -> {
                    try {
                        List<String> reasons = OBJECT_MAPPER.readValue(
                                item.getReasonsJson(), new TypeReference<>() {});
                        List<MealRecommendationResponse.MenuItemDto> menus = OBJECT_MAPPER.readValue(
                                item.getMenusJson(), new TypeReference<>() {});

                        return MealRecommendationResponse.RecommendationCardDto.builder()
                                .recommendationCardId(item.getId())
                                .rank(item.getRankOrder())
                                .title(item.getTitle())
                                .nutrients(MealRecommendationResponse.NutrientsDto.builder()
                                        .kcal(item.getTotalKcal() != null ? item.getTotalKcal().doubleValue() : 0)
                                        .carbs(item.getTotalCarbs() != null ? item.getTotalCarbs().doubleValue() : 0)
                                        .protein(item.getTotalProtein() != null ? item.getTotalProtein().doubleValue() : 0)
                                        .fat(item.getTotalFat() != null ? item.getTotalFat().doubleValue() : 0)
                                        .sodium(item.getTotalSodium() != null ? item.getTotalSodium().doubleValue() : null)
                                        .build())
                                .reasons(reasons)
                                .menus(menus)
                                .build();
                    } catch (Exception e) {
                        throw new CustomException(ErrorCode.AI_RECOMMENDATION_FAILED, "추천 카드 변환 중 오류가 발생했습니다.");
                    }
                })
                .toList();

        return MealRecommendationResponse.builder()
                .recommendationId(rec.getId())
                .mealType(rec.getMealType())
                .recommendations(cards)
                .build();
    }

    private enum CacheInvalidationReason {
        NONE,
        EXPIRED_DATE,
        PROFILE_CHANGED,
        FOOD_INPUT_CHANGED,
        VITALS_INPUT_CHANGED
    }

}
