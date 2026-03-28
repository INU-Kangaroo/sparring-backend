package com.kangaroo.sparring.domain.recommendation.entity;

import com.kangaroo.sparring.domain.common.BaseEntity;
import com.kangaroo.sparring.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 사용자별 음식 혈당 반응 피드백
 * 식전/식후 혈당 기반 EMA 가중치 누적
 */
@Entity
@Table(name = "user_food_feedback",
        indexes = {
                @Index(name = "idx_feedback_user_food", columnList = "user_id, food_code"),
                @Index(name = "idx_feedback_user_logged", columnList = "user_id, meal_logged_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserFoodFeedback extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "food_code", nullable = false, length = 64)
    private String foodCode;

    @Column(name = "meal_logged_at", nullable = false)
    private LocalDateTime mealLoggedAt;

    @Column(name = "blood_sugar_before")
    private Integer bloodSugarBefore;

    @Column(name = "blood_sugar_after")
    private Integer bloodSugarAfter;

    @Column(name = "glucose_delta")
    private Integer glucoseDelta;

    @Column(name = "measurement_gap_min")
    private Integer measurementGapMin;

    @Column(name = "feedback_weight", nullable = false)
    @Builder.Default
    private Double feedbackWeight = 0.0;

    @Column(name = "sample_count", nullable = false)
    @Builder.Default
    private Integer sampleCount = 0;

    /**
     * EMA 피드백 가중치 업데이트
     * α = 2 / (N + 1), N = 10
     * 신뢰도 낮은 샘플은 α 절반 적용
     */
    public void updateFeedback(double deltaScore, boolean lowReliability) {
        int n = 10;
        double alpha = 2.0 / (n + 1);
        if (lowReliability) alpha = alpha / 2.0;

        this.feedbackWeight = alpha * deltaScore + (1 - alpha) * this.feedbackWeight;
        this.sampleCount++;
    }
}
