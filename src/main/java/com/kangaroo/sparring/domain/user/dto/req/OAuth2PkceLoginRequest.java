package com.kangaroo.sparring.domain.user.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OAuth2PkceLoginRequest {

    @NotBlank
    @Schema(description = "OAuth2 authorization code", example = "4/0AfJohXk...")
    private String authorizationCode;

    @NotBlank
    @Schema(description = "OAuth2 redirect URI", example = "https://api.example.com/oauth/callback")
    private String redirectUri;

    @NotBlank
    @Schema(description = "PKCE code_verifier", example = "hJtX...sK9")
    private String codeVerifier;
}
