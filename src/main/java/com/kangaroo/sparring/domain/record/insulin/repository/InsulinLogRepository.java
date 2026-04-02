package com.kangaroo.sparring.domain.record.insulin.repository;

import com.kangaroo.sparring.domain.record.insulin.entity.InsulinLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InsulinLogRepository extends JpaRepository<InsulinLog, Long> {

    @Query("SELECT i FROM InsulinLog i " +
            "WHERE i.user.id = :userId " +
            "AND i.usedAt BETWEEN :startDate AND :endDate " +
            "AND i.isDeleted = false " +
            "ORDER BY i.usedAt ASC")
    List<InsulinLog> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT i FROM InsulinLog i " +
            "WHERE i.user.id = :userId " +
            "AND i.usedAt <= :baseTime " +
            "AND i.isDeleted = false " +
            "ORDER BY i.usedAt DESC")
    List<InsulinLog> findRecentByUserIdBefore(
            @Param("userId") Long userId,
            @Param("baseTime") LocalDateTime baseTime,
            Pageable pageable
    );
}
