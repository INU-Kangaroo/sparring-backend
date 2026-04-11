package com.kangaroo.sparring.domain.recommendation.service.support;

import com.kangaroo.sparring.domain.common.type.MealTime;
import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.recommendation.dto.ml.MealRecommendationMlRequest;
import com.kangaroo.sparring.domain.record.common.read.BloodPressureRecord;
import com.kangaroo.sparring.domain.record.common.read.BloodSugarRecord;
import com.kangaroo.sparring.domain.record.common.read.FoodRecord;
import com.kangaroo.sparring.domain.survey.type.BloodPressureStatus;
import com.kangaroo.sparring.domain.survey.type.BloodSugarStatus;
import com.kangaroo.sparring.domain.survey.type.ExerciseFrequency;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.type.Gender;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MealRecommendationMlRequestFactory {

    private final Clock kstClock;

    public MealRecommendationMlRequest create(
            User user,
            HealthProfile profile,
            MealTime mealTime,
            List<BloodSugarRecord> bloodSugarRecords,
            List<BloodPressureRecord> bloodPressureRecords,
            List<FoodRecord> recentFoods
    ) {
        MealRecommendationMlRequest.HealthProfile healthProfile = new MealRecommendationMlRequest.HealthProfile(
                resolveSex(profile.getGender()),
                resolveAge(profile),
                profile.getHeight() != null ? profile.getHeight().doubleValue() : 170.0,
                profile.getWeight() != null ? profile.getWeight().doubleValue() : 65.0,
                resolveActivityLevel(profile.getExerciseFrequency()),
                resolveBloodSugarStatus(profile.getBloodSugarStatus()),
                resolveBloodPressureStatus(profile.getBloodPressureStatus()),
                false
        );

        MealRecommendationMlRequest.PreGlucose preGlucose = null;
        if (!bloodSugarRecords.isEmpty()) {
            preGlucose = new MealRecommendationMlRequest.PreGlucose(
                    bloodSugarRecords.get(0).getGlucoseLevel(),
                    30
            );
        }

        MealRecommendationMlRequest.LatestBloodPressure latestBloodPressure = null;
        if (!bloodPressureRecords.isEmpty()) {
            latestBloodPressure = new MealRecommendationMlRequest.LatestBloodPressure(
                    bloodPressureRecords.get(0).getSystolic(),
                    bloodPressureRecords.get(0).getDiastolic()
            );
        }

        List<String> recentFoodNames = recentFoods.stream()
                .map(FoodRecord::getFoodName)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .limit(10)
                .toList();

        return new MealRecommendationMlRequest(
                user.getId(),
                mealTime.name(),
                healthProfile,
                preGlucose,
                latestBloodPressure,
                recentFoodNames
        );
    }

    private String resolveActivityLevel(ExerciseFrequency freq) {
        if (freq == null) return "SEDENTARY";
        return switch (freq) {
            case ZERO -> "SEDENTARY";
            case ONE_TO_TWO -> "LIGHT";
            case TWO_TO_THREE, THREE_TO_FOUR -> "MODERATE";
            case FOUR_TO_FIVE, DAILY -> "ACTIVE";
        };
    }

    private String resolveBloodSugarStatus(BloodSugarStatus status) {
        if (status == null) return "NORMAL";
        return switch (status) {
            case TYPE1, TYPE2 -> "DIABETIC";
            case BORDERLINE -> "PRE_DIABETIC";
            default -> "NORMAL";
        };
    }

    private String resolveSex(Gender gender) {
        if (gender == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "성별 정보가 필요합니다.");
        }
        return gender == Gender.MALE ? "MALE" : "FEMALE";
    }

    private String resolveBloodPressureStatus(BloodPressureStatus status) {
        if (status == null) return "NORMAL";
        return switch (status) {
            case STAGE1, STAGE2 -> "HYPERTENSION";
            case BORDERLINE -> "PRE_HYPERTENSION";
            default -> "NORMAL";
        };
    }

    private int resolveAge(HealthProfile profile) {
        if (profile.getBirthDate() == null) return 30;
        return Math.max(0, Period.between(profile.getBirthDate(), LocalDate.now(kstClock)).getYears());
    }
}
