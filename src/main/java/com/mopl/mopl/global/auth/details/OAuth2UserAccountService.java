package com.mopl.mopl.global.auth.details;

import com.mopl.mopl.domain.auth.dto.OAuth2UserInfo;
import com.mopl.mopl.domain.auth.exception.AuthAuthenticationFailedException;
import com.mopl.mopl.domain.user.entity.Social;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.exception.GlobalErrorCode;
import com.mopl.mopl.global.exception.oauth2.OAuth2LoginException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OAuth2UserAccountService {
    private final UserRepository userRepository;

    @Transactional
    public User findOrCreateSocialUser(OAuth2UserInfo userInfo) {
        User user = userRepository.findBySocialTypeAndSocialId(
                        userInfo.socialType(),
                        userInfo.socialId()
                )
                .orElseGet(() -> findOrCreateByEmail(userInfo));

        validateNotLocked(user);
        return user;
    }

    private User findOrCreateByEmail(OAuth2UserInfo userInfo) {
        return userRepository.findByEmail(userInfo.email())
                .map(existingUser -> {
                    validateNotLocked(existingUser);
                    return linkSocialAccount(
                            existingUser,
                            userInfo.socialType(),
                            userInfo.socialId()
                    );
                })
                .orElseGet(() -> saveSocialUser(userInfo));
    }

    private User saveSocialUser(OAuth2UserInfo userInfo) {
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

    private void validateNotLocked(User user) {
        if (user.isLocked()) {
            throw new LockedException("잠긴 계정입니다.");
        }
    }
}