package com.kangaroo.sparring.domain.record.controller;

import com.kangaroo.sparring.domain.exercise.log.dto.req.ExerciseLogRequest;
import com.kangaroo.sparring.domain.exercise.log.dto.res.ExerciseLogCreateResponse;
import com.kangaroo.sparring.domain.exercise.log.dto.res.ExerciseLogListItemResponse;
import com.kangaroo.sparring.domain.exercise.log.service.ExerciseLogService;
import com.kangaroo.sparring.domain.food.log.dto.req.FoodLogCreateRequest;
import com.kangaroo.sparring.domain.food.log.dto.res.FoodLogCreateResponse;
import com.kangaroo.sparring.domain.food.log.dto.res.FoodLogListItemResponse;
import com.kangaroo.sparring.domain.food.log.service.FoodLogService;
import com.kangaroo.sparring.domain.measurement.dto.req.BloodPressureLogCreateRequest;
import com.kangaroo.sparring.domain.measurement.dto.req.BloodSugarLogCreateRequest;
import com.kangaroo.sparring.domain.measurement.dto.res.BloodPressureLogResponse;
import com.kangaroo.sparring.domain.measurement.dto.res.BloodSugarLogResponse;
import com.kangaroo.sparring.domain.measurement.service.BloodPressureService;
import com.kangaroo.sparring.domain.measurement.service.BloodSugarService;
import com.kangaroo.sparring.domain.record.service.RecordQueryService;
import com.kangaroo.sparring.domain.record.support.RecordPeriodSupport;
import com.kangaroo.sparring.domain.record.type.RecordPeriod;
import com.kangaroo.sparring.global.security.principal.PrincipalResolver;
import com.kangaroo.sparring.global.security.principal.UserIdPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "기록 조회", description = "기간별 기록 조회 API")
@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class RecordController {

    private final BloodSugarService bloodSugarService;
    private final BloodPressureService bloodPressureService;
    private final FoodLogService foodLogService;
    private final ExerciseLogService exerciseLogService;
    private final RecordQueryService recordQueryService;
    private final Clock kstClock;

    @Operation(summary = "혈당 기록 등록")
    @PostMapping("/blood-sugar")
    public ResponseEntity<BloodSugarLogResponse> createBloodSugarRecord(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody BloodSugarLogCreateRequest request
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(bloodSugarService.createBloodSugarLog(userId, request));
    }

    @Operation(summary = "혈당 기록 조회", description = "period(daily/weekly/monthly/range) 기준으로 혈당 기록 조회")
    @GetMapping("/blood-sugar")
    public ResponseEntity<List<BloodSugarLogResponse>> getBloodSugarRecords(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @RequestParam(defaultValue = "daily") String period,
            @Parameter(description = "기준 날짜 (daily/weekly/monthly에서 사용)", example = "2026-03-26")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        RecordPeriod resolvedPeriod = RecordPeriod.from(period);
        RecordPeriodSupport.DateTimeRange range = RecordPeriodSupport.resolve(
                resolvedPeriod, date, year, month, startDate, endDate, kstClock
        );
        return ResponseEntity.ok(recordQueryService.getBloodSugarRecords(userId, range.start(), range.end()));
    }

    @Operation(summary = "혈압 기록 등록")
    @PostMapping("/blood-pressure")
    public ResponseEntity<BloodPressureLogResponse> createBloodPressureRecord(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody BloodPressureLogCreateRequest request
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(bloodPressureService.createBloodPressureLog(userId, request));
    }

    @Operation(summary = "혈압 기록 조회", description = "period(daily/weekly/monthly/range) 기준으로 혈압 기록 조회")
    @GetMapping("/blood-pressure")
    public ResponseEntity<List<BloodPressureLogResponse>> getBloodPressureRecords(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        RecordPeriod resolvedPeriod = RecordPeriod.from(period);
        RecordPeriodSupport.DateTimeRange range = RecordPeriodSupport.resolve(
                resolvedPeriod, date, year, month, startDate, endDate, kstClock
        );
        return ResponseEntity.ok(recordQueryService.getBloodPressureRecords(userId, range.start(), range.end()));
    }

    @Operation(summary = "식사 기록 등록")
    @PostMapping("/food")
    public ResponseEntity<FoodLogCreateResponse> createFoodRecord(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody FoodLogCreateRequest request
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(foodLogService.createFoodLog(userId, request));
    }

    @Operation(summary = "식사 기록 조회", description = "period(daily/weekly/monthly/range) 기준으로 식사 기록 조회")
    @GetMapping("/food")
    public ResponseEntity<List<FoodLogListItemResponse>> getFoodRecords(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        RecordPeriod resolvedPeriod = RecordPeriod.from(period);
        RecordPeriodSupport.DateTimeRange range = RecordPeriodSupport.resolve(
                resolvedPeriod, date, year, month, startDate, endDate, kstClock
        );
        return ResponseEntity.ok(recordQueryService.getFoodRecords(userId, range.start(), range.end()));
    }

    @Operation(summary = "식사 기록 삭제")
    @DeleteMapping("/food/{foodLogId}")
    public ResponseEntity<Void> deleteFoodRecord(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @PathVariable Long foodLogId
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        foodLogService.deleteFoodLog(userId, foodLogId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "운동 기록 등록")
    @PostMapping("/exercise")
    public ResponseEntity<ExerciseLogCreateResponse> createExerciseRecord(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody ExerciseLogRequest request
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(exerciseLogService.createExerciseLog(userId, request));
    }

    @Operation(summary = "운동 기록 조회", description = "period(daily/weekly/monthly/range) 기준으로 운동 기록 조회")
    @GetMapping("/exercise")
    public ResponseEntity<List<ExerciseLogListItemResponse>> getExerciseRecords(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        RecordPeriod resolvedPeriod = RecordPeriod.from(period);
        RecordPeriodSupport.DateTimeRange range = RecordPeriodSupport.resolve(
                resolvedPeriod, date, year, month, startDate, endDate, kstClock
        );
        return ResponseEntity.ok(recordQueryService.getExerciseRecords(userId, range.start(), range.end()));
    }
}
