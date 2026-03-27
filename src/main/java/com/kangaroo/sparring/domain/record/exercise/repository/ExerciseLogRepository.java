package com.kangaroo.sparring.domain.record.exercise.repository;

import com.kangaroo.sparring.domain.record.exercise.entity.ExerciseLog;
import com.kangaroo.sparring.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ExerciseLogRepository extends JpaRepository<ExerciseLog, Long> {

    List<ExerciseLog> findByUserOrderByLoggedAtDesc(User user);

    List<ExerciseLog> findByUserIdAndLoggedAtBetweenAndIsDeletedFalse(
            Long userId,
            LocalDateTime from,
            LocalDateTime to
    );

    List<ExerciseLog> findByUserIdAndLoggedAtBetweenAndIsDeletedFalseOrderByLoggedAtDesc(
            Long userId,
            LocalDateTime from,
            LocalDateTime to
    );
}
