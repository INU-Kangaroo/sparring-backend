package com.kangaroo.sparring.domain.recommendation.repository;

import com.kangaroo.sparring.domain.recommendation.entity.UserFoodFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFoodFeedbackRepository extends JpaRepository<UserFoodFeedback, Long> {

    Optional<UserFoodFeedback> findByUser_IdAndFoodCode(Long userId, String foodCode);

    List<UserFoodFeedback> findByUser_IdAndIsDeletedFalse(Long userId);
}
