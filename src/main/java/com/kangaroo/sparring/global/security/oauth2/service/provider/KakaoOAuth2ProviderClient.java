package com.kangaroo.sparring.global.security.oauth2.service.provider;

import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoOAuth2ProviderClient {
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestTemplate restTemplate;

    @Value("${spring.oauth2.kakao.client-id}")
    private String kakaoClientId;

    @Value("${spring.oauth2.kakao.client-secret:}")
    private String kakaoClientSecret;

    @Value("${spring.oauth2.kakao.token-uri}")
    private String kakaoTokenUri;

    @Value("${spring.oauth2.kakao.user-info-uri}")
    private String kakaoUserInfoUri;

    @Value("${spring.oauth2.kakao.access-token-info-uri}")
    private String kakaoAccessTokenInfoUri;

    @Value("${spring.oauth2.kakao.app-id}")
    private String kakaoAppId;

    public String exchangeAuthorizationCode(String code, String redirectUri, String codeVerifier) {
        if (code == null || code.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "code는 필수입니다.");
        }
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new CustomException(ErrorCode.OAUTH2_MISSING_REDIRECT_URI);
        }

        Map<String, Object> tokenResponse = requestAuthorizationCodeTokens(code, redirectUri, codeVerifier);
        String accessToken = tokenResponse != null ? (String) tokenResponse.get("access_token") : null;
        if (accessToken == null || accessToken.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        validateKakaoAccessToken(accessToken);
        return accessToken;
    }

    public String validateSdkAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "accessToken은 필수입니다.");
        }
        log.info("Using Kakao access token from client.");
        validateKakaoAccessToken(accessToken);
        return accessToken;
    }

    public Map<String, Object> fetchUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    kakaoUserInfoUri,
                    HttpMethod.POST,
                    entity,
                    MAP_RESPONSE_TYPE
            );
            return response.getBody();
        } catch (HttpClientErrorException ex) {
            log.warn("Kakao userinfo request failed: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    private Map<String, Object> requestAuthorizationCodeTokens(String code, String redirectUri, String codeVerifier) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("code", code);
        params.add("client_id", kakaoClientId);
        params.add("redirect_uri", redirectUri);

        if (codeVerifier != null && !codeVerifier.isBlank()) {
            params.add("code_verifier", codeVerifier);
        }
        if (kakaoClientSecret != null && !kakaoClientSecret.isBlank()) {
            params.add("client_secret", kakaoClientSecret);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    kakaoTokenUri,
                    HttpMethod.POST,
                    entity,
                    MAP_RESPONSE_TYPE
            );
            return response.getBody();
        } catch (HttpClientErrorException ex) {
            log.warn("Kakao token exchange failed: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    private void validateKakaoAccessToken(String accessToken) {
        if (kakaoAccessTokenInfoUri == null || kakaoAccessTokenInfoUri.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (kakaoAppId == null || kakaoAppId.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    kakaoAccessTokenInfoUri,
                    HttpMethod.GET,
                    entity,
                    MAP_RESPONSE_TYPE
            );
            Map<String, Object> body = response.getBody();
            if (body == null || !kakaoAppId.equals(String.valueOf(body.get("app_id")))) {
                throw new CustomException(ErrorCode.INVALID_TOKEN);
            }
        } catch (HttpClientErrorException ex) {
            log.warn("Kakao access token validation failed: status={}, body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

}
