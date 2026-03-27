package com.kangaroo.sparring.global.security.oauth2.service;

import com.kangaroo.sparring.domain.auth.dto.req.GoogleSdkLoginRequest;
import com.kangaroo.sparring.domain.auth.dto.req.KakaoSdkLoginRequest;
import com.kangaroo.sparring.domain.auth.dto.req.OAuth2PkceLoginRequest;
import com.kangaroo.sparring.domain.auth.dto.res.AuthResponse;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.auth.service.OAuth2UserService;
import com.kangaroo.sparring.global.security.jwt.JwtUtil;
import com.kangaroo.sparring.global.security.oauth2.user.OAuth2UserInfo;
import com.kangaroo.sparring.global.security.oauth2.user.OAuth2UserInfoFactory;
import com.kangaroo.sparring.global.security.oauth2.service.provider.GoogleOAuth2ProviderClient;
import com.kangaroo.sparring.global.security.oauth2.service.provider.KakaoOAuth2ProviderClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2CodeAuthService {

    private final OAuth2UserService oauth2UserService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final GoogleOAuth2ProviderClient googleOAuth2ProviderClient;
    private final KakaoOAuth2ProviderClient kakaoOAuth2ProviderClient;

    @Transactional
    public AuthResponse loginWithGooglePkce(OAuth2PkceLoginRequest request) {
        String accessToken = googleOAuth2ProviderClient.exchangePkceCode(
                request.getAuthorizationCode(),
                request.getRedirectUri(),
                request.getCodeVerifier()
        );
        return loginWithAccessToken("google", accessToken);
    }

    @Transactional
    public AuthResponse loginWithGoogleSdkCode(GoogleSdkLoginRequest request) {
        String accessToken = googleOAuth2ProviderClient.exchangeSdkAuthCode(request.getServerAuthCode());
        return loginWithAccessToken("google", accessToken);
    }

    @Transactional
    public AuthResponse loginWithKakaoPkce(OAuth2PkceLoginRequest request) {
        String accessToken = kakaoOAuth2ProviderClient.exchangeAuthorizationCode(
                request.getAuthorizationCode(),
                request.getRedirectUri(),
                request.getCodeVerifier()
        );
        return loginWithAccessToken("kakao", accessToken);
    }

    @Transactional
    public AuthResponse loginWithKakaoSdkAccessToken(KakaoSdkLoginRequest request) {
        String accessToken = kakaoOAuth2ProviderClient.validateSdkAccessToken(request.getAccessToken());
        return loginWithAccessToken("kakao", accessToken);
    }

    private AuthResponse loginWithAccessToken(String provider, String accessToken) {
        Map<String, Object> userAttributes = fetchUserInfo(provider, accessToken);
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.create(provider, userAttributes);
        User user = oauth2UserService.processOAuth2User(userInfo);
        user.updateLastLogin();
        return issueTokens(user);
    }

    private Map<String, Object> fetchUserInfo(String provider, String accessToken) {
        return switch (provider) {
            case "google" -> googleOAuth2ProviderClient.fetchUserInfo(accessToken);
            case "kakao" -> kakaoOAuth2ProviderClient.fetchUserInfo(accessToken);
            default -> throw new IllegalArgumentException("지원하지 않는 provider: " + provider);
        };
    }

    private AuthResponse issueTokens(User user) {
        String jwtAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
        String jwtRefreshToken = jwtUtil.generateRefreshToken(user.getId());
        refreshTokenService.saveRefreshToken(user.getId(), jwtRefreshToken);

        return AuthResponse.of(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                jwtAccessToken,
                jwtRefreshToken
        );
    }
}
