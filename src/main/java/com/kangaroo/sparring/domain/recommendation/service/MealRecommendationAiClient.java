package com.kangaroo.sparring.domain.recommendation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangaroo.sparring.domain.recommendation.dto.res.MealRecommendationResponse;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MealRecommendationAiClient {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${ml.server.recommend-url}")
    private String recommendUrl;

    @Value("${ml.server.recommend-path:/recommend}")
    private String recommendPath;

    /**
     * AI 서버에 식단 추천 요청
     */
    public AiRecommendResult recommend(Map<String, Object> requestBody) {
        try {
            String responseBody = webClientBuilder.build()
                    .post()
                    .uri(recommendUrl + recommendPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(requestBody))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseResponse(responseBody);

        } catch (Exception e) {
            log.error("AI 서버 식단 추천 호출 실패", e);
            throw new CustomException(ErrorCode.AI_RECOMMENDATION_FAILED);
        }
    }

    private AiRecommendResult parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            // goals
            List<String> goals = new ArrayList<>();
            root.path("goals").forEach(g -> goals.add(g.asText()));

            // meal_recommendations
            String mealTime = root.path("meal_time").asText();
            List<MealRecommendationResponse.RecommendedFoodDto> recommendations = new ArrayList<>();
            JsonNode recsNode = root.path("meal_recommendations");

            recsNode.forEach(item -> {
                List<String> reasons = new ArrayList<>();
                item.path("reasons").forEach(r -> reasons.add(r.asText()));

                recommendations.add(MealRecommendationResponse.RecommendedFoodDto.builder()
                        .foodName(item.path("food_name").asText())
                        .calories(nullableDouble(item, "calories"))
                        .carbs(nullableDouble(item, "carbs"))
                        .protein(nullableDouble(item, "protein"))
                        .fat(nullableDouble(item, "fat"))
                        .fiber(nullableDouble(item, "fiber"))
                        .sodium(nullableDouble(item, "sodium"))
                        .reasons(reasons)
                        .build());
            });

            // food_code 목록 추출 (내부 로그용)
            List<String> foodCodes = new ArrayList<>();
            recsNode.forEach(item -> {
                String fc = item.path("food_code").asText(null);
                if (fc != null && !fc.isBlank()) foodCodes.add(fc);
            });

            // fallback_level
            int fallbackLevel = root.path("fallback_level").asInt(0);

            // applied_constraints, feature_contrib, reason_codes (로그용)
            String appliedConstraints = root.path("applied_constraints").toString();
            String featureContrib = root.path("feature_contrib").toString();
            String reasonCodes = root.path("reason_codes").toString();

            return new AiRecommendResult(
                    mealTime, goals,
                    foodCodes,
                    recommendations,
                    fallbackLevel, appliedConstraints, featureContrib,
                    reasonCodes, objectMapper.writeValueAsString(foodCodes)
            );

        } catch (Exception e) {
            log.error("AI 서버 응답 파싱 실패: {}", responseBody, e);
            throw new CustomException(ErrorCode.AI_RECOMMENDATION_FAILED);
        }
    }

    private Double nullableDouble(JsonNode node, String field) {
        JsonNode val = node.path(field);
        return val.isNull() || val.isMissingNode() ? null : val.asDouble();
    }

    /**
     * AI 서버 응답 파싱 결과
     */
    public record AiRecommendResult(
            String mealTime,
            List<String> goals,
            List<String> foodCodes,
            List<MealRecommendationResponse.RecommendedFoodDto> recommendations,
            int fallbackLevel,
            String appliedConstraints,
            String featureContrib,
            String reasonCodes,
            String foodCodesJson
    ) {}
}
