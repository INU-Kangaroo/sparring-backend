package com.kangaroo.sparring.domain.recommendation.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MealRecommendationMlRequest(
        @JsonProperty("user_id") Long userId,
        @JsonProperty("meal_type") String mealType,
        @JsonProperty("health_profile") HealthProfile healthProfile,
        @JsonProperty("pre_glucose") PreGlucose preGlucose,
        @JsonProperty("latest_blood_pressure") LatestBloodPressure latestBloodPressure,
        @JsonProperty("recent_foods") List<String> recentFoods
) {
    public record HealthProfile(
            @JsonProperty("sex") String sex,
            @JsonProperty("age") int age,
            @JsonProperty("height_cm") double heightCm,
            @JsonProperty("weight_kg") double weightKg,
            @JsonProperty("activity_level") String activityLevel,
            @JsonProperty("blood_sugar_status") String bloodSugarStatus,
            @JsonProperty("blood_pressure_status") String bloodPressureStatus,
            @JsonProperty("has_dyslipidemia") boolean hasDyslipidemia
    ) {}

    public record PreGlucose(
            @JsonProperty("value_mg_dl") double valueMgDl,
            @JsonProperty("minutes_before_meal") int minutesBeforeMeal
    ) {}

    public record LatestBloodPressure(
            @JsonProperty("systolic") int systolic,
            @JsonProperty("diastolic") int diastolic
    ) {}
}
