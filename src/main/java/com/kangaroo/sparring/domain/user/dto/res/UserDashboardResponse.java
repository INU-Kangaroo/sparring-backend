package com.kangaroo.sparring.domain.user.dto.res;

import com.kangaroo.sparring.domain.user.type.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
@Builder
@Schema(description = "마이페이지 대시보드 응답")
public class UserDashboardResponse {

    @Schema(description = "상단 프로필")
    private Profile profile;

    @Schema(description = "측정 요약")
    private Record record;

    @Schema(description = "기본 정보")
    private BasicInfo basicInfo;

    @Getter
    @AllArgsConstructor
    @Builder
    @Schema(description = "상단 프로필")
    public static class Profile {
        @Schema(description = "이름(닉네임)", example = "홍길동")
        private String username;

        @Schema(description = "프로필 이미지 URL", example = "https://cdn.example.com/profile/1.png", nullable = true)
        private String profileImageUrl;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    @Schema(description = "측정 요약")
    public static class Record {
        @Schema(description = "혈당 총 측정 횟수", example = "187")
        private Long totalMeasurementCount;

        @Schema(description = "연속 측정 일수(오늘 기준)", example = "9")
        private Integer consecutiveMeasurementDays;

        @Schema(description = "평균 혈당 (mg/dL)", example = "118.4", nullable = true)
        private BigDecimal averageBloodSugarMgDl;

        @Schema(description = "최근 7일 평균 혈당 (mg/dL)", example = "112.7", nullable = true)
        private BigDecimal last7DaysAverageBloodSugarMgDl;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    @Schema(description = "기본 정보")
    public static class BasicInfo {
        @Schema(description = "이름", example = "홍길동")
        private String name;

        @Schema(description = "생년월일", example = "1998-11-03", nullable = true)
        private LocalDate birthDate;

        @Schema(description = "성별", example = "FEMALE", nullable = true)
        private Gender gender;

        @Schema(description = "키(cm)", example = "172.3", nullable = true)
        private BigDecimal heightCm;

        @Schema(description = "몸무게(kg)", example = "68.1", nullable = true)
        private BigDecimal weightKg;
    }
}
