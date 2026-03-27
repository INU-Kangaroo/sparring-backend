package com.kangaroo.sparring.domain.record.food.repository;

import com.kangaroo.sparring.domain.record.food.entity.FoodLog;
import com.kangaroo.sparring.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FoodLogRepository extends JpaRepository<FoodLog, Long> {

    List<FoodLog> findByUserAndEatenAtBetweenAndIsDeletedFalseOrderByEatenAtAsc(
            User user,
            LocalDateTime start,
            LocalDateTime end
    );

    List<FoodLog> findByUserIdAndEatenAtBetweenAndIsDeletedFalse(
            Long userId,
            LocalDateTime start,
            LocalDateTime end
    );

    List<FoodLog> findByUserIdAndEatenAtBetweenAndIsDeletedFalseOrderByEatenAtAsc(
            Long userId,
            LocalDateTime start,
            LocalDateTime end
    );

    Optional<FoodLog> findByIdAndIsDeletedFalse(Long id);
}
