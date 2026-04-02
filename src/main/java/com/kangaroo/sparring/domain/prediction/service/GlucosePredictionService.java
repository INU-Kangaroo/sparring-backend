package com.kangaroo.sparring.domain.prediction.service;

import com.kangaroo.sparring.domain.record.common.read.ExerciseRecord;
import com.kangaroo.sparring.domain.record.common.read.InsulinRecord;
import com.kangaroo.sparring.domain.catalog.entity.Food;
import com.kangaroo.sparring.domain.catalog.repository.FoodRepository;
import com.kangaroo.sparring.domain.record.common.read.BloodSugarRecord;
import com.kangaroo.sparring.domain.record.common.read.RecordReadService;
import com.kangaroo.sparring.domain.prediction.client.MlServerClient;
import com.kangaroo.sparring.domain.prediction.dto.res.GlucosePredictionResponse;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GlucosePredictionService {

    private static final int GLUCOSE_HISTORY_COUNT = 3;
    private static final int INSULIN_HISTORY_COUNT = 10;

    private final RecordReadService recordReadService;
    private final FoodRepository foodRepository;
    private final MlServerClient mlServerClient;
    private final Clock kstClock;

    public GlucosePredictionResponse predictGlucose(User user, Long foodId) {
        // 음식 + 영양정보 조회
        Food food = foodRepository.findByIdWithNutrition(foodId)
                .orElseThrow(() -> new CustomException(ErrorCode.FOOD_NOT_FOUND));

        double carbIntake = food.getCarbs() != null ? food.getCarbs() : 0.0;

        // 최근 혈당 기록 3개 조회
        List<BloodSugarRecord> recentLogs = recordReadService
                .getRecentBloodSugarRecords(user.getId(), GLUCOSE_HISTORY_COUNT);

        if (recentLogs.isEmpty()) {
            throw new CustomException(ErrorCode.INSUFFICIENT_DATA_FOR_PREDICTION);
        }

        LocalDateTime timestamp = LocalDateTime.now(kstClock).withNano(0);
        List<BloodSugarRecord> glucoseHistory = recentLogs.stream()
                .sorted(Comparator.comparing(BloodSugarRecord::getMeasurementTime))
                .toList();
        int mealType = resolveMealType(LocalTime.now(kstClock));
        int steps = 0;
        double intensity = resolveTodayIntensity(user.getId());
        List<InsulinRecord> insulinRecords = recordReadService
                .getRecentInsulinRecords(user.getId(), INSULIN_HISTORY_COUNT, timestamp)
                .stream()
                .sorted(Comparator.comparing(InsulinRecord::getUsedAt))
                .toList();

        boolean tempBasalActive = insulinRecords.stream().anyMatch(InsulinRecord::isTempBasalActive);
        double tempBasalValue = insulinRecords.stream()
                .filter(InsulinRecord::isTempBasalActive)
                .max(Comparator.comparing(InsulinRecord::getUsedAt))
                .map(record -> record.getTempBasalValue().doubleValue())
                .orElse(0.0);

        // ML 서버 호출
        MlServerClient.PredictionResult mlResult = mlServerClient.predictGlucose(
                timestamp,
                glucoseHistory,
                carbIntake,
                mealType,
                steps,
                intensity,
                insulinRecords,
                tempBasalActive,
                tempBasalValue
        );
        List<GlucosePredictionResponse.ForecastPoint> points = mlResult.getForecast();
        if (points.isEmpty()) {
            throw new CustomException(ErrorCode.AI_PREDICTION_FAILED, "예측 포인트가 없습니다.");
        }

        GlucosePredictionResponse.Peak peak = mlResult.getPeak();
        if (peak == null) {
            GlucosePredictionResponse.ForecastPoint peakPoint = points.stream()
                    .max(Comparator.comparingDouble(GlucosePredictionResponse.ForecastPoint::getPredictedGlucose))
                    .orElse(points.get(0));
            peak = GlucosePredictionResponse.Peak.builder()
                    .peakGlucose(peakPoint.getPredictedGlucose())
                    .peakTime(peakPoint.getTime())
                    .peakOffsetMinutes(peakPoint.getOffsetMinutes())
                    .build();
        }

        return GlucosePredictionResponse.of(
                food.getName(),
                mlResult.getPredictedGlucose(),
                mlResult.getPredictionOffsetMinutes(),
                mlResult.getPredictedTime(),
                points,
                mlResult.getMilestones(),
                peak,
                mlResult.getDebug(),
                peak.getPeakGlucose(),
                peak.getPeakOffsetMinutes()
        );
    }

    // 현재 시각 기준 식사 타입 (1:아침, 2:점심, 3:저녁, 4:간식)
    private int resolveMealType(LocalTime time) {
        int hour = time.getHour();
        if (hour >= 7 && hour < 12) return 1;
        if (hour >= 12 && hour < 18) return 2;
        if (hour >= 18 && hour < 24) return 3;
        return 4; // 0~6시 간식
    }

    // 오늘 운동 기록에서 intensity 계산 (metValue 기준, 없으면 0)
    private double resolveTodayIntensity(Long userId) {
        List<ExerciseRecord> todayLogs = recordReadService.getTodayExerciseRecords(userId, LocalDate.now(kstClock));

        return todayLogs.stream()
                .mapToDouble(ExerciseRecord::getMetValue)
                .map(met -> Math.min(met / 12.0, 1.0))
                .max()
                .orElse(0.0);
    }
}
