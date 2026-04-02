package com.kangaroo.sparring.domain.record.api.controller;

import com.kangaroo.sparring.domain.record.exercise.dto.req.ExerciseLogRequest;
import com.kangaroo.sparring.domain.record.exercise.dto.res.ExerciseLogCreateResponse;
import com.kangaroo.sparring.domain.record.exercise.dto.res.ExerciseLogListItemResponse;
import com.kangaroo.sparring.domain.record.food.dto.req.FoodLogCreateRequest;
import com.kangaroo.sparring.domain.record.food.dto.res.FoodLogCreateResponse;
import com.kangaroo.sparring.domain.record.food.dto.res.FoodLogListItemResponse;
import com.kangaroo.sparring.domain.record.blood.dto.req.BloodPressureLogCreateRequest;
import com.kangaroo.sparring.domain.record.blood.dto.req.BloodSugarLogCreateRequest;
import com.kangaroo.sparring.domain.record.blood.dto.res.BloodPressureLogResponse;
import com.kangaroo.sparring.domain.record.blood.dto.res.BloodSugarLogResponse;
import com.kangaroo.sparring.domain.record.api.dto.req.RecordQueryRequest;
import com.kangaroo.sparring.domain.record.insulin.dto.req.InsulinLogCreateRequest;
import com.kangaroo.sparring.domain.record.insulin.dto.res.InsulinLogResponse;
import com.kangaroo.sparring.domain.record.exercise.service.ExerciseLogService;
import com.kangaroo.sparring.domain.record.food.service.FoodLogService;
import com.kangaroo.sparring.domain.record.blood.service.BloodPressureService;
import com.kangaroo.sparring.domain.record.blood.service.BloodSugarService;
import com.kangaroo.sparring.domain.record.insulin.service.InsulinLogService;
import com.kangaroo.sparring.global.security.principal.PrincipalResolver;
import com.kangaroo.sparring.global.security.principal.UserIdPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.util.List;

@Tag(name = "기록", description = "기록 생성/조회 API")
@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class RecordController {

    private final BloodSugarService bloodSugarService;
    private final BloodPressureService bloodPressureService;
    private final InsulinLogService insulinLogService;
    private final FoodLogService foodLogService;
    private final ExerciseLogService exerciseLogService;
    private final Clock kstClock;

    @Operation(
            summary = "혈당 기록 등록",
            description = "신규 경로: /api/records/blood-sugar. 기존 /api/measurements/blood-sugar/logs 경로는 삭제 예정입니다."
    )
    @PostMapping("/blood-sugar")
    public ResponseEntity<BloodSugarLogResponse> createBloodSugarRecord(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody BloodSugarLogCreateRequest request
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(bloodSugarService.createBloodSugarLog(userId, request));
    }

    @Operation(
            summary = "혈당 기록 조회",
            description = "period(daily/weekly/monthly/range) 기준으로 혈당 기록 조회. 기존 /api/measurements/blood-sugar/logs 조회 경로는 삭제 예정입니다."
    )
    @GetMapping("/blood-sugar")
    public ResponseEntity<List<BloodSugarLogResponse>> getBloodSugarRecords(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @ParameterObject RecordQueryRequest query
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        var range = query.toRange(kstClock);
        return ResponseEntity.ok(bloodSugarService.getBloodSugarLogs(userId, range.start(), range.end()));
    }

    @Operation(
            summary = "혈압 기록 등록",
            description = "신규 경로: /api/records/blood-pressure. 기존 /api/measurements/blood-pressure/logs 경로는 삭제 예정입니다."
    )
    @PostMapping("/blood-pressure")
    public ResponseEntity<BloodPressureLogResponse> createBloodPressureRecord(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody BloodPressureLogCreateRequest request
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(bloodPressureService.createBloodPressureLog(userId, request));
    }

    @Operation(
            summary = "혈압 기록 조회",
            description = "period(daily/weekly/monthly/range) 기준으로 혈압 기록 조회. 기존 /api/measurements/blood-pressure/logs 조회 경로는 삭제 예정입니다."
    )
    @GetMapping("/blood-pressure")
    public ResponseEntity<List<BloodPressureLogResponse>> getBloodPressureRecords(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @ParameterObject RecordQueryRequest query
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        var range = query.toRange(kstClock);
        return ResponseEntity.ok(bloodPressureService.getBloodPressureLogs(userId, range.start(), range.end()));
    }

    @Operation(summary = "인슐린 기록 등록")
    @PostMapping("/insulin")
    public ResponseEntity<InsulinLogResponse> createInsulinRecord(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody InsulinLogCreateRequest request
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(insulinLogService.createInsulinLog(userId, request));
    }

    @Operation(summary = "인슐린 기록 조회", description = "period(daily/weekly/monthly/range) 기준으로 인슐린 기록 조회")
    @GetMapping("/insulin")
    public ResponseEntity<List<InsulinLogResponse>> getInsulinRecords(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @ParameterObject RecordQueryRequest query
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        var range = query.toRange(kstClock);
        return ResponseEntity.ok(insulinLogService.getInsulinLogs(userId, range.start(), range.end()));
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
            @ParameterObject RecordQueryRequest query
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        var range = query.toRange(kstClock);
        return ResponseEntity.ok(foodLogService.getFoodLogs(userId, range.start(), range.end()));
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
            @ParameterObject RecordQueryRequest query
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        var range = query.toRange(kstClock);
        return ResponseEntity.ok(exerciseLogService.getExerciseLogs(userId, range.start(), range.end()));
    }
}
