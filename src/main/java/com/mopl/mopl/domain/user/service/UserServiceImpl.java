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
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.exception.UserDuplicateException;
import com.mopl.mopl.domain.user.exception.UserNotFoundException;
import com.mopl.mopl.domain.user.mapper.UserMapper;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.event.user.UserEvent;
import com.mopl.mopl.global.event.user.UserUpdateLockEvent;
import com.mopl.mopl.global.event.user.UserUpdateRoleEvent;
import com.mopl.mopl.infrastructure.s3.S3ImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtRegistry jwtRegistry;
    private final S3ImageStorage s3ImageStorage;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public UserDto createUser(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserDuplicateException();
        }

        String password = passwordEncoder.encode(request.password());
        User user = new User(request.name(), request.email(), password, null, null);

        UserDto userDto = userMapper.toDto(userRepository.save(user));
        eventPublisher.publishEvent(UserEvent.of(user));
        return userDto;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public CursorResponseUserDto getAllUsers(CursorUserRequest request) {
        Instant cursorTime = null;
        if (request.cursor() != null && !request.cursor().isBlank()) {
            cursorTime = Instant.parse(request.cursor());
        }

        PageRequest pageRequest = PageRequest.of(0, request.limit() + 1);

        String emailLike = request.emailLike();
        Role roleEqual = request.roleEqual();

        List<User> users = userRepository.findUsersByCursor(
                emailLike,
                roleEqual,
                cursorTime,
                request.idAfter(),
                request.sortDirection(),
                pageRequest
        );

        boolean hasNext = users.size() > request.limit();
        if (hasNext) {
            users.remove(request.limit().intValue());
        }

        String nextCursor = null;
        UUID nextIdAfter = null;
        if (hasNext && !users.isEmpty()) {
            User lastUser = users.get(users.size() - 1);
            nextCursor = lastUser.getCreatedAt().toString();
            nextIdAfter = lastUser.getId();
        }

        long userCount = cursorTime == null
                ? userRepository.countUsersByEmailAndRole(emailLike, roleEqual)
                : -1L;

        List<UserDto> data = users.stream()
                .map(userMapper::toDto)
                .toList();

        return new CursorResponseUserDto(data, nextCursor, nextIdAfter, hasNext, userCount, request.sortBy(), request.sortDirection());
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        UserDto result = userMapper.toDto(user);
        return result;
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto updateUserRole(UUID userId, UserRoleUpdateRequest request) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        target.updateRole(request.role());
        eventPublisher.publishEvent(UserUpdateRoleEvent.of(target));
        return userMapper.toDto(target);
    }

    @Override
    @Transactional
    @PreAuthorize("principal.userDto.id == #userId")
    public UserDto updateUserPassword(UUID userId, ChangePasswordRequest request) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        String encodedPassword = passwordEncoder.encode(request.password());
        target.updatePassword(encodedPassword);
        jwtRegistry.invalidateJwtInformationByUserId(userId);
        return userMapper.toDto(target);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto updateUserLocked(UUID userId, UserLockUpdateRequest request) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (request.locked()) {
            target.lock();
            // 계정 정지하며 토큰 삭제
            jwtRegistry.invalidateJwtInformationByUserId(userId);
        } else {
            target.unlock();
        }

        eventPublisher.publishEvent(UserUpdateLockEvent.of(target));
        return userMapper.toDto(target);
    }

    @Override
    @Transactional
    @PreAuthorize("principal.userDto.id == #userId")
    public UserDto updateProfile(
            UUID userId, UserUpdateRequest request,
            MultipartFile profile
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        String key = null;

        if (profile != null && !profile.isEmpty()) {
            key = s3ImageStorage.upload(profile, "profile");

            String uploadedKey = key;

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            if (status == STATUS_ROLLED_BACK) {
                                s3ImageStorage.delete(uploadedKey);
                            }
                        }
                    }
            );
            user.updateProfileImage(key);
        }

        if (request.name() != null && !request.name().isBlank()) {
            user.updateName(request.name());
        }

        eventPublisher.publishEvent(UserEvent.of(user));
        return userMapper.toDto(user);
    }
}