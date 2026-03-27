package com.kangaroo.sparring.domain.auth.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GoogleSdkLoginRequest {

    @NotBlank
    @Schema(description = "Google SDK server auth code", example = "4/0AfJohXk...")
    private String serverAuthCode;
}
