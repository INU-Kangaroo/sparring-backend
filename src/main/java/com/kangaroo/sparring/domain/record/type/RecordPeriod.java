package com.kangaroo.sparring.domain.record.type;

import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;

import java.util.Locale;

public enum RecordPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    RANGE;

    public static RecordPeriod from(String value) {
        if (value == null || value.isBlank()) {
            return DAILY;
        }
        try {
            return RecordPeriod.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }
}
