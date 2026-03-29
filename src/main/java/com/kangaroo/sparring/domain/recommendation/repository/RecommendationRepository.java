package com.kangaroo.sparring.domain.recommendation.repository;

import com.kangaroo.sparring.domain.recommendation.entity.Recommendation;
import com.kangaroo.sparring.domain.recommendation.type.RecommendationType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Optional;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    Optional<Recommendation> findTopByUser_IdAndTypeAndFilterDurationAndFilterIntensityAndFilterLocationAndIsDeletedFalseAndCreatedAtAfterOrderByCreatedAtDesc(
            Long userId,
            RecommendationType type,
            String filterDuration,
            String filterIntensity,
            String filterLocation,
            LocalDateTime createdAt
    );

    Optional<Recommendation> findTopByUser_IdAndTypeAndIsDeletedFalseAndCreatedAtAfterOrderByCreatedAtDesc(
            Long userId,
            RecommendationType type,
            LocalDateTime createdAt
    );

    default Optional<Recommendation> findCachedExerciseRecommendation(
            Long userId,
            RecommendationType type,
            String filterDuration,
            String filterIntensity,
            String filterLocation,
            LocalDateTime createdAt
    ) {
        return findTopByUser_IdAndTypeAndFilterDurationAndFilterIntensityAndFilterLocationAndIsDeletedFalseAndCreatedAtAfterOrderByCreatedAtDesc(
                userId,
                type,
                filterDuration,
                filterIntensity,
                filterLocation,
                createdAt
        );
    }

    default Optional<Recommendation> findCachedRecommendation(
            Long userId,
            RecommendationType type,
            LocalDateTime createdAt
    ) {
        return findTopByUser_IdAndTypeAndIsDeletedFalseAndCreatedAtAfterOrderByCreatedAtDesc(
                userId,
                type,
                createdAt
        );
    }
}
