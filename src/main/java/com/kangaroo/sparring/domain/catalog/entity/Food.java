package com.kangaroo.sparring.domain.catalog.entity;

import com.kangaroo.sparring.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 음식 마스터 데이터 (기록용 전체 DB)
 * meal_nutrition 통합 — CSV 컬럼 최대한 보존
 */
@Entity
@Table(name = "food")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Food extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 식품 식별/분류
    @Column(name = "food_code", length = 64, unique = true)
    private String foodCode;

    @Column(name = "data_type", length = 20)
    private String dataType;

    @Column(name = "food_origin", length = 30)
    private String foodOrigin;

    @Column(name = "category_large", length = 50)
    private String categoryLarge;

    @Column(name = "category_medium", length = 50)
    private String categoryMedium;

    @Column(name = "category_small", length = 50)
    private String categorySmall;

    @Column(name = "rep_food_name", length = 100)
    private String repFoodName;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "nutrient_basis", length = 20)
    private String nutrientBasis;

    @Column(name = "ref_intake_amount", columnDefinition = "TEXT")
    private String refIntakeAmount;

    @Column(name = "food_weight", length = 50)
    private String foodWeight;

    @Column(name = "manufacturer", length = 255)
    private String manufacturer;

    @Column(name = "importer", length = 255)
    private String importer;

    @Column(name = "distributor", length = 255)
    private String distributor;

    // 영양소 (100g/100ml 기준)
    @Column(name = "calories")
    private Double calories;

    @Column(name = "water")
    private Double water;

    @Column(name = "protein")
    private Double protein;

    @Column(name = "fat")
    private Double fat;

    @Column(name = "carbs")
    private Double carbs;

    @Column(name = "sugar")
    private Double sugar;

    @Column(name = "fiber")
    private Double fiber;

    @Column(name = "calcium")
    private Double calcium;

    @Column(name = "iron")
    private Double iron;

    @Column(name = "potassium")
    private Double potassium;

    @Column(name = "sodium")
    private Double sodium;

    @Column(name = "cholesterol")
    private Double cholesterol;

    @Column(name = "saturated_fat")
    private Double saturatedFat;

    @Column(name = "trans_fat")
    private Double transFat;

    /**
     * 음식 생성
     */
    public static Food create(String name) {
        return Food.builder()
                .name(name)
                .build();
    }

    public void update(String name) {
        this.name = name;
    }

    public void updateDisplayMeta(String refIntakeAmount, String foodWeight, String manufacturer) {
        this.refIntakeAmount = refIntakeAmount;
        this.foodWeight = foodWeight;
        this.manufacturer = manufacturer;
    }
}
