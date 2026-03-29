package com.kangaroo.sparring.domain.catalog.dto.res;

import com.kangaroo.sparring.domain.catalog.entity.Food;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
@Schema(description = "음식 기본 정보 응답")
public class FoodResponse {

    @Schema(description = "음식 ID", example = "1")
    private Long id;

    @Schema(description = "음식명", example = "닭가슴살")
    private String name;

    @Schema(description = "식품기원", example = "가정식")
    private String foodOrigin;

    @Schema(description = "대분류", example = "육류")
    private String categoryLarge;

    @Schema(description = "중분류", example = "닭고기")
    private String categoryMedium;

    @Schema(description = "1회 섭취참고량", example = "100g")
    private String refIntakeAmount;

    @Schema(description = "식품중량", example = "100g")
    private String foodWeight;

    @Schema(description = "칼로리 (kcal)", example = "165.0")
    private Double calories;

    @Schema(description = "제조사명", example = "대한푸드텍(주)")
    private String manufacturer;

    public static FoodResponse from(Food food) {
        return FoodResponse.builder()
                .id(food.getId())
                .name(food.getName())
                .foodOrigin(food.getFoodOrigin())
                .categoryLarge(food.getCategoryLarge())
                .categoryMedium(food.getCategoryMedium())
                .refIntakeAmount(food.getRefIntakeAmount())
                .foodWeight(food.getFoodWeight())
                .calories(food.getCalories())
                .manufacturer(food.getManufacturer())
                .build();
    }
}
