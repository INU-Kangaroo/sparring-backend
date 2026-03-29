package com.kangaroo.sparring.domain.catalog.dto.res;

import com.kangaroo.sparring.domain.catalog.entity.Food;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
@Schema(description = "음식 상세 정보 응답 (영양 정보 포함)")
public class FoodDetailResponse {

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

    @Schema(description = "제조사명", example = "대한푸드텍(주)")
    private String manufacturer;

    @Schema(description = "칼로리 (kcal)", example = "165.0")
    private Double calories;

    @Schema(description = "탄수화물 (g)", example = "0.0")
    private Double carbs;

    @Schema(description = "당류 (g)", example = "0.0")
    private Double sugar;

    @Schema(description = "식이섬유 (g)", example = "0.0")
    private Double fiber;

    @Schema(description = "단백질 (g)", example = "31.0")
    private Double protein;

    @Schema(description = "지방 (g)", example = "3.6")
    private Double fat;

    @Schema(description = "포화지방 (g)", example = "1.0")
    private Double saturatedFat;

    @Schema(description = "트랜스지방 (g)", example = "0.0")
    private Double transFat;

    @Schema(description = "콜레스테롤 (mg)", example = "85.0")
    private Double cholesterol;

    @Schema(description = "나트륨 (mg)", example = "74.0")
    private Double sodium;

    @Schema(description = "칼슘 (mg)", example = "10.0")
    private Double calcium;

    @Schema(description = "철 (mg)", example = "0.5")
    private Double iron;

    @Schema(description = "칼륨 (mg)", example = "300.0")
    private Double potassium;

    public static FoodDetailResponse from(Food food) {
        return FoodDetailResponse.builder()
                .id(food.getId())
                .name(food.getName())
                .foodOrigin(food.getFoodOrigin())
                .categoryLarge(food.getCategoryLarge())
                .categoryMedium(food.getCategoryMedium())
                .refIntakeAmount(food.getRefIntakeAmount())
                .foodWeight(food.getFoodWeight())
                .manufacturer(food.getManufacturer())
                .calories(food.getCalories())
                .carbs(food.getCarbs())
                .sugar(food.getSugar())
                .fiber(food.getFiber())
                .protein(food.getProtein())
                .fat(food.getFat())
                .saturatedFat(food.getSaturatedFat())
                .transFat(food.getTransFat())
                .cholesterol(food.getCholesterol())
                .sodium(food.getSodium())
                .calcium(food.getCalcium())
                .iron(food.getIron())
                .potassium(food.getPotassium())
                .build();
    }
}
