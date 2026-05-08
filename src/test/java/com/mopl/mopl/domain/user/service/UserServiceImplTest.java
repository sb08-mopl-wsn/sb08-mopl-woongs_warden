package com.mopl.mopl.domain.user.service;

import com.mopl.mopl.domain.jwt.registry.JwtRegistry;
import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.dto.request.ChangePasswordRequest;
import com.mopl.mopl.domain.user.dto.request.UserCreateRequest;
import com.mopl.mopl.domain.user.dto.request.UserLockUpdateRequest;
import com.mopl.mopl.domain.user.dto.request.UserRoleUpdateRequest;
import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.exception.UserDuplicateException;
import com.mopl.mopl.domain.user.exception.UserNotFoundException;
import com.mopl.mopl.domain.user.mapper.UserMapper;
import com.mopl.mopl.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtRegistry jwtRegistry;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
                userRepository,
                userMapper,
                passwordEncoder,
                jwtRegistry
        );

        ReflectionTestUtils.setField(userService, "upper", "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        ReflectionTestUtils.setField(userService, "lower", "abcdefghijklmnopqrstuvwxyz");
        ReflectionTestUtils.setField(userService, "digit", "0123456789");
        ReflectionTestUtils.setField(userService, "special", "!@#$%^&*");
    }

    @Test
    @DisplayName("회원 생성에 성공한다")
    void createUser_success() {
        // given
        UserCreateRequest request = new UserCreateRequest(
                "user@test.com",
                "사용자",
                "Password1!"
        );

        UUID userId = UUID.randomUUID();

        UserDto expected = new UserDto(
                userId,
                Instant.now(),
                request.email(),
                request.name(),
                null,
                Role.USER,
                false
        );

        given(userRepository.existsByEmail(request.email()))
                .willReturn(false);

        given(passwordEncoder.encode(request.password()))
                .willReturn("encoded-password");

        given(userRepository.save(any(User.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        given(userMapper.toDto(any(User.class)))
                .willReturn(expected);

        // when
        UserDto result = userService.createUser(request);

        // then
        assertThat(result).isEqualTo(expected);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getEmail()).isEqualTo(request.email());
        assertThat(savedUser.getName()).isEqualTo(request.name());
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getRole()).isEqualTo(Role.USER);
        assertThat(savedUser.isLocked()).isFalse();

        verify(userRepository).existsByEmail(request.email());
        verify(passwordEncoder).encode(request.password());
        verify(userMapper).toDto(savedUser);
    }

    @Test
    @DisplayName("회원 생성 시 이메일이 중복되면 UserDuplicateException이 발생한다")
    void createUser_duplicateEmail_throwUserDuplicateException() {
        // given
        UserCreateRequest request = new UserCreateRequest(
                "duplicate@test.com",
                "중복사용자",
                "Password1!"
        );

        given(userRepository.existsByEmail(request.email()))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(UserDuplicateException.class);

        verify(userRepository).existsByEmail(request.email());
        verifyNoInteractions(passwordEncoder, userMapper);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("회원 단건 조회에 성공한다")
    void getUser_success() {
        UUID userId = UUID.randomUUID();
        User user = createUser();
        UserDto expected = createUserDto(userId, user);

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        given(userMapper.toDto(user))
                .willReturn(expected);

        UserDto result = userService.getUser(userId);

        assertThat(result).isEqualTo(expected);

        verify(userRepository).findById(userId);
        verify(userMapper).toDto(user);
    }

    @Test
    @DisplayName("회원 단건 조회 시 사용자가 없으면 UserNotFoundException이 발생한다")
    void getUser_notFound_throwUserNotFoundException() {
        UUID userId = UUID.randomUUID();

        given(userRepository.findById(userId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(userId))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findById(userId);
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("회원 권한 수정에 성공한다")
    void updateUserRole_success() {
        UUID userId = UUID.randomUUID();
        User user = createUser();

        UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.ADMIN);

        UserDto expected = new UserDto(
                userId,
                Instant.now(),
                user.getEmail(),
                user.getName(),
                null,
                Role.ADMIN,
                false
        );

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        given(userMapper.toDto(user))
                .willReturn(expected);

        UserDto result = userService.updateUserRole(userId, request);

        assertThat(result).isEqualTo(expected);
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);

        verify(userRepository).findById(userId);
        verify(userMapper).toDto(user);
    }

    @Test
    @DisplayName("회원 권한 수정 시 사용자가 없으면 UserNotFoundException이 발생한다")
    void updateUserRole_notFound_throwUserNotFoundException() {
        UUID userId = UUID.randomUUID();
        UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.ADMIN);

        given(userRepository.findById(userId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserRole(userId, request))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findById(userId);
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("회원 비밀번호 수정에 성공한다")
    void updateUserPassword_success() {
        UUID userId = UUID.randomUUID();
        User user = createUser();

        ChangePasswordRequest request = new ChangePasswordRequest("NewPassword1!");
        UserDto expected = createUserDto(userId, user);

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        given(passwordEncoder.encode(request.password()))
                .willReturn("new-encoded-password");

        given(userMapper.toDto(user))
                .willReturn(expected);

        UserDto result = userService.updateUserPassword(userId, request);

        assertThat(result).isEqualTo(expected);
        assertThat(user.getPassword()).isEqualTo("new-encoded-password");

        verify(userRepository).findById(userId);
        verify(passwordEncoder).encode(request.password());
        verify(jwtRegistry).invalidateJwtInformationByUserId(userId);
        verify(userMapper).toDto(user);
    }

    @Test
    @DisplayName("회원 비밀번호 수정 시 사용자가 없으면 UserNotFoundException이 발생한다")
    void updateUserPassword_notFound_throwUserNotFoundException() {
        UUID userId = UUID.randomUUID();
        ChangePasswordRequest request = new ChangePasswordRequest("NewPassword1!");

        given(userRepository.findById(userId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserPassword(userId, request))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findById(userId);
        verifyNoInteractions(passwordEncoder, jwtRegistry, userMapper);
    }

    @Test
    @DisplayName("관리자에 의한 비밀번호 초기화에 성공한다")
    void initUserPassword_success() {
        UUID userId = UUID.randomUUID();
        User user = createUser();
        UserDto expected = createUserDto(userId, user);

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        given(passwordEncoder.encode(any(String.class)))
                .willReturn("init-encoded-password");

        given(userMapper.toDto(user))
                .willReturn(expected);

        UserDto result = userService.initUserPassword(userId);

        assertThat(result).isEqualTo(expected);
        assertThat(user.getPassword()).isEqualTo("init-encoded-password");

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).encode(passwordCaptor.capture());

        String rawPassword = passwordCaptor.getValue();

        assertThat(rawPassword).hasSize(8);
        assertThat(rawPassword).matches(".*[A-Z].*");
        assertThat(rawPassword).matches(".*[a-z].*");
        assertThat(rawPassword).matches(".*[0-9].*");
        assertThat(rawPassword).matches(".*[!@#$%^&*].*");

        verify(userRepository).findById(userId);
        verify(jwtRegistry).invalidateJwtInformationByUserId(userId);
        verify(userMapper).toDto(user);
    }

    @Test
    @DisplayName("비밀번호 초기화 시 사용자가 없으면 UserNotFoundException이 발생한다")
    void initUserPassword_notFound_throwUserNotFoundException() {
        UUID userId = UUID.randomUUID();

        given(userRepository.findById(userId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.initUserPassword(userId))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findById(userId);
        verifyNoInteractions(passwordEncoder, jwtRegistry, userMapper);
    }

    @Test
    @DisplayName("회원을 잠금 처리하면 계정이 잠기고 JWT 정보가 무효화된다")
    void updateUserLocked_lock_success() {
        UUID userId = UUID.randomUUID();
        User user = createUser();

        UserLockUpdateRequest request = new UserLockUpdateRequest(true);

        UserDto expected = new UserDto(
                userId,
                Instant.now(),
                user.getEmail(),
                user.getName(),
                null,
                user.getRole(),
                true
        );

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        given(userMapper.toDto(user))
                .willReturn(expected);

        UserDto result = userService.updateUserLocked(userId, request);

        assertThat(result).isEqualTo(expected);
        assertThat(user.isLocked()).isTrue();

        verify(userRepository).findById(userId);
        verify(jwtRegistry).invalidateJwtInformationByUserId(userId);
        verify(userMapper).toDto(user);
    }

    @Test
    @DisplayName("회원 잠금을 해제하면 계정이 잠금 해제되고 JWT 정보는 무효화하지 않는다")
    void updateUserLocked_unlock_success() {
        UUID userId = UUID.randomUUID();
        User user = createUser();
        user.lock();

        UserLockUpdateRequest request = new UserLockUpdateRequest(false);

        UserDto expected = new UserDto(
                userId,
                Instant.now(),
                user.getEmail(),
                user.getName(),
                null,
                user.getRole(),
                false
        );

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        given(userMapper.toDto(user))
                .willReturn(expected);

        UserDto result = userService.updateUserLocked(userId, request);

        assertThat(result).isEqualTo(expected);
        assertThat(user.isLocked()).isFalse();

        verify(userRepository).findById(userId);
        verify(jwtRegistry, never()).invalidateJwtInformationByUserId(any(UUID.class));
        verify(userMapper).toDto(user);
    }

    @Test
    @DisplayName("회원 잠금 상태 수정 시 사용자가 없으면 UserNotFoundException이 발생한다")
    void updateUserLocked_notFound_throwUserNotFoundException() {
        UUID userId = UUID.randomUUID();
        UserLockUpdateRequest request = new UserLockUpdateRequest(true);

        given(userRepository.findById(userId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserLocked(userId, request))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findById(userId);
        verifyNoInteractions(jwtRegistry, userMapper);
    }

    private User createUser() {
        return new User(
                "사용자",
                "user@test.com",
                "encoded-password",
                null,
                null
        );
    }

    private UserDto createUserDto(UUID userId, User user) {
        return new UserDto(
                userId,
                Instant.now(),
                user.getEmail(),
                user.getName(),
                null,
                user.getRole(),
                user.isLocked()
        );
    }
}