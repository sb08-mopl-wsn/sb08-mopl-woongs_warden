package com.mopl.mopl.global.security;

import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.domain.user.entity.Social;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.mapper.UserMapper;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.auth.details.GoogleUserDetails;
import com.mopl.mopl.global.auth.details.GoogleUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class GoogleUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    private GoogleUserDetailsService service;

    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        service = new GoogleUserDetailsService(userRepository, userMapper);

        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();

        service.setRestOperations(restTemplate);
    }

    @Test
    @DisplayName("기존 구글 소셜 사용자가 있으면 GoogleUserDetails를 반환한다")
    void loadUser_existingGoogleUser_success() {
        User user = mockUser(false);
        UserDto userDto = userDto();

        mockGoogleUserInfo("""
                {
                  "sub": "google-sub-1",
                  "email": "google@test.com",
                  "name": "구글사용자",
                  "email_verified": true
                }
                """);

        given(userRepository.findBySocialTypeAndSocialId(Social.GOOGLE, "google-sub-1"))
                .willReturn(Optional.of(user));
        given(userMapper.toDto(user)).willReturn(userDto);

        GoogleUserDetails result = (GoogleUserDetails) service.loadUser(oauth2UserRequest("google"));

        assertThat(result.getUserDto()).isEqualTo(userDto);
        assertThat(result.getAttributes().get("sub")).isEqualTo("google-sub-1");
        assertThat(result.getAttributes().get("email")).isEqualTo("google@test.com");

        verify(userRepository).findBySocialTypeAndSocialId(Social.GOOGLE, "google-sub-1");
        verify(userMapper).toDto(user);

        mockServer.verify();
    }

    @Test
    @DisplayName("지원하지 않는 provider면 OAuth2AuthenticationException이 발생한다")
    void loadUser_unsupportedProvider_throwOAuth2AuthenticationException() {
        mockGoogleUserInfo("""
                {
                  "sub": "google-sub-1",
                  "email": "google@test.com",
                  "name": "구글사용자",
                  "email_verified": true
                }
                """);

        assertThatThrownBy(() -> service.loadUser(oauth2UserRequest("kakao")))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("지원하지 않는 소셜 로그인입니다.");

        verifyNoInteractions(userRepository, userMapper);
    }

    @Test
    @DisplayName("이메일 인증이 완료되지 않았으면 OAuth2AuthenticationException이 발생한다")
    void loadUser_emailNotVerified_throwOAuth2AuthenticationException() {
        mockGoogleUserInfo("""
                {
                  "sub": "google-sub-1",
                  "email": "google@test.com",
                  "name": "구글사용자",
                  "email_verified": false
                }
                """);

        assertThatThrownBy(() -> service.loadUser(oauth2UserRequest("google")))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("구글 이메일 인증이 완료되지 않은 계정입니다.");

        verifyNoInteractions(userRepository, userMapper);
    }

    @Test
    @DisplayName("기존 사용자가 잠긴 계정이면 LockedException이 발생한다")
    void loadUser_lockedUser_throwLockedException() {
        User lockedUser = mockUser(true);

        mockGoogleUserInfo("""
                {
                  "sub": "google-sub-1",
                  "email": "google@test.com",
                  "name": "구글사용자",
                  "email_verified": true
                }
                """);

        given(userRepository.findBySocialTypeAndSocialId(Social.GOOGLE, "google-sub-1"))
                .willReturn(Optional.of(lockedUser));

        assertThatThrownBy(() -> service.loadUser(oauth2UserRequest("google")))
                .isInstanceOf(LockedException.class)
                .hasMessageContaining("잠긴 계정입니다.");

        verify(userRepository).findBySocialTypeAndSocialId(Social.GOOGLE, "google-sub-1");
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("소셜 계정이 없고 같은 이메일 사용자가 있으면 구글 계정을 연결한다")
    void loadUser_existingEmailUser_linkGoogleAccount() {
        User existingUser = mockUser(false);
        UserDto userDto = userDto();

        mockGoogleUserInfo("""
                {
                  "sub": "google-sub-1",
                  "email": "google@test.com",
                  "name": "구글사용자",
                  "email_verified": true
                }
                """);

        given(userRepository.findBySocialTypeAndSocialId(Social.GOOGLE, "google-sub-1"))
                .willReturn(Optional.empty());
        given(userRepository.findByEmail("google@test.com"))
                .willReturn(Optional.of(existingUser));
        given(existingUser.getSocialType()).willReturn(null);
        given(existingUser.updateSocialInfo(Social.GOOGLE, "google-sub-1"))
                .willReturn(existingUser);
        given(userMapper.toDto(existingUser)).willReturn(userDto);

        GoogleUserDetails result = (GoogleUserDetails) service.loadUser(oauth2UserRequest("google"));

        assertThat(result.getUserDto()).isEqualTo(userDto);

        verify(existingUser).updateSocialInfo(Social.GOOGLE, "google-sub-1");
    }

    @Test
    @DisplayName("소셜 계정과 이메일 사용자가 없으면 신규 구글 사용자를 저장한다")
    void loadUser_newGoogleUser_saveUser() {
        User savedUser = mockUser(false);
        UserDto userDto = userDto();

        mockGoogleUserInfo("""
                {
                  "sub": "google-sub-1",
                  "email": "google@test.com",
                  "name": "구글사용자",
                  "email_verified": true
                }
                """);

        given(userRepository.findBySocialTypeAndSocialId(Social.GOOGLE, "google-sub-1"))
                .willReturn(Optional.empty());
        given(userRepository.findByEmail("google@test.com"))
                .willReturn(Optional.empty());
        given(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .willReturn(savedUser);
        given(userMapper.toDto(savedUser)).willReturn(userDto);

        GoogleUserDetails result = (GoogleUserDetails) service.loadUser(oauth2UserRequest("google"));

        assertThat(result.getUserDto()).isEqualTo(userDto);
        verify(userRepository).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    private void mockGoogleUserInfo(String responseBody) {
        mockServer.expect(requestTo("https://openidconnect.googleapis.com/v1/userinfo"))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
    }

    private OAuth2UserRequest oauth2UserRequest(String registrationId) {
        ClientRegistration clientRegistration = ClientRegistration
                .withRegistrationId(registrationId)
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8080/login/oauth2/code/" + registrationId)
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .userNameAttributeName("sub")
                .clientName("Google")
                .scope("email", "profile")
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "access-token",
                Instant.now(),
                Instant.now().plusSeconds(300)
        );

        return new OAuth2UserRequest(clientRegistration, accessToken);
    }

    private User mockUser(boolean locked) {
        User user = org.mockito.Mockito.mock(User.class);
        given(user.isLocked()).willReturn(locked);
        return user;
    }

    private UserDto userDto() {
        return new UserDto(
                UUID.randomUUID(),
                Instant.now(),
                "google@test.com",
                "구글사용자",
                null,
                Role.USER,
                false
        );
    }
}