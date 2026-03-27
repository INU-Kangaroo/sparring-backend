package com.kangaroo.sparring.domain.insight.weekly.service;

import com.kangaroo.sparring.domain.record.common.read.ExerciseRecord;
import com.kangaroo.sparring.domain.record.common.read.FoodRecord;
import com.kangaroo.sparring.domain.record.common.read.BloodPressureRecord;
import com.kangaroo.sparring.domain.record.common.read.BloodSugarRecord;
import com.kangaroo.sparring.domain.record.common.read.TemporalRecord;
import com.kangaroo.sparring.domain.insight.weekly.dto.internal.CommentEvidence;
import com.kangaroo.sparring.domain.insight.weekly.dto.internal.DailyConditionEvidence;
import com.kangaroo.sparring.domain.insight.weekly.dto.internal.HighlightEvidence;
import com.kangaroo.sparring.domain.insight.weekly.dto.internal.ImprovementEvidence;
import com.kangaroo.sparring.domain.insight.weekly.dto.internal.ReportEvidence;
import com.kangaroo.sparring.domain.insight.weekly.dto.internal.ScoreEvidence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ReportRuleEngine {

    private static final int WEEK_DAYS = 7;

    private final ReportRuleSupport reportRuleSupport;

    public ReportEvidence evaluate(
            LocalDate monday,
            List<BloodSugarRecord> bsLogs,
            List<BloodPressureRecord> bpLogs,
            List<FoodRecord> foodLogs,
            List<ExerciseRecord> exerciseLogs
    ) {
        ReportRuleSupport.BloodSugarStats bs = reportRuleSupport.calcBloodSugarStats(bsLogs);
        ReportRuleSupport.BloodPressureStats bp = reportRuleSupport.calcBloodPressureStats(bpLogs);
        ReportRuleSupport.MealStats meal = reportRuleSupport.calcMealStats(foodLogs);
        ReportRuleSupport.ExerciseStats exercise = reportRuleSupport.calcExerciseStats(exerciseLogs);

        int healthManagement = reportRuleSupport.calcHealthManagementScore(bs, bp);
        int bsRecordDays = calcMeasurementRecordDays(bsLogs, monday);
        int bpRecordDays = calcMeasurementRecordDays(bpLogs, monday);
        int measurementConsistency = reportRuleSupport.calcMeasurementConsistencyScore(bsRecordDays, bpRecordDays);
        int lifestyle = reportRuleSupport.calcLifestyleScore(meal, exercise);
        int stability = reportRuleSupport.calcStabilityScore(bsLogs, bpLogs);
        int trend = reportRuleSupport.calcTrendScore(monday, bsLogs, bpLogs);
        int overall = reportRuleSupport.calcOverallScore(
                healthManagement,
                measurementConsistency,
                lifestyle,
                stability,
                trend,
                bsLogs.isEmpty() && bpLogs.isEmpty(),
                foodLogs.isEmpty() && exerciseLogs.isEmpty()
        );

        List<DailyConditionEvidence> dailyConditions = reportRuleSupport.buildDailyConditions(
                monday, bsLogs, bpLogs, foodLogs, exerciseLogs
        );
        List<HighlightEvidence> highlights = reportRuleSupport.buildHighlights(
                bsLogs, bpLogs, foodLogs, exerciseLogs, monday
        );
        ImprovementEvidence improvement = reportRuleSupport.selectImprovementArea(bs, bp, meal, exercise);

        CommentEvidence comment = new CommentEvidence(
                bs.totalCount > 0 ? bs.overallAvg : null,
                bp.totalCount > 0 ? bp.systolicAvg : null,
                meal.totalCount > 0 ? meal.avgCaloriesPerDay : null,
                exercise.totalCount > 0 ? exercise.totalCount : null,
                calcRecordDays(bsLogs, bpLogs, foodLogs, exerciseLogs, monday)
        );

        return new ReportEvidence(
                comment.recordDays(),
                calcMeasurementRecordDays(bsLogs, monday),
                calcMeasurementRecordDays(bpLogs, monday),
                new ScoreEvidence(healthManagement, measurementConsistency, lifestyle, overall),
                dailyConditions,
                highlights,
                improvement,
                comment
        );
    }

    private int calcRecordDays(
            List<BloodSugarRecord> bs,
            List<BloodPressureRecord> bp,
            List<FoodRecord> meal,
            List<ExerciseRecord> exercise,
            LocalDate monday
    ) {
        Set<LocalDate> recordedDates = new HashSet<>();
        bs.forEach(l -> recordedDates.add(l.occurredAt().toLocalDate()));
        bp.forEach(l -> recordedDates.add(l.occurredAt().toLocalDate()));
        meal.forEach(l -> recordedDates.add(l.occurredAt().toLocalDate()));
        exercise.forEach(l -> recordedDates.add(l.occurredAt().toLocalDate()));
        return (int) recordedDates.stream()
                .filter(d -> !d.isBefore(monday) && !d.isAfter(monday.plusDays(WEEK_DAYS - 1L)))
                .count();
    }

    private int calcMeasurementRecordDays(List<? extends TemporalRecord> logs, LocalDate monday) {
        LocalDate sunday = monday.plusDays(WEEK_DAYS - 1L);
        return (int) logs.stream()
                .map(log -> log.occurredAt().toLocalDate())
                .filter(date -> !date.isBefore(monday) && !date.isAfter(sunday))
                .distinct()
                .count();
    }
}
