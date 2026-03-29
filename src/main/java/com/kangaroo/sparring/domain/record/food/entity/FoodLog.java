package com.kangaroo.sparring.domain.record.food.entity;

import com.kangaroo.sparring.domain.common.BaseEntity;
import com.kangaroo.sparring.domain.catalog.entity.Food;
import com.kangaroo.sparring.domain.common.type.MealTime;
import com.kangaroo.sparring.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "food_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FoodLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_id")
    private Food food;

    @Column(name = "food_name", nullable = false, length = 255)
    private String foodName;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_time", nullable = false, length = 20)
    private MealTime mealTime;

    @Column(name = "eaten_at", nullable = false)
    private LocalDateTime eatenAt;

    @Column(name = "eaten_amount_gram")
    private Double eatenAmountGram;

    @Column(name = "calories")
    private Double calories;

    @Column(name = "carbs")
    private Double carbs;

    @Column(name = "protein")
    private Double protein;

    @Column(name = "fat")
    private Double fat;

    @Column(name = "sodium")
    private Double sodium;

    @Column(name = "sugar")
    private Double sugar;

    @Column(name = "fiber")
    private Double fiber;

    public static FoodLog withFood(User user, Food food, MealTime mealTime, LocalDateTime eatenAt, Double eatenAmountGram) {
        double ratio = eatenAmountGram / 100.0;

        return FoodLog.builder()
                .user(user)
                .food(food)
                .foodName(food.getName())
                .mealTime(mealTime)
                .eatenAt(eatenAt)
                .eatenAmountGram(eatenAmountGram)
                .calories(scaleNutrition(food.getCalories(), ratio))
                .carbs(scaleNutrition(food.getCarbs(), ratio))
                .protein(scaleNutrition(food.getProtein(), ratio))
                .fat(scaleNutrition(food.getFat(), ratio))
                .sodium(scaleNutrition(food.getSodium(), ratio))
                .sugar(scaleNutrition(food.getSugar(), ratio))
                .fiber(scaleNutrition(food.getFiber(), ratio))
                .build();
    }

    public static FoodLog withoutFood(User user, String foodName, MealTime mealTime, LocalDateTime eatenAt) {
        return FoodLog.builder()
                .user(user)
                .food(null)
                .foodName(foodName)
                .mealTime(mealTime)
                .eatenAt(eatenAt)
                .build();
    }

    private static Double scaleNutrition(Double value, double ratio) {
        if (value == null) return null;
        return value * ratio;
    }
}
