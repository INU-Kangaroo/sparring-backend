package com.kangaroo.sparring.domain.recommendation.entity;

import com.kangaroo.sparring.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 추천용 음식 데이터
 * food 테이블과 동일 구조, 빵/음료 제외 ~5,000건
 */
@Entity
@Table(name = "recommend_food")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RecommendationFood extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "food_code", nullable = false, length = 64, unique = true)
    private String foodCode;

    @Column(name = "data_type", length = 20)
    private String dataType;

    @Column(name = "category_large", length = 50)
    private String categoryLarge;

    @Column(name = "category_medium", length = 50)
    private String categoryMedium;

    @Column(name = "rep_food_name", length = 100)
    private String repFoodName;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "basis_amount", length = 20)
    private String basisAmount;

    @Column(name = "ref_serving_size", length = 50)
    private String refServingSize;

    @Column(name = "food_weight", length = 50)
    private String foodWeight;

    @Column(name = "manufacturer", length = 255)
    private String manufacturer;

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
}
