package com.kangaroo.sparring.domain.record.insulin.dto.res;

import com.kangaroo.sparring.domain.record.insulin.entity.InsulinLog;
import com.kangaroo.sparring.domain.record.insulin.type.InsulinEventType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "인슐린 사용 기록 응답")
public class InsulinLogResponse {

    @Schema(description = "기록 ID", example = "1")
    private Long id;

    @Schema(description = "인슐린 이벤트 종류", example = "BOLUS")
    private InsulinEventType eventType;

    @Schema(description = "인슐린 용량", example = "2.0")
    private BigDecimal dose;

    @Schema(description = "인슐린 사용 시각", example = "2026-03-28T12:35:00")
    private LocalDateTime usedAt;

    @Schema(description = "인슐린 종류/제품명", example = "Novolog")
    private String insulinType;

    @Schema(description = "임시 기저 사용 여부", example = "false")
    private boolean tempBasalActive;

    @Schema(description = "임시 기저 인슐린 값", example = "0")
    private BigDecimal tempBasalValue;

    public static InsulinLogResponse from(InsulinLog insulinLog) {
        return InsulinLogResponse.builder()
                .id(insulinLog.getId())
                .eventType(insulinLog.getEventType())
                .dose(insulinLog.getDose())
                .usedAt(insulinLog.getUsedAt())
                .insulinType(insulinLog.getInsulinType())
                .tempBasalActive(insulinLog.isTempBasalActive())
                .tempBasalValue(insulinLog.getTempBasalValue())
                .build();
    }
}
