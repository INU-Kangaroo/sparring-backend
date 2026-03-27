package com.kangaroo.sparring.domain.record.common.read;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class FoodRecord implements TemporalRecord {
    private final LocalDateTime eatenAt;
    private final Double calories;

    public FoodRecord(LocalDateTime eatenAt, Double calories) {
        this.eatenAt = eatenAt;
        this.calories = calories;
    }

    @Override
    public LocalDateTime occurredAt() {
        return eatenAt;
    }
}
