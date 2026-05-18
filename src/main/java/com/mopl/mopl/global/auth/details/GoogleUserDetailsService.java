package com.mopl.mopl.global.auth.details;

import com.mopl.mopl.domain.auth.exception.AuthAuthenticationFailedException;
import com.mopl.mopl.domain.user.entity.Social;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.mapper.UserMapper;
import com.mopl.mopl.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
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
            throw oauth2Exception("unsupported_provider", "지원하지 않는 소셜 로그인입니다.");
        }

        Map<String, Object> attributes = oauth2User.getAttributes();

        String socialId = getRequiredString(attributes, "sub");
        String email = getRequiredString(attributes, "email");
        String name = getStringOrDefault(attributes, "name", email);
        Boolean emailVerified = (Boolean) attributes.get("email_verified");

        if (!Boolean.TRUE.equals(emailVerified)) {
            throw oauth2Exception("email_not_verified", "구글 이메일 인증이 완료되지 않은 계정입니다.");
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
                        // 동시에 생성된 경우, 재조회하여 링크
                        return userRepository.findByEmail(email)
                                .map(user -> linkGoogleAccount(user, socialId))
                                .orElseThrow(() -> new AuthAuthenticationFailedException());
                    }
                });
    }

    private User linkGoogleAccount(User existingUser, String socialId) {
        if (existingUser.getSocialType() == null) {
            return existingUser.updateSocialInfo(Social.GOOGLE, socialId);
        }

        if (existingUser.getSocialType() != Social.GOOGLE) {
            throw oauth2Exception("already_linked_other_social", "이미 다른 소셜 계정으로 가입된 이메일입니다.");
        }

        if (existingUser.getSocialId() == null) {
            return existingUser.updateSocialInfo(Social.GOOGLE, socialId);
        }

        if (!Objects.equals(existingUser.getSocialId(), socialId)) {
            throw oauth2Exception("social_id_mismatch", "기존 구글 계정 정보와 일치하지 않습니다.");
        }

        return existingUser;
    }

    private String getRequiredString(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);

        if (!(value instanceof String stringValue) || stringValue.isBlank()) {
            throw oauth2Exception("missing_attribute", "구글 사용자 정보에 " + key + " 값이 없습니다.");
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

    private OAuth2AuthenticationException oauth2Exception(String code, String message) {
        return new OAuth2AuthenticationException(new OAuth2Error(code), message);
    }
}