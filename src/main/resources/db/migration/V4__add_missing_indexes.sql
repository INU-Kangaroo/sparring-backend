-- 자주 사용되는 (user_id + 시간) 조합에 대한 복합 인덱스 추가
-- 기존: FK용 단일 user_id 인덱스만 존재 → 날짜 범위/정렬 쿼리 시 full scan
-- 효과: 주간 리포트, 챗봇 컨텍스트, 측정 이력 조회 모두 커버

-- 혈당 로그: 기간 조회, 최근 N건 조회, 월별 통계 등 모든 쿼리가 user_id + measurement_time 조합
CREATE INDEX idx_blood_sugar_log_user_time
    ON blood_sugar_log (user_id, measurement_time);

-- 혈압 로그: 기간 조회, 최근 N건 조회, 월별 통계 등
CREATE INDEX idx_blood_pressure_log_user_time
    ON blood_pressure_log (user_id, measured_at);

-- 식사 로그: 기간 조회 (오늘/주간/월간)
CREATE INDEX idx_food_log_user_eaten_at
    ON food_log (user_id, eaten_at);

-- 추천 세션: 캐시 조회 (user_id + type + created_at) — 24시간 캐시 히트 체크에 사용
CREATE INDEX idx_recommendation_session_user_type_created
    ON recommendation_session (user_id, type, created_at);
