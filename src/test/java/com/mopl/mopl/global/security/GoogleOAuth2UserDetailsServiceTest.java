package com.mopl.mopl.global.security;

import com.mopl.mopl.domain.auth.exception.AuthAuthenticationFailedException;
import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.domain.user.entity.Social;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.mapper.UserMapper;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.auth.details.OAuth2UserAccountService;
import com.mopl.mopl.global.auth.details.OAuth2UserDetails;
import com.mopl.mopl.global.auth.details.OAuth2UserDetailsService;
import com.mopl.mopl.global.auth.extractor.GoogleOAuth2UserInfoExtractor;
import com.mopl.mopl.global.exception.oauth2.OAuth2LoginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class GoogleOAuth2UserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    private OAuth2UserDetailsService service;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        OAuth2UserAccountService accountService =
                new OAuth2UserAccountService(userRepository);

        service = new OAuth2UserDetailsService(
                userMapper,
                accountService,
                List.of(new GoogleOAuth2UserInfoExtractor())
        );

        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();

        service.setRestOperations(restTemplate);
    }

    @Test
    @DisplayName("구글 sub가 없으면 IllegalArgumentException이 발생한다")
    void loadUser_missingSub_throwIllegalArgumentException() {
        mockGoogleUserInfo("""
                {
                  "email": "google@test.com",
                  "name": "구글사용자",
                  "email_verified": true
                }
                """);

        assertThatThrownBy(() -> service.loadUser(oauth2UserRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Attribute value for 'sub' cannot be null");

        verifyNoInteractions(userRepository, userMapper);

        mockServer.verify();
    }

    @Test
    @DisplayName("기존 구글 소셜 사용자가 있으면 OAuth2UserDetails를 반환한다")
    void loadUser_existingGoogleUser_success() {
        User user = org.mockito.Mockito.mock(User.class);
        UserDto userDto = userDto("google@test.com", "구글사용자");

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
        given(user.isLocked()).willReturn(false);
        given(userMapper.toDto(user)).willReturn(userDto);

        OAuth2UserDetails result = (OAuth2UserDetails) service.loadUser(oauth2UserRequest());

        assertThat(result.getUserDto()).isEqualTo(userDto);
        assertThat(result.getAttributes().get("sub")).isEqualTo("google-sub-1");
        assertThat(result.getAttributes().get("email")).isEqualTo("google@test.com");

        verify(userRepository).findBySocialTypeAndSocialId(Social.GOOGLE, "google-sub-1");
        verify(userMapper).toDto(user);

        mockServer.verify();
    }

    @Test
    @DisplayName("구글 이메일 인증이 완료되지 않았으면 OAuth2LoginException이 발생한다")
    void loadUser_emailNotVerified_throwOAuth2LoginException() {
        mockGoogleUserInfo("""
                {
                  "sub": "google-sub-1",
                  "email": "google@test.com",
                  "name": "구글사용자",
                  "email_verified": false
                }
                """);

        assertThatThrownBy(() -> service.loadUser(oauth2UserRequest()))
                .isInstanceOf(OAuth2LoginException.class);

        verifyNoInteractions(userRepository, userMapper);

        mockServer.verify();
    }

    @Test
    @DisplayName("구글 email이 없으면 OAuth2LoginException이 발생한다")
    void loadUser_missingEmail_throwOAuth2LoginException() {
        mockGoogleUserInfo("""
                {
                  "sub": "google-sub-1",
                  "name": "구글사용자",
                  "email_verified": true
                }
                """);

        assertThatThrownBy(() -> service.loadUser(oauth2UserRequest()))
                .isInstanceOf(OAuth2LoginException.class);

        verifyNoInteractions(userRepository, userMapper);

        mockServer.verify();
    }

    @Test
    @DisplayName("같은 이메일 기존 사용자가 잠긴 계정이면 구글 계정을 연결하지 않고 LockedException이 발생한다")
    void loadUser_existingEmailLockedUser_throwLockedException() {
        User existingUser = org.mockito.Mockito.mock(User.class);

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
        given(existingUser.isLocked()).willReturn(true);

        assertThatThrownBy(() -> service.loadUser(oauth2UserRequest()))
                .isInstanceOf(LockedException.class)
                .hasMessageContaining("잠긴 계정입니다.");

        verify(existingUser, org.mockito.Mockito.never())
                .updateSocialInfo(org.mockito.Mockito.any(), org.mockito.Mockito.anyString());
        verifyNoInteractions(userMapper);

        mockServer.verify();
    }

    @Test
    @DisplayName("신규 저장 중 이메일 중복 후 재조회된 사용자가 잠긴 계정이면 LockedException이 발생한다")
    void loadUser_saveDataIntegrityViolation_lockedUser_throwLockedException() {
        User existingUser = org.mockito.Mockito.mock(User.class);

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
                .willReturn(Optional.empty())
                .willReturn(Optional.of(existingUser));
        given(userRepository.save(any(User.class)))
                .willThrow(DataIntegrityViolationException.class);
        given(existingUser.isLocked()).willReturn(true);

        assertThatThrownBy(() -> service.loadUser(oauth2UserRequest()))
                .isInstanceOf(LockedException.class)
                .hasMessageContaining("잠긴 계정입니다.");

        verify(existingUser, org.mockito.Mockito.never())
                .updateSocialInfo(org.mockito.Mockito.any(), org.mockito.Mockito.anyString());
        verifyNoInteractions(userMapper);

        mockServer.verify();
    }

    @Test
    @DisplayName("기존 구글 사용자가 잠긴 계정이면 LockedException이 발생한다")
    void loadUser_lockedUser_throwLockedException() {
        User lockedUser = org.mockito.Mockito.mock(User.class);

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
        given(lockedUser.isLocked()).willReturn(true);

        assertThatThrownBy(() -> service.loadUser(oauth2UserRequest()))
                .isInstanceOf(LockedException.class)
                .hasMessageContaining("잠긴 계정입니다.");

        verifyNoInteractions(userMapper);

        mockServer.verify();
    }

    @Test
    @DisplayName("소셜 계정이 없고 같은 이메일 사용자의 socialType이 null이면 구글 계정을 연결한다")
    void loadUser_existingEmailUser_socialTypeNull_linkGoogleAccount() {
        User existingUser = org.mockito.Mockito.mock(User.class);
        UserDto userDto = userDto("google@test.com", "구글사용자");

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
        given(existingUser.isLocked()).willReturn(false);
        given(userMapper.toDto(existingUser)).willReturn(userDto);

        OAuth2UserDetails result = (OAuth2UserDetails) service.loadUser(oauth2UserRequest());

        assertThat(result.getUserDto()).isEqualTo(userDto);

        verify(existingUser).updateSocialInfo(Social.GOOGLE, "google-sub-1");

        mockServer.verify();
    }

    @Test
    @DisplayName("같은 이메일 사용자가 다른 소셜 타입이면 OAuth2LoginException이 발생한다")
    void loadUser_existingEmailUser_otherSocialType_throwOAuth2LoginException() {
        User existingUser = org.mockito.Mockito.mock(User.class);

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
        given(existingUser.getSocialType()).willReturn(Social.KAKAO);

        assertThatThrownBy(() -> service.loadUser(oauth2UserRequest()))
                .isInstanceOf(OAuth2LoginException.class);

        verifyNoInteractions(userMapper);

        mockServer.verify();
    }

    @Test
    @DisplayName("같은 이메일 사용자의 구글 socialId가 다르면 OAuth2LoginException이 발생한다")
    void loadUser_existingEmailUser_socialIdMismatch_throwOAuth2LoginException() {
        User existingUser = org.mockito.Mockito.mock(User.class);

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
        given(existingUser.getSocialType()).willReturn(Social.GOOGLE);
        given(existingUser.getSocialId()).willReturn("other-google-sub");

        assertThatThrownBy(() -> service.loadUser(oauth2UserRequest()))
                .isInstanceOf(OAuth2LoginException.class);

        verifyNoInteractions(userMapper);

        mockServer.verify();
    }

    @Test
    @DisplayName("소셜 계정과 이메일 사용자가 없으면 신규 구글 사용자를 저장한다")
    void loadUser_newGoogleUser_saveUser() {
        User savedUser = org.mockito.Mockito.mock(User.class);
        UserDto userDto = userDto("google@test.com", "구글사용자");

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
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(savedUser.isLocked()).willReturn(false);
        given(userMapper.toDto(savedUser)).willReturn(userDto);

        OAuth2UserDetails result = (OAuth2UserDetails) service.loadUser(oauth2UserRequest());

        assertThat(result.getUserDto()).isEqualTo(userDto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User savedArgument = captor.getValue();
        assertThat(savedArgument.getEmail()).isEqualTo("google@test.com");
        assertThat(savedArgument.getName()).isEqualTo("구글사용자");
        assertThat(savedArgument.getSocialType()).isEqualTo(Social.GOOGLE);
        assertThat(savedArgument.getSocialId()).isEqualTo("google-sub-1");

        mockServer.verify();
    }

    @Test
    @DisplayName("구글 name이 없으면 email을 이름으로 사용해서 신규 저장한다")
    void loadUser_googleNameMissing_useEmailAsName() {
        User savedUser = org.mockito.Mockito.mock(User.class);
        UserDto userDto = userDto("google@test.com", "google@test.com");

        mockGoogleUserInfo("""
                {
                  "sub": "google-sub-1",
                  "email": "google@test.com",
                  "email_verified": true
                }
                """);

        given(userRepository.findBySocialTypeAndSocialId(Social.GOOGLE, "google-sub-1"))
                .willReturn(Optional.empty());
        given(userRepository.findByEmail("google@test.com"))
                .willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(savedUser.isLocked()).willReturn(false);
        given(userMapper.toDto(savedUser)).willReturn(userDto);

        OAuth2UserDetails result = (OAuth2UserDetails) service.loadUser(oauth2UserRequest());

        assertThat(result.getUserDto()).isEqualTo(userDto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        assertThat(captor.getValue().getName()).isEqualTo("google@test.com");

        mockServer.verify();
    }

    @Test
    @DisplayName("신규 저장 중 이메일 중복이 발생하면 이메일 재조회 후 구글 계정을 연결한다")
    void loadUser_saveDataIntegrityViolation_findByEmailAndLinkGoogleAccount() {
        User existingUser = org.mockito.Mockito.mock(User.class);
        UserDto userDto = userDto("google@test.com", "구글사용자");

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
                .willReturn(Optional.empty())
                .willReturn(Optional.of(existingUser));
        given(userRepository.save(any(User.class)))
                .willThrow(DataIntegrityViolationException.class);
        given(existingUser.getSocialType()).willReturn(null);
        given(existingUser.updateSocialInfo(Social.GOOGLE, "google-sub-1"))
                .willReturn(existingUser);
        given(existingUser.isLocked()).willReturn(false);
        given(userMapper.toDto(existingUser)).willReturn(userDto);

        OAuth2UserDetails result = (OAuth2UserDetails) service.loadUser(oauth2UserRequest());

        assertThat(result.getUserDto()).isEqualTo(userDto);

        verify(existingUser).updateSocialInfo(Social.GOOGLE, "google-sub-1");

        mockServer.verify();
    }

    @Test
    @DisplayName("신규 저장 중 이메일 중복 후 재조회도 실패하면 AuthAuthenticationFailedException이 발생한다")
    void loadUser_saveDataIntegrityViolation_findByEmailEmpty_throwAuthAuthenticationFailedException() {
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
                .willReturn(Optional.empty())
                .willReturn(Optional.empty());
        given(userRepository.save(any(User.class)))
                .willThrow(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> service.loadUser(oauth2UserRequest()))
                .isInstanceOf(AuthAuthenticationFailedException.class);

        verifyNoInteractions(userMapper);

        mockServer.verify();
    }

    private void mockGoogleUserInfo(String responseBody) {
        mockServer.expect(requestTo("https://openidconnect.googleapis.com/v1/userinfo"))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
    }

    private OAuth2UserRequest oauth2UserRequest() {
        ClientRegistration clientRegistration = ClientRegistration
                .withRegistrationId("google")
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8080/login/oauth2/code/google")
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

    private UserDto userDto(String email, String name) {
        return new UserDto(
                UUID.randomUUID(),
                Instant.now(),
                email,
                name,
                null,
                Role.USER,
                false
        );
    }
}