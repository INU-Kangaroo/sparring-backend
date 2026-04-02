package com.kangaroo.sparring.domain.record.insulin.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kangaroo.sparring.domain.record.insulin.type.InsulinEventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "인슐린 사용 기록 등록 요청 (모든 시간은 KST 기준)")
public class InsulinLogCreateRequest {

    @Schema(description = "인슐린 이벤트 종류", example = "BOLUS", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "인슐린 이벤트 종류는 필수입니다")
    private InsulinEventType eventType;

    @Schema(description = "인슐린 용량", example = "2.0", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "인슐린 용량은 필수입니다")
    @DecimalMin(value = "0.0", inclusive = true, message = "인슐린 용량은 0 이상이어야 합니다")
    private BigDecimal dose;

    @Schema(description = "사용 날짜 (KST)", example = "2026-03-28", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "사용 날짜는 필수입니다")
    private LocalDate usedDate;

    @Schema(description = "사용 시간 (KST, HH:mm)", example = "12:35", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonFormat(pattern = "HH:mm")
    @NotNull(message = "사용 시간은 필수입니다")
    private LocalTime usedTime;

    @Schema(description = "인슐린 종류/제품명", example = "Novolog", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "인슐린 종류는 필수입니다")
    @Size(max = 100, message = "인슐린 종류는 100자 이내로 입력해주세요")
    private String insulinType;

    @Schema(description = "임시 기저 사용 여부", example = "false")
    private Boolean tempBasalActive;

    @Schema(description = "임시 기저 인슐린 값", example = "0")
    @DecimalMin(value = "0.0", inclusive = true, message = "임시 기저 인슐린 값은 0 이상이어야 합니다")
    private BigDecimal tempBasalValue;
}
