package com.kangaroo.sparring.domain.user.dto.req;

import com.kangaroo.sparring.domain.user.type.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@Schema(description = "마이페이지 프로필 수정 요청")
public class UpdateUserProfileRequest {

    @Schema(description = "이름(닉네임)", example = "홍길동", nullable = true)
    @Size(min = 1, max = 50, message = "이름은 1자 이상 50자 이하여야 합니다")
    private String username;

    @Schema(description = "생년월일", example = "1995-03-12", nullable = true)
    @Past(message = "생년월일은 과거 날짜여야 합니다")
    private LocalDate birthDate;

    @Schema(description = "성별", example = "FEMALE", nullable = true)
    private Gender gender;

    @Schema(description = "키(cm)", example = "175.5", nullable = true)
    @DecimalMin(value = "100.0", message = "키는 100cm 이상이어야 합니다")
    @DecimalMax(value = "250.0", message = "키는 250cm 이하여야 합니다")
    @Digits(integer = 3, fraction = 2, message = "키는 소수점 둘째 자리까지 입력 가능합니다")
    private BigDecimal height;

    @Schema(description = "몸무게(kg)", example = "70.2", nullable = true)
    @DecimalMin(value = "30.0", message = "몸무게는 30kg 이상이어야 합니다")
    @DecimalMax(value = "300.0", message = "몸무게는 300kg 이하여야 합니다")
    @Digits(integer = 3, fraction = 2, message = "몸무게는 소수점 둘째 자리까지 입력 가능합니다")
    private BigDecimal weight;

    @Schema(description = "프로필 이미지 URL", example = "https://cdn.example.com/profile/1.png", nullable = true)
    @URL(message = "올바른 URL 형식이 아닙니다")
    @Size(max = 500, message = "프로필 이미지 URL은 500자 이하여야 합니다")
    private String profileImageUrl;
}
