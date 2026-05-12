package com.mopl.mopl.domain.user.service;

import com.mopl.mopl.domain.jwt.registry.JwtRegistry;
import com.mopl.mopl.domain.user.dto.CursorResponseUserDto;
import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.dto.request.*;
import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.exception.UserDuplicateException;
import com.mopl.mopl.domain.user.exception.UserNotFoundException;
import com.mopl.mopl.domain.user.mapper.UserMapper;
import com.mopl.mopl.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtRegistry jwtRegistry;  //todo 분산에서는 다른걸로

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

        // todo 이벤트로 변경 알리기
        target.updateRole(request.role());
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
        // todo 나중에 이벤트 변경 알리기, 정지면 메일로 보내야 할거 같다
        return userMapper.toDto(target);
    }

}