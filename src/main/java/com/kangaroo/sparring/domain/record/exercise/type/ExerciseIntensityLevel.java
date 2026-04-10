package com.kangaroo.sparring.domain.record.exercise.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExerciseIntensityLevel {
    LOW("저강도"),
    MODERATE("중강도"),
    HIGH("고강도");

    private final String description;
}
