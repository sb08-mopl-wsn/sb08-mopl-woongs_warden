package com.mopl.mopl.domain.user.service;

import com.mopl.mopl.domain.jwt.registry.JwtRegistry;
import com.mopl.mopl.domain.notification.dto.CursorResponseNotificationDto;
import com.mopl.mopl.domain.notification.dto.NotificationDto;
import com.mopl.mopl.domain.notification.entity.Notification;
import com.mopl.mopl.domain.user.dto.CursorUserListResponse;
import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.dto.request.*;
import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.domain.user.entity.SortDirection;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.exception.UserDuplicateException;
import com.mopl.mopl.domain.user.exception.UserNotFoundException;
import com.mopl.mopl.domain.user.mapper.UserMapper;
import com.mopl.mopl.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
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

    // admin이 유저를 초기화할 경우 쓸 비밀번호
    @Value("${password.policy.upper}")
    private String upper;

    @Value("${password.policy.lower}")
    private String lower;

    @Value("${password.policy.digit}")
    private String digit;

    @Value("${password.policy.special}")
    private String special;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public UserDto createUser(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserDuplicateException();
        }

        String password = passwordEncoder.encode(request.password());
        User user = new User(request.name(), request.email(), password, null, null);

        // todo 나중에 이벤트로 알리기
        userRepository.save(user);
        return userMapper.toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public CursorUserListResponse getAllUsers(CursorUserRequest request) {
        Instant cursorTime = null;
        if (request.cursor() != null && !request.cursor().isBlank()) {
            cursorTime = Instant.parse(request.cursor());
        }

        PageRequest pageRequest = PageRequest.of(0, request.limit() + 1);
        List<User> users;

        String emailLike = request.emailLike();
        Role roleEqual = request.roleEqual();

        if (SortDirection.ASCENDING.equals(request.sortDirection())) {
            users = userRepository.findUsersByCursorAsc(
                    emailLike, roleEqual, cursorTime, request.idAfter(), pageRequest
            );
        } else {
            users = userRepository.findUsersByCursorDesc(
                    emailLike, roleEqual, cursorTime, request.idAfter(), pageRequest
            );
        }

        boolean hasNext = users.size() > request.limit();
        if (hasNext) {
            users.remove(request.limit().intValue());
        }

        String nextCursor = null;
        UUID nextIdAfter = null;
        if (!users.isEmpty()) {
            User lastUser = users.get(users.size() - 1);
            nextCursor = lastUser.getCreatedAt().toString();
            nextIdAfter = lastUser.getId();
        }

        long userCount = userRepository.count();

        List<UserDto> data = users.stream()
                .map(userMapper::toDto)
                .toList();

        return new CursorUserListResponse(data, nextCursor, nextIdAfter, hasNext,userCount,request.sortBy(),request.sortDirection());
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

        // todo 이벤트로 변경 알리기
        target.updateRole(request.role());
        return userMapper.toDto(target);
    }

    @Override
    @Transactional
    @PreAuthorize("principal.userDto.id == #userId")
    // TODO 이거 만료시간 3분 해야됨
    public UserDto updateUserPassword(UUID userId, ChangePasswordRequest request) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        String encodedPassword = passwordEncoder.encode(request.password());
        target.updatePassword(encodedPassword);
        jwtRegistry.invalidateJwtInformationByUserId(userId);
        return userMapper.toDto(target);
    }

    /**
     * 비밀번호 초기화시 사용
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto initUserPassword(UUID userId) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        String rawPassword = generateInitPassword();
        String encodedPassword = passwordEncoder.encode(rawPassword);

        target.updatePassword(encodedPassword);
        jwtRegistry.invalidateJwtInformationByUserId(userId);

        // TODO 나중에 메일로 어떻게 변경이 됬는지 보내는 이벤트가 필요함
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
        // todo 나중에 이벤트 변경 알리기, 정지면 메일로 보내야 할거 같다
        return userMapper.toDto(target);
    }

    private String generateInitPassword() {
        StringBuilder password = new StringBuilder();
        String all = upper + lower + digit + special;

        // 필수 조건 보장
        password.append(upper.charAt(secureRandom.nextInt(upper.length())));
        password.append(lower.charAt(secureRandom.nextInt(lower.length())));
        password.append(digit.charAt(secureRandom.nextInt(digit.length())));
        password.append(special.charAt(secureRandom.nextInt(special.length())));

        // 나머지 4자리 랜덤
        for (int i = 0; i < 4; i++) {
            password.append(all.charAt(secureRandom.nextInt(all.length())));
        }

        // 섞기
        return password.chars()
                .mapToObj(c -> (char) c)
                .sorted((a, b) -> secureRandom.nextInt(3) - 1)
                .collect(StringBuilder::new,
                        StringBuilder::append,
                        StringBuilder::append)
                .toString();
    }
}