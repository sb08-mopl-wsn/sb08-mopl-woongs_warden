package com.mopl.mopl.domain.user.service;

import com.mopl.mopl.domain.jwt.registry.JwtRegistry;
import com.mopl.mopl.domain.user.dto.CursorResponseUserDto;
import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.dto.request.ChangePasswordRequest;
import com.mopl.mopl.domain.user.dto.request.CursorUserRequest;
import com.mopl.mopl.domain.user.dto.request.UserCreateRequest;
import com.mopl.mopl.domain.user.dto.request.UserLockUpdateRequest;
import com.mopl.mopl.domain.user.dto.request.UserRoleUpdateRequest;
import com.mopl.mopl.domain.user.dto.request.UserUpdateRequest;
import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.domain.user.entity.SortBy;
import com.mopl.mopl.domain.user.entity.SortDirection;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.exception.UserDuplicateException;
import com.mopl.mopl.domain.user.exception.UserNotFoundException;
import com.mopl.mopl.domain.user.mapper.UserMapper;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.event.user.UserEvent;
import com.mopl.mopl.global.event.user.UserUpdateLockEvent;
import com.mopl.mopl.global.event.user.UserUpdateRoleEvent;
import com.mopl.mopl.infrastructure.s3.S3ImageStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK;

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

    @Mock
    private S3ImageStorage s3ImageStorage;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private UserServiceImpl userService;

    @Mock
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
                userRepository,
                userMapper,
                passwordEncoder,
                jwtRegistry,
                s3ImageStorage,
                eventPublisher,
                cacheManager
        );
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("첫 페이지 사용자 목록 조회 시 DESC 정렬 방향으로 조회하고 countUsersByEmailAndRole을 호출한다")
    void getAllUsers_firstPage_desc_success() {
        CursorUserRequest request = new CursorUserRequest(
                null,
                null,
                null,
                null,
                20,
                SortDirection.DESCENDING,
                SortBy.createdAt
        );

        User user = createUser();
        UserDto userDto = createUserDto(UUID.randomUUID(), user);

        given(userRepository.findUsersByCursor(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(SortDirection.DESCENDING),
                any(Pageable.class)
        )).willReturn(List.of(user));

        given(userRepository.countUsersByEmailAndRole(
                nullable(String.class),
                nullable(Role.class)
        )).willReturn(1L);

        given(userMapper.toDto(user)).willReturn(userDto);

        CursorResponseUserDto result = userService.getAllUsers(request);

        assertThat(result.data()).containsExactly(userDto);
        assertThat(result.nextCursor()).isNull();
        assertThat(result.nextIdAfter()).isNull();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.totalCount()).isEqualTo(1L);
        assertThat(result.sortBy()).isEqualTo(SortBy.createdAt);
        assertThat(result.sortDirection()).isEqualTo(SortDirection.DESCENDING);
    }

    @Test
    @DisplayName("첫 페이지 사용자 목록 조회 시 ASC 정렬 방향으로 조회하고 countUsersByEmailAndRole을 호출한다")
    void getAllUsers_firstPage_asc_success() {
        CursorUserRequest request = new CursorUserRequest(
                "user",
                Role.USER,
                null,
                null,
                20,
                SortDirection.ASCENDING,
                SortBy.email
        );

        User user = createUser();
        UserDto userDto = createUserDto(UUID.randomUUID(), user);

        given(userRepository.findUsersByCursor(
                eq("user"),
                eq(Role.USER),
                isNull(),
                isNull(),
                eq(SortDirection.ASCENDING),
                any(Pageable.class)
        )).willReturn(List.of(user));

        given(userRepository.countUsersByEmailAndRole("user", Role.USER))
                .willReturn(1L);

        given(userMapper.toDto(user)).willReturn(userDto);

        CursorResponseUserDto result = userService.getAllUsers(request);

        assertThat(result.data()).containsExactly(userDto);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.totalCount()).isEqualTo(1L);
        assertThat(result.sortBy()).isEqualTo(SortBy.email);
        assertThat(result.sortDirection()).isEqualTo(SortDirection.ASCENDING);
    }

    @Test
    @DisplayName("다음 페이지 사용자 목록 조회 시 countUsersByEmailAndRole을 호출하지 않고 totalCount는 -1L을 반환한다")
    void getAllUsers_nextPage_skipCount_success() {
        UUID idAfter = UUID.randomUUID();
        String cursor = "2026-05-11T04:36:08Z";
        Instant cursorTime = Instant.parse(cursor);

        CursorUserRequest request = new CursorUserRequest(
                null,
                null,
                cursor,
                idAfter,
                20,
                SortDirection.DESCENDING,
                SortBy.createdAt
        );

        User user = createUser();
        UserDto userDto = createUserDto(UUID.randomUUID(), user);

        given(userRepository.findUsersByCursor(
                isNull(),
                isNull(),
                eq(cursorTime),
                eq(idAfter),
                eq(SortDirection.DESCENDING),
                any(Pageable.class)
        )).willReturn(List.of(user));

        given(userMapper.toDto(user)).willReturn(userDto);

        CursorResponseUserDto result = userService.getAllUsers(request);

        assertThat(result.data()).containsExactly(userDto);
        assertThat(result.totalCount()).isEqualTo(-1L);

        verify(userRepository, never()).countUsersByEmailAndRole(any(), any());
    }

    @Test
    @DisplayName("조회 결과가 limit보다 많으면 hasNext가 true이고 마지막 요소를 제거한다")
    void getAllUsers_hasNext_success() {
        CursorUserRequest request = new CursorUserRequest(
                null,
                null,
                null,
                null,
                1,
                SortDirection.DESCENDING,
                SortBy.createdAt
        );

        User firstUser = createUser();
        UUID firstUserId = UUID.randomUUID();
        Instant firstCreatedAt = Instant.parse("2026-05-11T04:36:08Z");
        ReflectionTestUtils.setField(firstUser, "id", firstUserId);
        ReflectionTestUtils.setField(firstUser, "createdAt", firstCreatedAt);

        User extraUser = createUser();
        ReflectionTestUtils.setField(extraUser, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(extraUser, "createdAt", Instant.parse("2026-05-10T04:36:08Z"));

        UserDto firstUserDto = createUserDto(firstUserId, firstUser);

        given(userRepository.findUsersByCursor(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(SortDirection.DESCENDING),
                any(Pageable.class)
        )).willReturn(new ArrayList<>(List.of(firstUser, extraUser)));

        given(userRepository.countUsersByEmailAndRole(nullable(String.class), nullable(Role.class)))
                .willReturn(2L);

        given(userMapper.toDto(firstUser)).willReturn(firstUserDto);

        CursorResponseUserDto result = userService.getAllUsers(request);

        assertThat(result.data()).containsExactly(firstUserDto);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(firstCreatedAt.toString());
        assertThat(result.nextIdAfter()).isEqualTo(firstUserId);
        assertThat(result.totalCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("회원 생성에 성공한다")
    void createUser_success() {
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

        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn("encoded-password");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedUser, "id", userId);
            return savedUser;
        });
        given(userMapper.toDto(any(User.class))).willReturn(expected);

        UserDto result = userService.createUser(request);

        assertThat(result).isEqualTo(expected);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getEmail()).isEqualTo(request.email());
        assertThat(savedUser.getName()).isEqualTo(request.name());
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getRole()).isEqualTo(Role.USER);
        assertThat(savedUser.isLocked()).isFalse();

        verify(eventPublisher).publishEvent(isA(UserEvent.class));
    }

    @Test
    @DisplayName("회원 생성 시 이메일이 중복되면 UserDuplicateException이 발생한다")
    void createUser_duplicateEmail_throwUserDuplicateException() {
        UserCreateRequest request = new UserCreateRequest(
                "duplicate@test.com",
                "중복사용자",
                "Password1!"
        );

        given(userRepository.existsByEmail(request.email())).willReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(UserDuplicateException.class);

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder, userMapper, eventPublisher);
    }

    @Test
    @DisplayName("회원 단건 조회에 성공한다")
    void getUser_success() {
        UUID userId = UUID.randomUUID();
        User user = createUser();
        UserDto expected = createUserDto(userId, user);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userMapper.toDto(user)).willReturn(expected);

        UserDto result = userService.getUser(userId);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("회원 단건 조회 시 사용자가 없으면 UserNotFoundException이 발생한다")
    void getUser_notFound_throwUserNotFoundException() {
        UUID userId = UUID.randomUUID();

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(userId))
                .isInstanceOf(UserNotFoundException.class);

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

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userMapper.toDto(user)).willReturn(expected);

        UserDto result = userService.updateUserRole(userId, request);

        assertThat(result).isEqualTo(expected);
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);

        verify(eventPublisher).publishEvent(isA(UserUpdateRoleEvent.class));
    }

    @Test
    @DisplayName("회원 권한 수정 시 사용자가 없으면 UserNotFoundException이 발생한다")
    void updateUserRole_notFound_throwUserNotFoundException() {
        UUID userId = UUID.randomUUID();
        UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.ADMIN);

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserRole(userId, request))
                .isInstanceOf(UserNotFoundException.class);

        verifyNoInteractions(userMapper, eventPublisher);
    }

    @Test
    @DisplayName("회원 비밀번호 수정에 성공하고 JWT 정보가 무효화된다")
    void updateUserPassword_success() {
        UUID userId = UUID.randomUUID();
        User user = createUser();
        user.updateTemporaryPassword("temp-password-hash", "origin-password-hash", Instant.now().plusSeconds(300));

        ChangePasswordRequest request = new ChangePasswordRequest("NewPassword1!");
        UserDto expected = createUserDto(userId, user);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(passwordEncoder.encode(request.password())).willReturn("new-encoded-password");
        given(userMapper.toDto(user)).willReturn(expected);

        UserDto result = userService.updateUserPassword(userId, request);

        assertThat(result).isEqualTo(expected);
        assertThat(user.getPassword()).isEqualTo("new-encoded-password");
        assertThat(user.getTemporaryPassword()).isNull();
        assertThat(user.getTemporaryPasswordExpiredAt()).isNull();

        verify(jwtRegistry).invalidateJwtInformationByUserId(userId);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("회원 비밀번호 수정 시 사용자가 없으면 UserNotFoundException이 발생한다")
    void updateUserPassword_notFound_throwUserNotFoundException() {
        UUID userId = UUID.randomUUID();
        ChangePasswordRequest request = new ChangePasswordRequest("NewPassword1!");

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserPassword(userId, request))
                .isInstanceOf(UserNotFoundException.class);

        verifyNoInteractions(passwordEncoder, jwtRegistry, userMapper, eventPublisher);
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

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userMapper.toDto(user)).willReturn(expected);

        UserDto result = userService.updateUserLocked(userId, request);

        assertThat(result).isEqualTo(expected);
        assertThat(user.isLocked()).isTrue();

        verify(jwtRegistry).invalidateJwtInformationByUserId(userId);
        verify(eventPublisher).publishEvent(isA(UserUpdateLockEvent.class));
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

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userMapper.toDto(user)).willReturn(expected);

        UserDto result = userService.updateUserLocked(userId, request);

        assertThat(result).isEqualTo(expected);
        assertThat(user.isLocked()).isFalse();

        verify(jwtRegistry, never()).invalidateJwtInformationByUserId(any(UUID.class));
        verify(eventPublisher).publishEvent(isA(UserUpdateLockEvent.class));
    }

    @Test
    @DisplayName("회원 잠금 상태 수정 시 사용자가 없으면 UserNotFoundException이 발생한다")
    void updateUserLocked_notFound_throwUserNotFoundException() {
        UUID userId = UUID.randomUUID();
        UserLockUpdateRequest request = new UserLockUpdateRequest(true);

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserLocked(userId, request))
                .isInstanceOf(UserNotFoundException.class);

        verifyNoInteractions(jwtRegistry, userMapper, eventPublisher);
    }

    @Test
    @DisplayName("프로필 수정 시 이름과 프로필 이미지를 함께 수정한다")
    void updateProfile_nameAndProfile_success() {
        initTransactionSynchronization();

        UUID userId = UUID.randomUUID();
        User user = createUser();
        UserUpdateRequest request = new UserUpdateRequest("변경된이름");

        MockMultipartFile profile = new MockMultipartFile(
                "profile",
                "profile.png",
                "image/png",
                "image-content".getBytes()
        );

        String uploadedKey = "profile/test-profile.png";

        UserDto expected = new UserDto(
                userId,
                Instant.now(),
                user.getEmail(),
                "변경된이름",
                "https://cdn.example.com/profile/test-profile.png",
                user.getRole(),
                false
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(s3ImageStorage.upload(profile, "profile")).willReturn(uploadedKey);
        given(userMapper.toDto(user)).willReturn(expected);

        UserDto result = userService.updateProfile(userId, request, profile);

        assertThat(result).isEqualTo(expected);
        assertThat(user.getName()).isEqualTo("변경된이름");
        assertThat(user.getProfileImageKey()).isEqualTo(uploadedKey);
        assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);

        verify(s3ImageStorage).upload(profile, "profile");
        verify(userMapper).toDto(user);
        verify(eventPublisher).publishEvent(isA(UserEvent.class));
    }

    @Test
    @DisplayName("프로필 수정 중 트랜잭션이 롤백되면 업로드된 S3 이미지를 삭제한다")
    void updateProfile_rollback_deleteUploadedS3Object_success() {
        initTransactionSynchronization();

        UUID userId = UUID.randomUUID();
        User user = createUser();
        UserUpdateRequest request = new UserUpdateRequest("변경된이름");

        MockMultipartFile profile = new MockMultipartFile(
                "profile",
                "profile.png",
                "image/png",
                "image-content".getBytes()
        );

        String uploadedKey = "profile/test-profile.png";

        UserDto expected = new UserDto(
                userId,
                Instant.now(),
                user.getEmail(),
                "변경된이름",
                "https://cdn.example.com/profile/test-profile.png",
                user.getRole(),
                false
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(s3ImageStorage.upload(profile, "profile")).willReturn(uploadedKey);
        given(userMapper.toDto(user)).willReturn(expected);

        userService.updateProfile(userId, request, profile);

        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();

        assertThat(synchronizations).hasSize(1);

        synchronizations.forEach(
                synchronization -> synchronization.afterCompletion(STATUS_ROLLED_BACK)
        );

        verify(s3ImageStorage).upload(profile, "profile");
        verify(s3ImageStorage).delete(uploadedKey);
        verify(eventPublisher).publishEvent(isA(UserEvent.class));
    }

    @Test
    @DisplayName("프로필 수정 시 이름만 전달되면 이름만 수정하고 S3 업로드는 호출하지 않는다")
    void updateProfile_onlyName_success() {
        UUID userId = UUID.randomUUID();
        User user = createUser();
        UserUpdateRequest request = new UserUpdateRequest("이름만수정");

        UserDto expected = new UserDto(
                userId,
                Instant.now(),
                user.getEmail(),
                "이름만수정",
                null,
                user.getRole(),
                false
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userMapper.toDto(user)).willReturn(expected);

        UserDto result = userService.updateProfile(userId, request, null);

        assertThat(result).isEqualTo(expected);
        assertThat(user.getName()).isEqualTo("이름만수정");
        assertThat(user.getProfileImageKey()).isNull();

        verifyNoInteractions(s3ImageStorage);
        verify(userMapper).toDto(user);
        verify(eventPublisher).publishEvent(isA(UserEvent.class));
    }

    @Test
    @DisplayName("프로필 수정 시 이미지만 전달되면 이미지만 수정하고 이름은 유지한다")
    void updateProfile_onlyProfile_success() {
        initTransactionSynchronization();

        UUID userId = UUID.randomUUID();
        User user = createUser();
        UserUpdateRequest request = new UserUpdateRequest("");

        MockMultipartFile profile = new MockMultipartFile(
                "profile",
                "profile.jpg",
                "image/jpeg",
                "image-content".getBytes()
        );

        String uploadedKey = "profile/test-profile.jpg";

        UserDto expected = new UserDto(
                userId,
                Instant.now(),
                user.getEmail(),
                user.getName(),
                "https://cdn.example.com/profile/test-profile.jpg",
                user.getRole(),
                false
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(s3ImageStorage.upload(profile, "profile")).willReturn(uploadedKey);
        given(userMapper.toDto(user)).willReturn(expected);

        UserDto result = userService.updateProfile(userId, request, profile);

        assertThat(result).isEqualTo(expected);
        assertThat(user.getName()).isEqualTo("사용자");
        assertThat(user.getProfileImageKey()).isEqualTo(uploadedKey);
        assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);

        verify(s3ImageStorage).upload(profile, "profile");
        verify(userMapper).toDto(user);
        verify(eventPublisher).publishEvent(isA(UserEvent.class));
    }

    @Test
    @DisplayName("프로필 수정 시 빈 파일이면 S3 업로드를 호출하지 않는다")
    void updateProfile_emptyProfile_skipS3Upload_success() {
        UUID userId = UUID.randomUUID();
        User user = createUser();
        UserUpdateRequest request = new UserUpdateRequest("변경된이름");

        MockMultipartFile emptyProfile = new MockMultipartFile(
                "profile",
                "empty.png",
                "image/png",
                new byte[0]
        );

        UserDto expected = new UserDto(
                userId,
                Instant.now(),
                user.getEmail(),
                "변경된이름",
                null,
                user.getRole(),
                false
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userMapper.toDto(user)).willReturn(expected);

        UserDto result = userService.updateProfile(userId, request, emptyProfile);

        assertThat(result).isEqualTo(expected);
        assertThat(user.getName()).isEqualTo("변경된이름");
        assertThat(user.getProfileImageKey()).isNull();

        verifyNoInteractions(s3ImageStorage);
        verify(userMapper).toDto(user);
        verify(eventPublisher).publishEvent(isA(UserEvent.class));
    }

    @Test
    @DisplayName("프로필 수정 시 사용자가 없으면 UserNotFoundException이 발생한다")
    void updateProfile_notFound_throwUserNotFoundException() {
        UUID userId = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest("변경된이름");

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfile(userId, request, null))
                .isInstanceOf(UserNotFoundException.class);

        verifyNoInteractions(s3ImageStorage, userMapper, eventPublisher);
    }

    private void initTransactionSynchronization() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.initSynchronization();
        }
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