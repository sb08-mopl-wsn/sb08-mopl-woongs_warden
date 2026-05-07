package com.mopl.mopl.domain.user.service;

import com.mopl.mopl.domain.jwt.registry.JwtRegistry;
import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.dto.request.ChangePasswordRequest;
import com.mopl.mopl.domain.user.dto.request.UserCreateRequest;
import com.mopl.mopl.domain.user.dto.request.UserLockUpdateRequest;
import com.mopl.mopl.domain.user.dto.request.UserRoleUpdateRequest;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.exception.UserDuplicateException;
import com.mopl.mopl.domain.user.exception.UserNotFoundException;
import com.mopl.mopl.domain.user.mapper.UserMapper;
import com.mopl.mopl.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtRegistry jwtRegistry;

    // admin이 유저를 초기화할 경우 쓸 비밀번호
    @Value("${initPassword.init.user}")
    private String initPassword;

    @Override
    @Transactional
    public UserDto createUser(UserCreateRequest request) {
        try {
            String password = passwordEncoder.encode(request.password());
            User user = new User(request.name(), request.email(), password, null, null);

            // email이 유니크라 중복이면 자동으로 취소되면서 에러로 이동
            userRepository.saveAndFlush(user);
            log.info("[User-Service] 작업 완료");
            return userMapper.toDto(user);

        } catch (DataIntegrityViolationException e) {
            log.error("[User-Service] 에러 중복 이메일: detail = {}", request.email());
            throw new UserDuplicateException();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        UserDto result = userMapper.toDto(user);
        log.info("[User-Service] 작업 완료: content {}", result.id());
        return result;
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto updateUserRole(UUID userId, UserRoleUpdateRequest request) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        target.updateRole(request.role());

        log.info("[User-Service] 사용자 권한 수정 완료: userId = {}, name = {}", userId, target.getName());
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

        log.info("[User-Service] 사용자 비밀번호 수정 완료: userId = {}, name = {}", userId, target.getName());
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

        String encodedPassword = passwordEncoder.encode(initPassword);
        target.updatePassword(encodedPassword);

        log.info("[User-Service] 사용자 비밀번호 수정 완료: userId = {}, name = {}", userId, target.getName());
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

        log.info("[User-Service] 사용자 잠금 상태 수정 완료: userId = {}", userId);
        return userMapper.toDto(target);
    }
}