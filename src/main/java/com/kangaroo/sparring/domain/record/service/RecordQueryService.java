package com.kangaroo.sparring.domain.record.service;

import com.kangaroo.sparring.domain.exercise.log.dto.res.ExerciseLogListItemResponse;
import com.kangaroo.sparring.domain.exercise.log.repository.ExerciseLogRepository;
import com.kangaroo.sparring.domain.food.log.dto.res.FoodLogListItemResponse;
import com.kangaroo.sparring.domain.food.log.repository.FoodLogRepository;
import com.kangaroo.sparring.domain.measurement.dto.res.BloodPressureLogResponse;
import com.kangaroo.sparring.domain.measurement.dto.res.BloodSugarLogResponse;
import com.kangaroo.sparring.domain.measurement.service.BloodPressureService;
import com.kangaroo.sparring.domain.measurement.service.BloodSugarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecordQueryService {

    private final BloodSugarService bloodSugarService;
    private final BloodPressureService bloodPressureService;
    private final FoodLogRepository foodLogRepository;
    private final ExerciseLogRepository exerciseLogRepository;

    public List<BloodSugarLogResponse> getBloodSugarRecords(Long userId, LocalDateTime start, LocalDateTime end) {
        return bloodSugarService.getBloodSugarLogs(userId, start, end);
    }

    public List<BloodPressureLogResponse> getBloodPressureRecords(Long userId, LocalDateTime start, LocalDateTime end) {
        return bloodPressureService.getBloodPressureLogs(userId, start, end);
    }

    public List<FoodLogListItemResponse> getFoodRecords(Long userId, LocalDateTime start, LocalDateTime end) {
        return foodLogRepository.findByUserIdAndEatenAtBetweenAndIsDeletedFalse(userId, start, end).stream()
                .map(FoodLogListItemResponse::from)
                .toList();
    }

    public List<ExerciseLogListItemResponse> getExerciseRecords(Long userId, LocalDateTime start, LocalDateTime end) {
        return exerciseLogRepository.findByUserIdAndLoggedAtBetweenAndIsDeletedFalse(userId, start, end).stream()
                .map(ExerciseLogListItemResponse::from)
                .toList();
    }
}
