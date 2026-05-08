package com.mopl.mopl.domain.auth;

import com.mopl.mopl.domain.auth.service.AuthServiceImpl;
import com.mopl.mopl.domain.jwt.registry.JwtRegistry;
import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.dto.request.UserRoleUpdateRequest;
import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.exception.UserNotFoundException;
import com.mopl.mopl.domain.user.mapper.UserMapper;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtRegistry jwtRegistry;

    @InjectMocks
    private AuthServiceImpl authService;

    @Nested
    @DisplayName("현재 사용자 조회")
    class GetCurrentUserInfo {

        @Test
        @DisplayName("현재 사용자 조회 성공")
        void getCurrentUserInfo_success() {
            // given
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

            when(userRepository.findByEmail("test@test.com"))
                    .thenReturn(Optional.of(user));
            when(userMapper.toDto(user))
                    .thenReturn(userDto);

            // when
            UserDto result = authService.getCurrentUserInfo(userDetails);

            // then
            assertThat(result).isEqualTo(userDto);
            verify(userRepository).findByEmail("test@test.com");
            verify(userMapper).toDto(user);
        }

        @Test
        @DisplayName("UserDetails가 null이면 null 반환")
        void getCurrentUserInfo_nullUserDetails() {
            // when
            UserDto result = authService.getCurrentUserInfo(null);

            // then
            assertThat(result).isNull();
            verifyNoInteractions(userRepository, userMapper);
        }

        @Test
        @DisplayName("사용자를 찾을 수 없으면 예외")
        void getCurrentUserInfo_userNotFound() {
            // given
            UUID userId = UUID.randomUUID();

            UserDto userDto = new UserDto(
                    userId,
                    null,
                    "missing@test.com",
                    "missing",
                    null,
                    Role.USER,
                    false
            );

            MoplUserDetails userDetails = new MoplUserDetails(userDto, "encodedPassword");

            when(userRepository.findByEmail("missing@test.com"))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.getCurrentUserInfo(userDetails))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("권한 변경")
    class UpdateRole {

        @Test
        @DisplayName("권한 변경 성공")
        void updateRole_success() {
            // given
            UUID userId = UUID.randomUUID();

            User user = mock(User.class);
            User savedUser = mock(User.class);

            UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.ADMIN);

            UserDto resultDto = new UserDto(
                    userId,
                    null,
                    "admin@test.com",
                    "admin",
                    null,
                    Role.ADMIN,
                    false
            );

            when(userRepository.findById(userId))
                    .thenReturn(Optional.of(user));
            when(userRepository.save(user))
                    .thenReturn(savedUser);
            when(savedUser.getId())
                    .thenReturn(userId);
            when(userMapper.toDto(savedUser))
                    .thenReturn(resultDto);

            // when
            UserDto result = authService.updateRole(userId, request);

            // then
            verify(user).updateRole(Role.ADMIN);
            verify(userRepository).save(user);
            verify(jwtRegistry).invalidateJwtInformationByUserId(userId);
            assertThat(result).isEqualTo(resultDto);
        }

        @Test
        @DisplayName("유저가 없으면 예외")
        void updateRole_userNotFound() {
            // given
            UUID userId = UUID.randomUUID();
            UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.ADMIN);

            when(userRepository.findById(userId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.updateRole(userId, request))
                    .isInstanceOf(UserNotFoundException.class);

            verify(userRepository, never()).save(any());
            verifyNoInteractions(jwtRegistry);
        }

        @Test
        @DisplayName("JWT 무효화 실패해도 권한 변경 결과 반환")
        void updateRole_jwtInvalidateFail() {
            // given
            UUID userId = UUID.randomUUID();

            User user = mock(User.class);
            User savedUser = mock(User.class);

            UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.ADMIN);

            UserDto resultDto = new UserDto(
                    userId,
                    null,
                    "admin@test.com",
                    "admin",
                    null,
                    Role.ADMIN,
                    false
            );

            when(userRepository.findById(userId))
                    .thenReturn(Optional.of(user));
            when(userRepository.save(user))
                    .thenReturn(savedUser);
            when(savedUser.getId())
                    .thenReturn(userId);
            when(userMapper.toDto(savedUser))
                    .thenReturn(resultDto);

            doThrow(new RuntimeException("jwt invalidate fail"))
                    .when(jwtRegistry)
                    .invalidateJwtInformationByUserId(userId);

            // when
            UserDto result = authService.updateRole(userId, request);

            // then
            verify(user).updateRole(Role.ADMIN);
            verify(jwtRegistry).invalidateJwtInformationByUserId(userId);
            assertThat(result).isEqualTo(resultDto);
        }
    }
}