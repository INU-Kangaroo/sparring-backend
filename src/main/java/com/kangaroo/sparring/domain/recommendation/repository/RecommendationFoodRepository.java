package com.kangaroo.sparring.domain.recommendation.repository;

import com.kangaroo.sparring.domain.recommendation.entity.RecommendationFood;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecommendationFoodRepository extends JpaRepository<RecommendationFood, Long> {

    Optional<RecommendationFood> findByFoodCode(String foodCode);

    List<RecommendationFood> findByFoodCodeIn(List<String> foodCodes);

    List<RecommendationFood> findByCategoryLargeNotIn(List<String> excludedCategories);
}
