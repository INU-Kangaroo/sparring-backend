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
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MealRecommendationAiClient {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${ml.recommendation.url}")
    private String serverUrl;

    @Value("${ml.recommendation.path:/api/v1/recommendations}")
    private String recommendPath;

    public AiRecommendResult recommend(Map<String, Object> requestBody) {
        long startedAt = System.currentTimeMillis();
        String endpoint = serverUrl + recommendPath;
        log.info("FastAPI 식단 추천 호출 시작: endpoint={}", endpoint);

        try {
            String responseBody = webClientBuilder.build()
                    .post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(requestBody))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            AiRecommendResult result = parseResponse(responseBody);
            log.info("FastAPI 식단 추천 호출 성공: endpoint={}, elapsedMs={}, cards={}",
                    endpoint, System.currentTimeMillis() - startedAt, result.cards().size());
            return result;
        } catch (WebClientResponseException e) {
            log.error("FastAPI 식단 추천 HTTP 오류: endpoint={}, status={}, elapsedMs={}, body={}",
                    endpoint, e.getStatusCode(), System.currentTimeMillis() - startedAt, e.getResponseBodyAsString(), e);
            throw new CustomException(ErrorCode.AI_RECOMMENDATION_FAILED);
        } catch (Exception e) {
            log.error("FastAPI 식단 추천 호출 실패: endpoint={}, elapsedMs={}",
                    endpoint, System.currentTimeMillis() - startedAt, e);
            throw new CustomException(ErrorCode.AI_RECOMMENDATION_FAILED);
        }
    }

    private AiRecommendResult parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            String mealType = root.path("meal_type").asText();
            double mealTargetKcal = root.path("target").path("meal_target_kcal").asDouble(0);

            List<MealRecommendationResponse.RecommendationCardDto> cards = new ArrayList<>();
            root.path("recommendations").forEach(item -> {
                List<String> reasons = new ArrayList<>();
                item.path("reasons").forEach(r -> reasons.add(r.asText()));

                List<MealRecommendationResponse.MenuItemDto> menus = new ArrayList<>();
                item.path("recipes").forEach(r -> {
                    menus.add(MealRecommendationResponse.MenuItemDto.builder()
                            .id(r.path("recipe_id").asLong())
                            .name(r.path("recipe_name").asText())
                            .kcal(r.path("kcal").asDouble(0))
                            .carbs(r.path("carbs").asDouble(0))
                            .protein(r.path("protein").asDouble(0))
                            .fat(r.path("fat").asDouble(0))
                            .sodium(r.path("sodium").isNull() ? null : r.path("sodium").asDouble())
                            .build());
                });

                cards.add(MealRecommendationResponse.RecommendationCardDto.builder()
                        .rank(item.path("rank").asInt())
                        .title(item.path("set_title").asText())
                        .nutrients(MealRecommendationResponse.NutrientsDto.builder()
                                .kcal(item.path("total_kcal").asDouble(0))
                                .carbs(item.path("total_carbs").asDouble(0))
                                .protein(item.path("total_protein").asDouble(0))
                                .fat(item.path("total_fat").asDouble(0))
                                .sodium(item.path("total_sodium").isNull() ? null : item.path("total_sodium").asDouble())
                                .build())
                        .reasons(reasons)
                        .menus(menus)
                        .build());
            });

            return new AiRecommendResult(mealType, mealTargetKcal, cards);

        } catch (Exception e) {
            log.error("FastAPI 응답 파싱 실패: {}", responseBody, e);
            throw new CustomException(ErrorCode.AI_RECOMMENDATION_FAILED);
        }
    }

    public record AiRecommendResult(
            String mealType,
            double mealTargetKcal,
            List<MealRecommendationResponse.RecommendationCardDto> cards
    ) {}
}
