package com.kangaroo.sparring.domain.user.dto.req;

import com.kangaroo.sparring.domain.user.type.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "회원가입 요청")
public class SignupRequest {

    @Schema(description = "이메일", example = "test@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @Schema(description = "비밀번호 (최소 8자)", example = "password123!", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
    private String password;

    @Schema(description = "닉네임", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "닉네임은 필수입니다")
    private String username;

    @Schema(description = "생년월일 (YYYY-MM-DD)", example = "1990-01-01", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "생년월일은 필수입니다")
    @Past(message = "생년월일은 과거 날짜여야 합니다")
    private LocalDate birthDate;

    @Schema(description = "성별", example = "MALE", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "성별은 필수입니다")
    private Gender gender;
}
