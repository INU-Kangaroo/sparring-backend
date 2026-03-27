package com.kangaroo.sparring.domain.auth.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoSdkLoginRequest {

    @NotBlank
    @Schema(description = "Kakao SDK access token", example = "eyJhbGciOi...")
    private String accessToken;
}
