package com.mopl.mopl.global.auth.extractor;

import com.mopl.mopl.domain.auth.dto.OAuth2UserInfo;
import com.mopl.mopl.domain.user.entity.Social;
import com.mopl.mopl.global.exception.GlobalErrorCode;
import com.mopl.mopl.global.exception.oauth2.OAuth2LoginException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class KakaoOAuth2UserInfoExtractor implements OAuth2UserInfoExtractor {

    private static final String REGISTRATION_ID = "kakao";

    @Override
    public boolean supports(String registrationId) {
        return REGISTRATION_ID.equals(registrationId);
    }

    @Override
    public OAuth2UserInfo extract(Map<String, Object> attributes) {
        Object id = attributes.get("id");

        if (id == null) {
            throw new OAuth2LoginException(GlobalErrorCode.OAUTH2_MISSING_ATTRIBUTE, "id");
        }

        Map<String, Object> kakaoAccount = getRequiredMap(attributes, "kakao_account");

        String email = getRequiredString(kakaoAccount, "email");
        Boolean emailVerified = getRequiredBoolean(kakaoAccount, "is_email_verified");

        if (!Boolean.TRUE.equals(emailVerified)) {
            throw new OAuth2LoginException(GlobalErrorCode.OAUTH2_EMAIL_NOT_VERIFIED);
        }

        String name = extractNicknameOrDefault(kakaoAccount, email);

        return new OAuth2UserInfo(
                Social.KAKAO,
                String.valueOf(id),
                email,
                name
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getRequiredMap(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);

        if (!(value instanceof Map<?, ?>)) {
            throw new OAuth2LoginException(GlobalErrorCode.OAUTH2_MISSING_ATTRIBUTE, key);
        }

        return (Map<String, Object>) value;
    }

    private String getRequiredString(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);

        if (!(value instanceof String stringValue) || stringValue.isBlank()) {
            throw new OAuth2LoginException(GlobalErrorCode.OAUTH2_MISSING_ATTRIBUTE, key);
        }

        return stringValue;
    }

    @SuppressWarnings("unchecked")
    private String extractNicknameOrDefault(Map<String, Object> kakaoAccount, String defaultName) {
        Object profileValue = kakaoAccount.get("profile");

        if (!(profileValue instanceof Map<?, ?>)) {
            return defaultName;
        }

        Map<String, Object> profile = (Map<String, Object>) profileValue;
        Object nickname = profile.get("nickname");

        if (nickname instanceof String nicknameValue && !nicknameValue.isBlank()) {
            return nicknameValue;
        }

        return defaultName;
    }

    private Boolean getRequiredBoolean(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);

        if (!(value instanceof Boolean)) {
            throw new OAuth2LoginException(GlobalErrorCode.OAUTH2_MISSING_ATTRIBUTE, key);
        }

        return (Boolean) value;
    }
}