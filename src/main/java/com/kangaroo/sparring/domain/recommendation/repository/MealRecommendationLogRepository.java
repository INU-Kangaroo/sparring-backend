package com.kangaroo.sparring.domain.recommendation.repository;

import com.kangaroo.sparring.domain.recommendation.entity.MealRecommendationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MealRecommendationLogRepository extends JpaRepository<MealRecommendationLog, Long> {

    List<MealRecommendationLog> findByUserIdOrderByRecommendedAtDesc(Long userId);
}
