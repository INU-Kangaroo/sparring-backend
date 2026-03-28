package com.kangaroo.sparring.domain.recommendation.entity;

import com.kangaroo.sparring.domain.common.BaseEntity;
import com.kangaroo.sparring.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 식단 추천 로그
 * 추천 시 적용된 제약/기여도/이유 코드 저장 (디버깅/분석용)
 */
@Entity
@Table(name = "meal_recommendation_log",
        indexes = {
                @Index(name = "idx_rec_log_user_time", columnList = "user_id, recommended_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MealRecommendationLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "recommended_at", nullable = false)
    private LocalDateTime recommendedAt;

    @Column(name = "meal_time", nullable = false, length = 10)
    private String mealTime;

    /**
     * Fallback 단계
     * 0=정상, 1=당류완화, 2=나트륨완화, 3=중복허용
     */
    @Column(name = "fallback_level", nullable = false)
    @Builder.Default
    private Integer fallbackLevel = 0;

    /**
     * 적용된 제약 목록
     * ex) ["sugar_limit", "sodium_limit", "meal_time_dinner"]
     */
    @Column(name = "applied_constraints", columnDefinition = "JSON")
    private String appliedConstraints;

    /**
     * 영양소별 점수 기여도
     * ex) {"carbs": 0.42, "fiber": 0.28, "protein": 0.18}
     */
    @Column(name = "feature_contrib", columnDefinition = "JSON")
    private String featureContrib;

    /**
     * 추천 이유 코드
     * ex) ["LOW_CARB", "HIGH_FIBER", "PROTEIN_SUPPLEMENT"]
     */
    @Column(name = "reason_codes", columnDefinition = "JSON")
    private String reasonCodes;

    /**
     * 추천된 food_code 목록
     */
    @Column(name = "food_codes", columnDefinition = "JSON")
    private String foodCodes;
}
