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

        @Schema(description = "식품기원", example = "가정식")
        private String foodOrigin;

        @Schema(description = "대분류", example = "밥류")
        private String categoryLarge;

        @Schema(description = "중분류", example = "잡곡밥")
        private String categoryMedium;

        @Schema(description = "1회 섭취참고량", example = "210g")
        private String refIntakeAmount;

        @Schema(description = "식품중량", example = "210g")
        private String foodWeight;

        @Schema(description = "칼로리 (kcal)", example = "150.0")
        private Double calories;

        @Schema(description = "탄수화물 (g)", example = "32.0")
        private Double carbs;

        @Schema(description = "당류 (g)", example = "0.2")
        private Double sugar;

        @Schema(description = "식이섬유 (g)", example = "1.2")
        private Double fiber;

        @Schema(description = "단백질 (g)", example = "3.0")
        private Double protein;

        @Schema(description = "지방 (g)", example = "0.8")
        private Double fat;

        @Schema(description = "포화지방 (g)", example = "0.2")
        private Double saturatedFat;

        @Schema(description = "트랜스지방 (g)", example = "0.0")
        private Double transFat;

        @Schema(description = "콜레스테롤 (mg)", example = "0.0")
        private Double cholesterol;

        @Schema(description = "나트륨 (mg)", example = "2.0")
        private Double sodium;

        @Schema(description = "추천 이유", example = "[\"탄수화물이 낮아 혈당 부담이 적습니다\", \"식이섬유가 높아 포만감 유지에 도움이 됩니다\"]")
        private List<String> reasons;
    }
}
