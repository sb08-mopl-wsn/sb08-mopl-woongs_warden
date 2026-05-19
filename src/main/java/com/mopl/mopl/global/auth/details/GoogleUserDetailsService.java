package com.mopl.mopl.global.auth.details;

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

@Service
@RequiredArgsConstructor
public class GoogleUserDetailsService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        if (!"google".equals(registrationId)) {
            throw new OAuth2LoginException(GlobalErrorCode.OAUTH2_UNSUPPORTED_PROVIDER);
        }

        Map<String, Object> attributes = oauth2User.getAttributes();

        String socialId = getRequiredString(attributes, "sub");
        String email = getRequiredString(attributes, "email");
        String name = getStringOrDefault(attributes, "name", email);
        Boolean emailVerified = (Boolean) attributes.get("email_verified");

        if (!Boolean.TRUE.equals(emailVerified)) {
            throw new OAuth2LoginException(GlobalErrorCode.OAUTH2_EMAIL_NOT_VERIFIED);
        }

        User user = userRepository.findBySocialTypeAndSocialId(Social.GOOGLE, socialId)
                .orElseGet(() -> findOrCreateGoogleUser(email, name, socialId));

        if (user.isLocked()) {
            throw new LockedException("잠긴 계정입니다.");
        }

        return new GoogleUserDetails(userMapper.toDto(user), attributes);
    }

    private User findOrCreateGoogleUser(String email, String name, String socialId) {
        return userRepository.findByEmail(email)
                .map(existingUser -> linkGoogleAccount(existingUser, socialId))
                .orElseGet(() -> {
                    try {
                        return userRepository.save(
                                User.builder()
                                        .email(email)
                                        .name(name)
                                        .password(null)
                                        .socialType(Social.GOOGLE)
                                        .socialId(socialId)
                                        .build()
                        );
                    } catch (DataIntegrityViolationException e) {
                        return userRepository.findByEmail(email)
                                .map(user -> linkGoogleAccount(user, socialId))
                                .orElseThrow(AuthAuthenticationFailedException::new);
                    }
                });
    }

    private User linkGoogleAccount(User existingUser, String socialId) {
        if (existingUser.getSocialType() == null) {
            return existingUser.updateSocialInfo(Social.GOOGLE, socialId);
        }

        if (existingUser.getSocialType() != Social.GOOGLE) {
            throw new OAuth2LoginException(GlobalErrorCode.OAUTH2_ALREADY_LINKED_OTHER_SOCIAL);
        }

        if (existingUser.getSocialId() == null) {
            return existingUser.updateSocialInfo(Social.GOOGLE, socialId);
        }

        if (!Objects.equals(existingUser.getSocialId(), socialId)) {
            throw new OAuth2LoginException(GlobalErrorCode.OAUTH2_SOCIAL_ID_MISMATCH);
        }

        return existingUser;
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