package com.kangaroo.sparring.domain.record.common.read;

import com.kangaroo.sparring.domain.record.blood.repository.BloodPressureLogRepository;
import com.kangaroo.sparring.domain.record.blood.repository.BloodSugarLogRepository;
import com.kangaroo.sparring.domain.record.exercise.repository.ExerciseLogRepository;
import com.kangaroo.sparring.domain.record.food.repository.FoodLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecordReadService {

    private final BloodSugarLogRepository bloodSugarLogRepository;
    private final BloodPressureLogRepository bloodPressureLogRepository;
    private final FoodLogRepository foodLogRepository;
    private final ExerciseLogRepository exerciseLogRepository;

    public List<BloodSugarRecord> getBloodSugarRecords(Long userId, LocalDateTime start, LocalDateTime end) {
        return bloodSugarLogRepository.findByUserIdAndDateRange(userId, start, end).stream()
                .map(log -> new BloodSugarRecord(log.getGlucoseLevel(), log.getMeasurementTime(), log.getMeasurementLabel()))
                .toList();
    }

    public List<BloodPressureRecord> getBloodPressureRecords(Long userId, LocalDateTime start, LocalDateTime end) {
        return bloodPressureLogRepository.findByUserIdAndDateRange(userId, start, end).stream()
                .map(log -> new BloodPressureRecord(log.getSystolic(), log.getDiastolic(), log.getMeasuredAt()))
                .toList();
    }

    public List<FoodRecord> getFoodRecords(Long userId, LocalDateTime start, LocalDateTime end) {
        return foodLogRepository.findByUserIdAndEatenAtBetweenAndIsDeletedFalseOrderByEatenAtAsc(userId, start, end).stream()
<<<<<<< Updated upstream
                .map(log -> new FoodRecord(log.getEatenAt(), log.getCalories()))
                .toList();
    }

=======
                .map(log -> new FoodRecord(
                        log.getEatenAt(),
                        log.getFoodName(),
                        log.getCalories(),
                        log.getCarbs(),
                        log.getProtein(),
                        log.getFat(),
                        log.getSodium(),
                        null,          // sugar (food_log에 없음)
                        log.getFiber()
                ))
                .toList();
    }

    public List<FoodRecord> getTodayFoodRecords(Long userId, LocalDate today) {
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);
        return getFoodRecords(userId, start, end);
    }

    public List<FoodRecord> getRecentFoodRecords(Long userId, LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);
        return getFoodRecords(userId, start, end);
    }

>>>>>>> Stashed changes
    public List<ExerciseRecord> getExerciseRecords(Long userId, LocalDateTime start, LocalDateTime end) {
        return exerciseLogRepository.findByUserIdAndLoggedAtBetweenAndIsDeletedFalseOrderByLoggedAtDesc(userId, start, end).stream()
                .map(log -> new ExerciseRecord(log.getLoggedAt(), log.getMetValue()))
                .toList();
    }

    public List<BloodSugarRecord> getRecentBloodSugarRecords(Long userId, int count) {
        return bloodSugarLogRepository.findRecentByUserId(userId, PageRequest.of(0, count)).stream()
                .map(log -> new BloodSugarRecord(log.getGlucoseLevel(), log.getMeasurementTime(), log.getMeasurementLabel()))
                .toList();
    }

    public List<BloodPressureRecord> getRecentBloodPressureRecords(Long userId, int count) {
        return bloodPressureLogRepository.findRecentByUserId(userId, PageRequest.of(0, count)).stream()
                .map(log -> new BloodPressureRecord(log.getSystolic(), log.getDiastolic(), log.getMeasuredAt()))
                .toList();
    }

    public List<ExerciseRecord> getTodayExerciseRecords(Long userId, LocalDate today) {
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);
        return getExerciseRecords(userId, start, end);
    }

    public Long countBloodSugarRecords(Long userId) {
        return bloodSugarLogRepository.countByUserId(userId);
    }

    public Double getAverageBloodSugar(Long userId) {
        return bloodSugarLogRepository.findAverageGlucoseByUserId(userId);
    }

    public Double getAverageBloodSugar(Long userId, LocalDateTime start, LocalDateTime end) {
        return bloodSugarLogRepository.findAverageGlucoseByUserIdAndDateRange(userId, start, end);
    }

    public List<BloodSugarRecord> getBloodSugarRecordsDesc(Long userId) {
        return bloodSugarLogRepository.findByUserIdAndIsDeletedFalseOrderByMeasurementTimeDesc(userId).stream()
                .map(log -> new BloodSugarRecord(log.getGlucoseLevel(), log.getMeasurementTime(), log.getMeasurementLabel()))
                .toList();
    }
}
