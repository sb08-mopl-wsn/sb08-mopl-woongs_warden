package com.mopl.mopl.global.auth.extractor;

import com.mopl.mopl.domain.auth.dto.OAuth2UserInfo;
import com.mopl.mopl.domain.user.entity.Social;
import com.mopl.mopl.global.exception.GlobalErrorCode;
import com.mopl.mopl.global.exception.oauth2.OAuth2LoginException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GoogleOAuth2UserInfoExtractor implements OAuth2UserInfoExtractor {

    private static final String REGISTRATION_ID = "google";

    @Override
    public boolean supports(String registrationId) {
        return REGISTRATION_ID.equals(registrationId);
    }

    @Override
    public OAuth2UserInfo extract(Map<String, Object> attributes) {
        String socialId = getRequiredString(attributes, "sub");
        String email = getRequiredString(attributes, "email");
        String name = getStringOrDefault(attributes, "name", email);
        Boolean emailVerified = (Boolean) attributes.get("email_verified");

        if (!Boolean.TRUE.equals(emailVerified)) {
            throw new OAuth2LoginException(GlobalErrorCode.OAUTH2_EMAIL_NOT_VERIFIED);
        }

        return new OAuth2UserInfo(
                Social.GOOGLE,
                socialId,
                email,
                name
        );
    }

    private String getRequiredString(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);

        if (!(value instanceof String stringValue) || stringValue.isBlank()) {
            throw new OAuth2LoginException(GlobalErrorCode.OAUTH2_MISSING_ATTRIBUTE, key);
        }

        return stringValue;
    }

    private String getStringOrDefault(Map<String, Object> attributes, String key, String defaultValue) {
        Object value = attributes.get(key);

        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue;
        }

        return defaultValue;
    }
}