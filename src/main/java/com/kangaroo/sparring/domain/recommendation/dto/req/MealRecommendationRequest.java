package com.kangaroo.sparring.domain.recommendation.dto.req;

import com.kangaroo.sparring.domain.common.type.MealTime;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Schema(description = "식단 추천 요청")
public class MealRecommendationRequest {

    @NotNull
    @Schema(
            description = "식사 시간대 (영문 enum 또는 한글 라벨 모두 허용: BREAKFAST/LUNCH/DINNER/SNACK 또는 아침/점심/저녁/간식)",
            example = "아침"
    )
    private MealTime mealTime;

    @Min(1) @Max(50)
    @Schema(description = "추천 개수 (1~50, 기본 10)", example = "10")
    private Integer topN = 10;
}
