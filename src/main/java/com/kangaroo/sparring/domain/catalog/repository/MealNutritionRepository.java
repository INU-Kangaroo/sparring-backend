package com.kangaroo.sparring.domain.catalog.repository;

import com.kangaroo.sparring.domain.catalog.entity.MealNutrition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MealNutritionRepository extends JpaRepository<MealNutrition, Long> {
}
