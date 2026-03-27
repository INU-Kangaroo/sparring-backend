package com.kangaroo.sparring.domain.record.common.read;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class BloodPressureRecord implements TemporalRecord {
    private final Integer systolic;
    private final Integer diastolic;
    private final LocalDateTime measuredAt;

    public BloodPressureRecord(Integer systolic, Integer diastolic, LocalDateTime measuredAt) {
        this.systolic = systolic;
        this.diastolic = diastolic;
        this.measuredAt = measuredAt;
    }

    @Override
    public LocalDateTime occurredAt() {
        return measuredAt;
    }
}
