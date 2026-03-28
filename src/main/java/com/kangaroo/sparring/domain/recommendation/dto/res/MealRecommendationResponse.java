package com.kangaroo.sparring.domain.recommendation.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "식단 추천 응답")
public class MealRecommendationResponse {

    @Schema(description = "식사 시간대", example = "BREAKFAST")
    private String mealTime;

    @Schema(description = "추천 음식 목록 (점수 상위순)")
    private List<RecommendedFoodDto> recommendations;

    @Getter
    @Builder
    @Schema(description = "추천 음식")
    public static class RecommendedFoodDto {

        @Schema(description = "음식 ID (혈당 예측 API 호출용)", example = "1")
        private Long foodId;

        @Schema(description = "식품명", example = "현미밥")
        private String foodName;

        @Schema(description = "칼로리 (kcal)", example = "150.0")
        private Double calories;

        @Schema(description = "1인분 라벨", example = "1인분")
        private String portionLabel;

        @Schema(description = "1인분 중량", example = "240g")
        private String portionAmount;

        @Schema(description = "탄수화물 (g)", example = "32.0")
        private Double carbs;

        @Schema(description = "단백질 (g)", example = "3.0")
        private Double protein;

        @Schema(description = "지방 (g)", example = "0.8")
        private Double fat;

        @Schema(description = "식이섬유 (g)", example = "1.2")
        private Double fiber;

        @Schema(description = "나트륨 (mg)", example = "2.0")
        private Double sodium;

        @Schema(description = "추천 이유", example = "[\"탄수화물이 낮아 혈당 부담이 적습니다\", \"식이섬유가 높아 포만감 유지에 도움이 됩니다\"]")
        private List<String> reasons;
    }
}
