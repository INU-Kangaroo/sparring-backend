package com.kangaroo.sparring.domain.insight.weekly.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangaroo.sparring.domain.insight.weekly.dto.res.ReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
class ReportCacheService {

    private static final String KEY_PREFIX = "weekly-report:";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    Optional<ReportResponse> getCached(Long userId, LocalDate weekStartDate) {
        String key = buildKey(userId, weekStartDate);
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached == null || cached.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(cached, ReportResponse.class));
        } catch (Exception e) {
            log.warn("주간 보고서 캐시 조회 실패: key={}", key, e);
            return Optional.empty();
        }
    }

    void cache(Long userId, LocalDate weekStartDate, ReportResponse response) {
        String key = buildKey(userId, weekStartDate);
        try {
            String value = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, value, DEFAULT_TTL);
        } catch (Exception e) {
            log.warn("주간 보고서 캐시 저장 실패: key={}", key, e);
        }
    }

    private String buildKey(Long userId, LocalDate weekStartDate) {
        return KEY_PREFIX + userId + ":" + weekStartDate;
    }
}
