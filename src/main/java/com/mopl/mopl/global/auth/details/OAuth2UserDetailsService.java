package com.mopl.mopl.global.auth.details;

import com.mopl.mopl.domain.auth.dto.OAuth2UserInfo;
import com.mopl.mopl.domain.auth.exception.AuthAuthenticationFailedException;
import com.mopl.mopl.domain.user.entity.Social;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.mapper.UserMapper;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.exception.GlobalErrorCode;
import com.mopl.mopl.global.exception.oauth2.OAuth2LoginException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OAuth2UserDetailsService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oauth2User.getAttributes();

        OAuth2UserInfo userInfo = extractUserInfo(registrationId, attributes);

        User user = userRepository.findBySocialTypeAndSocialId(
                        userInfo.socialType(),
                        userInfo.socialId()
                )
                .orElseGet(() -> findOrCreateSocialUser(userInfo));

        validateNotLocked(user);

        return new OAuth2UserDetails(userMapper.toDto(user), attributes);
    }

    private OAuth2UserInfo extractUserInfo(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId) {
            case "google" -> extractGoogleUserInfo(attributes);
            case "kakao" -> extractKakaoUserInfo(attributes);
            default -> throw new OAuth2LoginException(GlobalErrorCode.OAUTH2_UNSUPPORTED_PROVIDER);
        };
    }

    private OAuth2UserInfo extractGoogleUserInfo(Map<String, Object> attributes) {
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

    private OAuth2UserInfo extractKakaoUserInfo(Map<String, Object> attributes) {
        Object id = attributes.get("id");

        if (id == null) {
            throw new OAuth2LoginException(GlobalErrorCode.OAUTH2_MISSING_ATTRIBUTE, "id");
        }

        Map<String, Object> kakaoAccount = getRequiredMap(attributes, "kakao_account");

        String email = getRequiredString(kakaoAccount, "email");

        Boolean emailVerified = (Boolean) kakaoAccount.get("is_email_verified");
        if (!Boolean.TRUE.equals(emailVerified)) {
            throw new OAuth2LoginException(GlobalErrorCode.OAUTH2_EMAIL_NOT_VERIFIED);
        }

        String name = email;

        Object profileValue = kakaoAccount.get("profile");
        if (profileValue instanceof Map<?, ?> profile) {
            Object nickname = profile.get("nickname");

            if (nickname instanceof String nicknameValue && !nicknameValue.isBlank()) {
                name = nicknameValue;
            }
        }

        return new OAuth2UserInfo(
                Social.KAKAO,
                String.valueOf(id),
                email,
                name
        );
    }

    private User findOrCreateSocialUser(OAuth2UserInfo userInfo) {
        return userRepository.findByEmail(userInfo.email())
                .map(existingUser -> {
                    validateNotLocked(existingUser);
                    return linkSocialAccount(
                            existingUser,
                            userInfo.socialType(),
                            userInfo.socialId()
                    );
                })
                .orElseGet(() -> {
                    try {
                        return userRepository.save(
                                User.builder()
                                        .email(userInfo.email())
                                        .name(userInfo.name())
                                        .password(null)
                                        .socialType(userInfo.socialType())
                                        .socialId(userInfo.socialId())
                                        .build()
                        );
                    } catch (DataIntegrityViolationException e) {
                        return userRepository.findByEmail(userInfo.email())
                                .map(existingUser -> {
                                    validateNotLocked(existingUser);
                                    return linkSocialAccount(
                                            existingUser,
                                            userInfo.socialType(),
                                            userInfo.socialId()
                                    );
                                })
                                .orElseThrow(AuthAuthenticationFailedException::new);
                    }
                });
    }

    private User linkSocialAccount(User existingUser, Social socialType, String socialId) {
        if (existingUser.getSocialType() == null) {
            return existingUser.updateSocialInfo(socialType, socialId);
        }

        if (existingUser.getSocialType() != socialType) {
            throw new OAuth2LoginException(GlobalErrorCode.OAUTH2_ALREADY_LINKED_OTHER_SOCIAL);
        }

        if (existingUser.getSocialId() == null) {
            return existingUser.updateSocialInfo(socialType, socialId);
        }

        if (!Objects.equals(existingUser.getSocialId(), socialId)) {
            throw new OAuth2LoginException(GlobalErrorCode.OAUTH2_SOCIAL_ID_MISMATCH);
        }

        return existingUser;
    }

    private Map<String, Object> getRequiredMap(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);

        if (!(value instanceof Map<?, ?> mapValue)) {
            throw new OAuth2LoginException(GlobalErrorCode.OAUTH2_MISSING_ATTRIBUTE, key);
        }

        return mapValue.entrySet()
                .stream()
                .filter(entry -> entry.getKey() instanceof String)
                .collect(Collectors.toMap(
                        entry -> (String) entry.getKey(),
                        Map.Entry::getValue
                ));
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

    private void validateNotLocked(User user) {
        if (user.isLocked()) {
            throw new LockedException("잠긴 계정입니다.");
        }
    }
}