package com.kangaroo.sparring.domain.recommendation.repository;

import com.kangaroo.sparring.domain.recommendation.entity.FoodRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodRecommendationRepository extends JpaRepository<FoodRecommendation, Long> {

    List<FoodRecommendation> findByUser_IdOrderByRecommendedAtDesc(Long userId);
}
