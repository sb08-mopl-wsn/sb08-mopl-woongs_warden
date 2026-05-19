package com.mopl.mopl.global.security;

import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.domain.user.entity.Social;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.mapper.UserMapper;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.auth.details.OAuth2UserDetails;
import com.mopl.mopl.global.auth.details.OAuth2UserDetailsService;
import com.mopl.mopl.global.exception.oauth2.OAuth2LoginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class KakaoOAuth2UserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    private OAuth2UserDetailsService service;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        service = new OAuth2UserDetailsService(userRepository, userMapper);

        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();

        service.setRestOperations(restTemplate);
    }

    @Test
    @DisplayName("기존 카카오 소셜 사용자가 있으면 OAuth2UserDetails를 반환한다")
    void loadUser_existingKakaoUser_success() {
        User user = org.mockito.Mockito.mock(User.class);
        UserDto userDto = userDto("kakao@test.com", "카카오사용자");

        mockKakaoUserInfo("""
                {
                  "id": 12345,
                  "kakao_account": {
                    "email": "kakao@test.com",
                    "is_email_verified": true,
                    "profile": {
                      "nickname": "카카오사용자"
                    }
                  }
                }
                """);

        given(userRepository.findBySocialTypeAndSocialId(Social.KAKAO, "12345"))
                .willReturn(Optional.of(user));
        given(user.isLocked()).willReturn(false);
        given(userMapper.toDto(user)).willReturn(userDto);

        OAuth2UserDetails result = (OAuth2UserDetails) service.loadUser(oauth2UserRequest());

        assertThat(result.getUserDto()).isEqualTo(userDto);
        assertThat(result.getAttributes().get("id")).isEqualTo(12345);

        verify(userRepository).findBySocialTypeAndSocialId(Social.KAKAO, "12345");
        verify(userMapper).toDto(user);

        mockServer.verify();
    }

    @Test
    @DisplayName("카카오 id가 없으면 IllegalArgumentException이 발생한다")
    void loadUser_missingId_throwIllegalArgumentException() {
        mockKakaoUserInfo("""
            {
              "kakao_account": {
                "email": "kakao@test.com",
                "is_email_verified": true
              }
            }
            """);

        assertThatThrownBy(() -> service.loadUser(oauth2UserRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Attribute value for 'id' cannot be null");

        verifyNoInteractions(userRepository, userMapper);

        mockServer.verify();
    }

    @Test
    @DisplayName("카카오 kakao_account가 없으면 OAuth2LoginException이 발생한다")
    void loadUser_missingKakaoAccount_throwOAuth2LoginException() {
        mockKakaoUserInfo("""
                {
                  "id": 12345
                }
                """);

        assertThatThrownBy(() -> service.loadUser(oauth2UserRequest()))
                .isInstanceOf(OAuth2LoginException.class);

        verifyNoInteractions(userRepository, userMapper);

        mockServer.verify();
    }

    @Test
    @DisplayName("카카오 email이 없으면 OAuth2LoginException이 발생한다")
    void loadUser_missingEmail_throwOAuth2LoginException() {
        mockKakaoUserInfo("""
                {
                  "id": 12345,
                  "kakao_account": {
                    "is_email_verified": true,
                    "profile": {
                      "nickname": "카카오사용자"
                    }
                  }
                }
                """);

        assertThatThrownBy(() -> service.loadUser(oauth2UserRequest()))
                .isInstanceOf(OAuth2LoginException.class);

        verifyNoInteractions(userRepository, userMapper);

        mockServer.verify();
    }

    @Test
    @DisplayName("카카오 이메일 인증이 false면 OAuth2LoginException이 발생한다")
    void loadUser_emailNotVerifiedFalse_throwOAuth2LoginException() {
        mockKakaoUserInfo("""
                {
                  "id": 12345,
                  "kakao_account": {
                    "email": "kakao@test.com",
                    "is_email_verified": false,
                    "profile": {
                      "nickname": "카카오사용자"
                    }
                  }
                }
                """);

        assertThatThrownBy(() -> service.loadUser(oauth2UserRequest()))
                .isInstanceOf(OAuth2LoginException.class);

        verifyNoInteractions(userRepository, userMapper);

        mockServer.verify();
    }

    @Test
    @DisplayName("기존 카카오 사용자가 잠긴 계정이면 LockedException이 발생한다")
    void loadUser_lockedUser_throwLockedException() {
        User lockedUser = org.mockito.Mockito.mock(User.class);

        mockKakaoUserInfo("""
                {
                  "id": 12345,
                  "kakao_account": {
                    "email": "kakao@test.com",
                    "is_email_verified": true,
                    "profile": {
                      "nickname": "카카오사용자"
                    }
                  }
                }
                """);

        given(userRepository.findBySocialTypeAndSocialId(Social.KAKAO, "12345"))
                .willReturn(Optional.of(lockedUser));
        given(lockedUser.isLocked()).willReturn(true);

        assertThatThrownBy(() -> service.loadUser(oauth2UserRequest()))
                .isInstanceOf(LockedException.class)
                .hasMessageContaining("잠긴 계정입니다.");

        verifyNoInteractions(userMapper);

        mockServer.verify();
    }

    @Test
    @DisplayName("소셜 계정이 없고 같은 이메일 사용자의 socialType이 null이면 카카오 계정을 연결한다")
    void loadUser_existingEmailUser_socialTypeNull_linkKakaoAccount() {
        User existingUser = org.mockito.Mockito.mock(User.class);
        UserDto userDto = userDto("kakao@test.com", "카카오사용자");

        mockKakaoUserInfo("""
                {
                  "id": 12345,
                  "kakao_account": {
                    "email": "kakao@test.com",
                    "is_email_verified": true,
                    "profile": {
                      "nickname": "카카오사용자"
                    }
                  }
                }
                """);

        given(userRepository.findBySocialTypeAndSocialId(Social.KAKAO, "12345"))
                .willReturn(Optional.empty());
        given(userRepository.findByEmail("kakao@test.com"))
                .willReturn(Optional.of(existingUser));
        given(existingUser.getSocialType()).willReturn(null);
        given(existingUser.updateSocialInfo(Social.KAKAO, "12345"))
                .willReturn(existingUser);
        given(existingUser.isLocked()).willReturn(false);
        given(userMapper.toDto(existingUser)).willReturn(userDto);

        OAuth2UserDetails result = (OAuth2UserDetails) service.loadUser(oauth2UserRequest());

        assertThat(result.getUserDto()).isEqualTo(userDto);

        verify(existingUser).updateSocialInfo(Social.KAKAO, "12345");

        mockServer.verify();
    }

    @Test
    @DisplayName("같은 이메일 사용자가 다른 소셜 타입이면 OAuth2LoginException이 발생한다")
    void loadUser_existingEmailUser_otherSocialType_throwOAuth2LoginException() {
        User existingUser = org.mockito.Mockito.mock(User.class);

        mockKakaoUserInfo("""
                {
                  "id": 12345,
                  "kakao_account": {
                    "email": "kakao@test.com",
                    "is_email_verified": true,
                    "profile": {
                      "nickname": "카카오사용자"
                    }
                  }
                }
                """);

        given(userRepository.findBySocialTypeAndSocialId(Social.KAKAO, "12345"))
                .willReturn(Optional.empty());
        given(userRepository.findByEmail("kakao@test.com"))
                .willReturn(Optional.of(existingUser));
        given(existingUser.getSocialType()).willReturn(Social.GOOGLE);

        assertThatThrownBy(() -> service.loadUser(oauth2UserRequest()))
                .isInstanceOf(OAuth2LoginException.class);

        verifyNoInteractions(userMapper);

        mockServer.verify();
    }

    @Test
    @DisplayName("같은 이메일 사용자의 카카오 socialId가 다르면 OAuth2LoginException이 발생한다")
    void loadUser_existingEmailUser_socialIdMismatch_throwOAuth2LoginException() {
        User existingUser = org.mockito.Mockito.mock(User.class);

        mockKakaoUserInfo("""
                {
                  "id": 12345,
                  "kakao_account": {
                    "email": "kakao@test.com",
                    "is_email_verified": true,
                    "profile": {
                      "nickname": "카카오사용자"
                    }
                  }
                }
                """);

        given(userRepository.findBySocialTypeAndSocialId(Social.KAKAO, "12345"))
                .willReturn(Optional.empty());
        given(userRepository.findByEmail("kakao@test.com"))
                .willReturn(Optional.of(existingUser));
        given(existingUser.getSocialType()).willReturn(Social.KAKAO);
        given(existingUser.getSocialId()).willReturn("other-kakao-id");

        assertThatThrownBy(() -> service.loadUser(oauth2UserRequest()))
                .isInstanceOf(OAuth2LoginException.class);

        verifyNoInteractions(userMapper);

        mockServer.verify();
    }

    @Test
    @DisplayName("소셜 계정과 이메일 사용자가 없으면 신규 카카오 사용자를 저장한다")
    void loadUser_newKakaoUser_saveUser() {
        User savedUser = org.mockito.Mockito.mock(User.class);
        UserDto userDto = userDto("kakao@test.com", "카카오사용자");

        mockKakaoUserInfo("""
                {
                  "id": 12345,
                  "kakao_account": {
                    "email": "kakao@test.com",
                    "is_email_verified": true,
                    "profile": {
                      "nickname": "카카오사용자"
                    }
                  }
                }
                """);

        given(userRepository.findBySocialTypeAndSocialId(Social.KAKAO, "12345"))
                .willReturn(Optional.empty());
        given(userRepository.findByEmail("kakao@test.com"))
                .willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(savedUser.isLocked()).willReturn(false);
        given(userMapper.toDto(savedUser)).willReturn(userDto);

        OAuth2UserDetails result = (OAuth2UserDetails) service.loadUser(oauth2UserRequest());

        assertThat(result.getUserDto()).isEqualTo(userDto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User savedArgument = captor.getValue();
        assertThat(savedArgument.getEmail()).isEqualTo("kakao@test.com");
        assertThat(savedArgument.getName()).isEqualTo("카카오사용자");
        assertThat(savedArgument.getSocialType()).isEqualTo(Social.KAKAO);
        assertThat(savedArgument.getSocialId()).isEqualTo("12345");

        mockServer.verify();
    }

    @Test
    @DisplayName("카카오 nickname이 없으면 email을 이름으로 사용해서 신규 저장한다")
    void loadUser_nicknameMissing_useEmailAsName() {
        User savedUser = org.mockito.Mockito.mock(User.class);
        UserDto userDto = userDto("kakao@test.com", "kakao@test.com");

        mockKakaoUserInfo("""
                {
                  "id": 12345,
                  "kakao_account": {
                    "email": "kakao@test.com",
                    "is_email_verified": true,
                    "profile": {}
                  }
                }
                """);

        given(userRepository.findBySocialTypeAndSocialId(Social.KAKAO, "12345"))
                .willReturn(Optional.empty());
        given(userRepository.findByEmail("kakao@test.com"))
                .willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(savedUser.isLocked()).willReturn(false);
        given(userMapper.toDto(savedUser)).willReturn(userDto);

        OAuth2UserDetails result = (OAuth2UserDetails) service.loadUser(oauth2UserRequest());

        assertThat(result.getUserDto()).isEqualTo(userDto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        assertThat(captor.getValue().getName()).isEqualTo("kakao@test.com");

        mockServer.verify();
    }

    private void mockKakaoUserInfo(String responseBody) {
        mockServer.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
    }

    private OAuth2UserRequest oauth2UserRequest() {
        ClientRegistration clientRegistration = ClientRegistration
                .withRegistrationId("kakao")
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8080/login/oauth2/code/kakao")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me")
                .userNameAttributeName("id")
                .clientName("Kakao")
                .scope("profile_nickname", "account_email")
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