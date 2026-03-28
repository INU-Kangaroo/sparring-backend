package com.kangaroo.sparring.domain.recommendation.repository;

import com.kangaroo.sparring.domain.recommendation.entity.RecommendFood;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecommendFoodRepository extends JpaRepository<RecommendFood, Long> {

    Optional<RecommendFood> findByFoodCode(String foodCode);

    List<RecommendFood> findByCategoryLargeNotIn(List<String> excludedCategories);
}
