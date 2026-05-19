package com.mopl.mopl.global.auth.details;

import com.mopl.mopl.domain.auth.dto.OAuth2UserInfo;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.mapper.UserMapper;
import com.mopl.mopl.global.auth.extractor.OAuth2UserInfoExtractor;
import com.mopl.mopl.global.exception.GlobalErrorCode;
import com.mopl.mopl.global.exception.oauth2.OAuth2LoginException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OAuth2UserDetailsService extends DefaultOAuth2UserService {

    private final UserMapper userMapper;
    private final OAuth2UserAccountService oAuth2UserAccountService;
    private final List<OAuth2UserInfoExtractor> extractors;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oauth2User.getAttributes();

        OAuth2UserInfo userInfo = extractUserInfo(registrationId, attributes);
        User user = oAuth2UserAccountService.findOrCreateSocialUser(userInfo);

        return new OAuth2UserDetails(userMapper.toDto(user), attributes);
    }

    private OAuth2UserInfo extractUserInfo(String registrationId, Map<String, Object> attributes) {
        return extractors.stream()
                .filter(extractor -> extractor.supports(registrationId))
                .findFirst()
                .orElseThrow(() -> new OAuth2LoginException(GlobalErrorCode.OAUTH2_UNSUPPORTED_PROVIDER))
                .extract(attributes);
    }
}