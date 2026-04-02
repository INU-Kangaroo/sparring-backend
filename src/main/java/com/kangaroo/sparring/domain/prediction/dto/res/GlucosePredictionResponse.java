package com.kangaroo.sparring.domain.prediction.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@Schema(description = "혈당 예측 응답")
public class GlucosePredictionResponse {

    @Schema(description = "음식명", example = "바질 페스토 샐러드")
    private String foodName;

    @Schema(description = "최종 예측 혈당값", example = "129")
    private Double predictedGlucose;

    @Schema(description = "최종 예측 시점까지의 분 거리", example = "120")
    private Integer predictionOffsetMinutes;

    @Schema(description = "최종 예측 시각", example = "2026-03-28T15:20:00Z")
    private String predictedTime;

    @Schema(description = "시간대별 혈당 예측 (ML forecast 원본 기준)")
    private List<ForecastPoint> forecast;

    @Schema(description = "주요 시점(10,30,60,90,120분) 예측값")
    private Map<String, ForecastPoint> milestones;

    @Schema(description = "예측 구간 내 최고 혈당 정보")
    private Peak peak;

    @Schema(description = "ML 디버그 정보", nullable = true)
    private Object debug;

    @Schema(description = "예측 구간 내 최고 혈당 (mg/dL)", example = "148")
    private Double peakGlucose;

    @Schema(description = "최고 혈당 도달 시점 (분)", example = "30")
    private Integer peakOffsetMinutes;

    @Getter
    @Builder
    @Schema(description = "시간대별 혈당 예측 포인트")
    public static class ForecastPoint {
        @Schema(description = "해당 예측 시각", example = "2026-03-28T13:30:00Z")
        private String time;

        @Schema(description = "기준 시각으로부터 경과 분", example = "30")
        private Integer offsetMinutes;

        @Schema(description = "예측 순번", example = "1")
        private Integer step;

        @Schema(description = "예측 혈당 (mg/dL)", example = "148.0")
        private Double predictedGlucose;
    }

    @Getter
    @Builder
    @Schema(description = "예측 구간 최고 혈당 정보")
    public static class Peak {
        @Schema(description = "예측 구간 최고 혈당값", example = "133")
        private Double peakGlucose;

        @Schema(description = "최고 혈당 시각", example = "2026-03-28T14:00:00Z")
        private String peakTime;

        @Schema(description = "최고 혈당 도달 offset 분", example = "40")
        private Integer peakOffsetMinutes;
    }

    public static GlucosePredictionResponse of(
            String foodName,
            Double predictedGlucose,
            Integer predictionOffsetMinutes,
            String predictedTime,
            List<ForecastPoint> forecast,
            Map<String, ForecastPoint> milestones,
            Peak peak,
            Object debug,
            Double peakGlucose,
            Integer peakOffsetMinutes
    ) {
        return GlucosePredictionResponse.builder()
                .foodName(foodName)
                .predictedGlucose(predictedGlucose)
                .predictionOffsetMinutes(predictionOffsetMinutes)
                .predictedTime(predictedTime)
                .forecast(forecast)
                .milestones(milestones)
                .peak(peak)
                .debug(debug)
                .peakGlucose(peakGlucose)
                .peakOffsetMinutes(peakOffsetMinutes)
                .build();
    }
}
