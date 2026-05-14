package com.mopl.mopl.domain.auth;

import com.mopl.mopl.domain.auth.exception.AuthExpiredTokenException;
import com.mopl.mopl.domain.auth.exception.AuthFailedRefrshToken;
import com.mopl.mopl.domain.auth.exception.AuthInvalidTokenException;
import com.mopl.mopl.domain.auth.service.AuthServiceImpl;
import com.mopl.mopl.domain.jwt.dto.JwtDTO;
import com.mopl.mopl.domain.jwt.dto.JwtInformation;
import com.mopl.mopl.domain.jwt.registry.JwtRegistry;
import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.exception.UserNotFoundException;
import com.mopl.mopl.domain.user.mapper.UserMapper;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.auth.JwtTokenProvider;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import com.mopl.mopl.global.auth.details.MoplUserDetailsService;
import com.mopl.mopl.global.event.user.UserPasswordInitEvent;
import com.nimbusds.jose.JOSEException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtRegistry jwtRegistry;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private MoplUserDetailsService userDetailsService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "upper", "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        ReflectionTestUtils.setField(authService, "lower", "abcdefghijklmnopqrstuvwxyz");
        ReflectionTestUtils.setField(authService, "digit", "0123456789");
        ReflectionTestUtils.setField(authService, "special", "@$!%*#?&");
    }

    @Nested
    @DisplayName("현재 사용자 조회")
    class GetCurrentUserInfo {

        @Test
        @DisplayName("현재 사용자 조회 성공")
        void getCurrentUserInfo_success() {
            UUID userId = UUID.randomUUID();

            UserDto userDto = new UserDto(
                    userId,
                    null,
                    "test@test.com",
                    "tester",
                    null,
                    Role.USER,
                    false
            );

            MoplUserDetails userDetails = new MoplUserDetails(userDto, "encodedPassword");
            User user = mock(User.class);

            when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
            when(userMapper.toDto(user)).thenReturn(userDto);

            UserDto result = authService.getCurrentUserInfo(userDetails);

            assertThat(result).isEqualTo(userDto);
            verify(userRepository).findByEmail("test@test.com");
            verify(userMapper).toDto(user);
        }

        @Test
        @DisplayName("UserDetails가 null이면 null 반환")
        void getCurrentUserInfo_nullUserDetails() {
            UserDto result = authService.getCurrentUserInfo(null);

            assertThat(result).isNull();
            verifyNoInteractions(userRepository, userMapper);
        }

        @Test
        @DisplayName("사용자를 찾을 수 없으면 예외")
        void getCurrentUserInfo_userNotFound() {
            UserDto userDto = new UserDto(
                    UUID.randomUUID(),
                    null,
                    "missing@test.com",
                    "missing",
                    null,
                    Role.USER,
                    false
            );

            MoplUserDetails userDetails = new MoplUserDetails(userDto, "encodedPassword");

            when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.getCurrentUserInfo(userDetails))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("토큰 재발급")
    class Refresh {

        @Test
        @DisplayName("refresh 성공")
        void refresh_success() throws JOSEException {
            String oldRefreshToken = "old.refresh.token";
            String oldAccessToken = "old.access.token";
            String newAccessToken = "new.access.token";
            String newRefreshToken = "new.refresh.token";

            UserDto userDto = new UserDto(
                    UUID.randomUUID(),
                    null,
                    "admin@admin.com",
                    "관리자",
                    null,
                    Role.ADMIN,
                    false
            );

            JwtInformation oldJwtInfo = new JwtInformation(
                    userDto,
                    oldAccessToken,
                    oldRefreshToken
            );

            MoplUserDetails userDetails = new MoplUserDetails(userDto, "encodedPassword");

            when(jwtTokenProvider.validateRefreshToken(oldRefreshToken)).thenReturn(true);
            when(jwtRegistry.hasActiveJwtInformationByRefreshToken(oldRefreshToken)).thenReturn(true);
            when(jwtRegistry.getJwtInformationByRefreshToken(oldRefreshToken)).thenReturn(oldJwtInfo);
            when(jwtTokenProvider.getUserEmailFromToken(oldRefreshToken)).thenReturn("admin@admin.com");
            when(userDetailsService.loadUserByUsername("admin@admin.com")).thenReturn(userDetails);
            when(jwtTokenProvider.generateAccessToken(userDetails)).thenReturn(newAccessToken);
            when(jwtTokenProvider.generateRefreshToken(userDetails)).thenReturn(newRefreshToken);

            JwtDTO result = authService.refresh(oldRefreshToken, response);

            assertThat(result.userDto()).isEqualTo(userDto);
            assertThat(result.accessToken()).isEqualTo(newAccessToken);

            ArgumentCaptor<JwtInformation> captor = ArgumentCaptor.forClass(JwtInformation.class);

            verify(jwtRegistry).rotateJwtInformation(eq(oldRefreshToken), captor.capture());
            assertThat(captor.getValue().getUser()).isEqualTo(userDto);
            assertThat(captor.getValue().getAccessToken()).isEqualTo(newAccessToken);
            assertThat(captor.getValue().getRefreshToken()).isEqualTo(newRefreshToken);

            verify(jwtTokenProvider).addRefreshCookie(response, newRefreshToken);
            verify(jwtRegistry, never()).rollbackRotateJwtInformation(any(), any(), any());
        }

        @Test
        @DisplayName("refreshToken이 null이면 AuthInvalidTokenException")
        void refresh_fail_nullToken() {
            assertThatThrownBy(() -> authService.refresh(null, response))
                    .isInstanceOf(AuthInvalidTokenException.class);

            verify(jwtTokenProvider, never()).validateRefreshToken(any());
            verifyNoInteractions(jwtRegistry);
        }

        @Test
        @DisplayName("refreshToken 검증 실패 시 AuthInvalidTokenException")
        void refresh_fail_invalidToken() {
            String refreshToken = "invalid.refresh.token";

            when(jwtTokenProvider.validateRefreshToken(refreshToken)).thenReturn(false);

            assertThatThrownBy(() -> authService.refresh(refreshToken, response))
                    .isInstanceOf(AuthInvalidTokenException.class);

            verify(jwtRegistry, never()).hasActiveJwtInformationByRefreshToken(any());
            verify(jwtRegistry, never()).getJwtInformationByRefreshToken(any());
        }

        @Test
        @DisplayName("활성화된 refreshToken이 아니면 쿠키 만료 후 AuthExpiredTokenException")
        void refresh_fail_inactiveToken() {
            String refreshToken = "inactive.refresh.token";

            when(jwtTokenProvider.validateRefreshToken(refreshToken)).thenReturn(true);
            when(jwtRegistry.hasActiveJwtInformationByRefreshToken(refreshToken)).thenReturn(false);

            assertThatThrownBy(() -> authService.refresh(refreshToken, response))
                    .isInstanceOf(AuthExpiredTokenException.class);

            verify(jwtTokenProvider).expireRefreshCookie(response);
            verify(jwtRegistry, never()).getJwtInformationByRefreshToken(any());
        }

        @Test
        @DisplayName("토큰 생성 중 예외 발생 시 rollback 후 AuthFailedRefrshToken")
        void refresh_fail_tokenGenerationException() throws JOSEException {
            String oldRefreshToken = "old.refresh.token";
            String oldAccessToken = "old.access.token";

            UserDto userDto = new UserDto(
                    UUID.randomUUID(),
                    null,
                    "admin@admin.com",
                    "관리자",
                    null,
                    Role.ADMIN,
                    false
            );

            JwtInformation oldJwtInfo = new JwtInformation(
                    userDto,
                    oldAccessToken,
                    oldRefreshToken
            );

            MoplUserDetails userDetails = new MoplUserDetails(userDto, "encodedPassword");

            when(jwtTokenProvider.validateRefreshToken(oldRefreshToken)).thenReturn(true);
            when(jwtRegistry.hasActiveJwtInformationByRefreshToken(oldRefreshToken)).thenReturn(true);
            when(jwtRegistry.getJwtInformationByRefreshToken(oldRefreshToken)).thenReturn(oldJwtInfo);
            when(jwtTokenProvider.getUserEmailFromToken(oldRefreshToken)).thenReturn("admin@admin.com");
            when(userDetailsService.loadUserByUsername("admin@admin.com")).thenReturn(userDetails);
            when(jwtTokenProvider.generateAccessToken(userDetails))
                    .thenThrow(new RuntimeException("access token fail"));

            assertThatThrownBy(() -> authService.refresh(oldRefreshToken, response))
                    .isInstanceOf(AuthFailedRefrshToken.class);

            verify(jwtRegistry, never()).rotateJwtInformation(any(), any());
            verify(jwtRegistry).rollbackRotateJwtInformation(
                    oldRefreshToken,
                    oldJwtInfo,
                    null
            );
        }

        @Test
        @DisplayName("쿠키 추가 실패 시 rollback 후 AuthFailedRefrshToken")
        void refresh_fail_addRefreshCookieException() throws JOSEException {
            String oldRefreshToken = "old.refresh.token";
            String oldAccessToken = "old.access.token";
            String newAccessToken = "new.access.token";
            String newRefreshToken = "new.refresh.token";

            UserDto userDto = new UserDto(
                    UUID.randomUUID(),
                    null,
                    "admin@admin.com",
                    "관리자",
                    null,
                    Role.ADMIN,
                    false
            );

            JwtInformation oldJwtInfo = new JwtInformation(
                    userDto,
                    oldAccessToken,
                    oldRefreshToken
            );

            MoplUserDetails userDetails = new MoplUserDetails(userDto, "encodedPassword");

            when(jwtTokenProvider.validateRefreshToken(oldRefreshToken)).thenReturn(true);
            when(jwtRegistry.hasActiveJwtInformationByRefreshToken(oldRefreshToken)).thenReturn(true);
            when(jwtRegistry.getJwtInformationByRefreshToken(oldRefreshToken)).thenReturn(oldJwtInfo);
            when(jwtTokenProvider.getUserEmailFromToken(oldRefreshToken)).thenReturn("admin@admin.com");
            when(userDetailsService.loadUserByUsername("admin@admin.com")).thenReturn(userDetails);
            when(jwtTokenProvider.generateAccessToken(userDetails)).thenReturn(newAccessToken);
            when(jwtTokenProvider.generateRefreshToken(userDetails)).thenReturn(newRefreshToken);

            doThrow(new RuntimeException("cookie fail"))
                    .when(jwtTokenProvider)
                    .addRefreshCookie(response, newRefreshToken);

            assertThatThrownBy(() -> authService.refresh(oldRefreshToken, response))
                    .isInstanceOf(AuthFailedRefrshToken.class);

            verify(jwtRegistry).rotateJwtInformation(any(), any());
            verify(jwtRegistry).rollbackRotateJwtInformation(
                    oldRefreshToken,
                    oldJwtInfo,
                    newRefreshToken
            );
        }
    }

    @Nested
    @DisplayName("비밀번호 초기화")
    class InitUserPassword {

        @Test
        @DisplayName("비밀번호 초기화 성공")
        void initUserPassword_success() {
            String email = "user@test.com";
            String originPassword = "originEncodedPassword";
            String encodedTempPassword = "encodedTempPassword";

            User user = mock(User.class);

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(user.getPassword()).thenReturn(originPassword);
            when(user.getEmail()).thenReturn(email);
            when(user.getName()).thenReturn("테스트유저");
            when(user.getId()).thenReturn(UUID.randomUUID());
            when(passwordEncoder.encode(anyString())).thenReturn(encodedTempPassword);

            authService.initUserPassword(email);

            ArgumentCaptor<String> rawPasswordCaptor =
                    ArgumentCaptor.forClass(String.class);

            verify(passwordEncoder).encode(rawPasswordCaptor.capture());

            String rawPassword = rawPasswordCaptor.getValue();

            assertThat(rawPassword).hasSize(8);

            verify(user).updateTemporaryPassword(
                    eq(encodedTempPassword),
                    eq(originPassword),
                    any(Instant.class)
            );

            verify(eventPublisher).publishEvent(
                    isA(UserPasswordInitEvent.class)
            );
        }

        @Test
        @DisplayName("사용자가 없으면 UserNotFoundException")
        void initUserPassword_userNotFound() {
            String email = "missing@test.com";

            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.initUserPassword(email))
                    .isInstanceOf(UserNotFoundException.class);

            verifyNoInteractions(passwordEncoder, eventPublisher);
        }
    }
}