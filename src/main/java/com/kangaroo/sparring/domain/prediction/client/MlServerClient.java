package com.kangaroo.sparring.domain.prediction.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kangaroo.sparring.domain.record.common.read.BloodSugarRecord;
import com.kangaroo.sparring.domain.record.common.read.InsulinRecord;
import com.kangaroo.sparring.domain.prediction.dto.res.GlucosePredictionResponse;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MlServerClient {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${ml.server.url}")
    private String mlServerUrl;

    public PredictionResult predictGlucose(
            LocalDateTime timestamp,
            List<BloodSugarRecord> glucoseHistory,
            double carbIntake,
            int mealType,
            int steps,
            double intensity,
            List<InsulinRecord> insulinEvents,
            boolean tempBasalActive,
            double tempBasalValue
    ) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("timestamp", timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            ArrayNode glucoseHistoryNode = requestBody.putArray("glucoseHistory");
            for (BloodSugarRecord record : glucoseHistory) {
                ObjectNode item = glucoseHistoryNode.addObject();
                item.put("glucoseLevel", record.getGlucoseLevel());
                item.put("measuredAt", record.getMeasurementTime().withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                item.put("measurementLabel", record.getMeasurementLabel());
            }

            requestBody.put("carbIntake", carbIntake);
            requestBody.put("mealType", mealType);
            requestBody.put("steps", steps);
            requestBody.put("intensity", intensity);

            ArrayNode insulinEventsNode = requestBody.putArray("insulinEvents");
            for (InsulinRecord record : insulinEvents) {
                ObjectNode item = insulinEventsNode.addObject();
                item.put("eventType", record.getEventType().name().toLowerCase());
                item.put("dose", record.getDose());
                item.put("usedAt", record.getUsedAt().withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                item.put("insulinType", record.getInsulinType());
            }

            requestBody.put("tempBasalActive", tempBasalActive);
            requestBody.put("tempBasalValue", tempBasalValue);
            requestBody.put("debug", false);

            String responseBody = webClientBuilder.build()
                    .post()
                    .uri(mlServerUrl + "/predict-glucose")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode forecastNode = root.get("forecast");
            if (forecastNode == null || !forecastNode.isArray() || forecastNode.isEmpty()) {
                throw new CustomException(ErrorCode.AI_PREDICTION_FAILED, "forecast 응답이 비어 있습니다.");
            }

            List<GlucosePredictionResponse.ForecastPoint> points = new ArrayList<>();
            for (JsonNode point : forecastNode) {
                points.add(GlucosePredictionResponse.ForecastPoint.builder()
                        .time(readOptionalText(point, "time", "time"))
                        .offsetMinutes(readInt(point, "offsetMinutes", "offset_minutes"))
                        .step(readOptionalInt(point, "step", "step"))
                        .predictedGlucose(readDouble(point, "predictedGlucose", "predicted_glucose"))
                        .build());
            }

            if (points.isEmpty()) {
                throw new CustomException(ErrorCode.AI_PREDICTION_FAILED, "forecast 파싱 결과가 비어 있습니다.");
            }

            points.sort(Comparator.comparingInt(GlucosePredictionResponse.ForecastPoint::getOffsetMinutes));

            Map<String, GlucosePredictionResponse.ForecastPoint> milestones = parseMilestones(root.get("milestones"));
            GlucosePredictionResponse.Peak peak = parsePeak(root.get("peak"));

            Double predictedGlucose = readOptionalDouble(root, "predictedGlucose", "predicted_glucose");
            if (predictedGlucose == null) {
                predictedGlucose = points.get(points.size() - 1).getPredictedGlucose();
            }
            Integer predictionOffsetMinutes = readOptionalInt(root, "predictionOffsetMinutes", "prediction_offset_minutes");
            if (predictionOffsetMinutes == null) {
                predictionOffsetMinutes = points.get(points.size() - 1).getOffsetMinutes();
            }
            String predictedTime = readOptionalText(root, "predictedTime", "predicted_time");
            if (predictedTime == null) {
                predictedTime = points.get(points.size() - 1).getTime();
            }

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

            if (milestones.isEmpty()) {
                milestones = buildMilestonesFromForecast(points);
            }

            JsonNode debugNode = root.get("debug");
            Object debug = (debugNode == null || debugNode.isNull()) ? null : objectMapper.treeToValue(debugNode, Object.class);

            return PredictionResult.builder()
                    .predictedGlucose(predictedGlucose)
                    .predictionOffsetMinutes(predictionOffsetMinutes)
                    .predictedTime(predictedTime)
                    .forecast(points)
                    .milestones(milestones)
                    .peak(peak)
                    .debug(debug)
                    .build();

        } catch (Exception e) {
            log.error("ML 서버 혈당 예측 호출 실패", e);
            throw new CustomException(ErrorCode.AI_PREDICTION_FAILED);
        }
    }

    private int readInt(JsonNode node, String camelCaseField, String snakeCaseField) {
        JsonNode target = node.get(camelCaseField);
        if (target == null) {
            target = node.get(snakeCaseField);
        }
        if (target == null || target.isNull()) {
            throw new CustomException(ErrorCode.AI_PREDICTION_FAILED, "예측 응답 필드 누락: " + camelCaseField);
        }
        return target.asInt();
    }

    private double readDouble(JsonNode node, String camelCaseField, String snakeCaseField) {
        JsonNode target = node.get(camelCaseField);
        if (target == null) {
            target = node.get(snakeCaseField);
        }
        if (target == null || target.isNull()) {
            throw new CustomException(ErrorCode.AI_PREDICTION_FAILED, "예측 응답 필드 누락: " + camelCaseField);
        }
        return target.asDouble();
    }

    private Integer readOptionalInt(JsonNode node, String camelCaseField, String snakeCaseField) {
        JsonNode target = node.get(camelCaseField);
        if (target == null) {
            target = node.get(snakeCaseField);
        }
        if (target == null || target.isNull()) {
            return null;
        }
        return target.asInt();
    }

    private Double readOptionalDouble(JsonNode node, String camelCaseField, String snakeCaseField) {
        JsonNode target = node.get(camelCaseField);
        if (target == null) {
            target = node.get(snakeCaseField);
        }
        if (target == null || target.isNull()) {
            return null;
        }
        return target.asDouble();
    }

    private String readOptionalText(JsonNode node, String camelCaseField, String snakeCaseField) {
        JsonNode target = node.get(camelCaseField);
        if (target == null) {
            target = node.get(snakeCaseField);
        }
        if (target == null || target.isNull()) {
            return null;
        }
        return target.asText();
    }

    private Map<String, GlucosePredictionResponse.ForecastPoint> parseMilestones(JsonNode milestonesNode) {
        Map<String, GlucosePredictionResponse.ForecastPoint> milestones = new LinkedHashMap<>();
        if (milestonesNode == null || !milestonesNode.isObject()) {
            return milestones;
        }

        String[] keys = {"10", "30", "60", "90", "120"};
        for (String key : keys) {
            JsonNode point = milestonesNode.get(key);
            if (point == null || point.isNull()) {
                continue;
            }
            milestones.put(key, GlucosePredictionResponse.ForecastPoint.builder()
                    .time(readOptionalText(point, "time", "time"))
                    .offsetMinutes(readInt(point, "offsetMinutes", "offset_minutes"))
                    .step(readOptionalInt(point, "step", "step"))
                    .predictedGlucose(readDouble(point, "predictedGlucose", "predicted_glucose"))
                    .build());
        }
        return milestones;
    }

    private Map<String, GlucosePredictionResponse.ForecastPoint> buildMilestonesFromForecast(
            List<GlucosePredictionResponse.ForecastPoint> forecast
    ) {
        Map<String, GlucosePredictionResponse.ForecastPoint> milestones = new LinkedHashMap<>();
        int[] offsets = {10, 30, 60, 90, 120};
        for (int offset : offsets) {
            forecast.stream()
                    .filter(point -> point.getOffsetMinutes() == offset)
                    .findFirst()
                    .ifPresent(point -> milestones.put(String.valueOf(offset), point));
        }
        return milestones;
    }

    private GlucosePredictionResponse.Peak parsePeak(JsonNode peakNode) {
        if (peakNode == null || peakNode.isNull() || !peakNode.isObject()) {
            return null;
        }
        Double peakGlucose = readOptionalDouble(peakNode, "peakGlucose", "peak_glucose");
        Integer peakOffset = readOptionalInt(peakNode, "peakOffsetMinutes", "peak_offset_minutes");
        if (peakGlucose == null || peakOffset == null) {
            return null;
        }
        return GlucosePredictionResponse.Peak.builder()
                .peakGlucose(peakGlucose)
                .peakTime(readOptionalText(peakNode, "peakTime", "peak_time"))
                .peakOffsetMinutes(peakOffset)
                .build();
    }

    @lombok.Getter
    @lombok.Builder
    public static class PredictionResult {
        private Double predictedGlucose;
        private Integer predictionOffsetMinutes;
        private String predictedTime;
        private List<GlucosePredictionResponse.ForecastPoint> forecast;
        private Map<String, GlucosePredictionResponse.ForecastPoint> milestones;
        private GlucosePredictionResponse.Peak peak;
        private Object debug;
    }
}
