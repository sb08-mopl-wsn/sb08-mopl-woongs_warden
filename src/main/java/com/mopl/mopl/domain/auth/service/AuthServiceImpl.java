package com.mopl.mopl.domain.auth.service;

import com.mopl.mopl.domain.jwt.registry.JwtRegistry;
import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.dto.request.UserRoleUpdateRequest;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.exception.UserNotFoundException;
import com.mopl.mopl.domain.user.mapper.UserMapper;
import com.mopl.mopl.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthServiceImpl implements AuthService {
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final JwtRegistry jwtRegistry;

    @Override
    @Transactional(readOnly = true)
    public UserDto getCurrentUserInfo(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }

        String username = userDetails.getUsername();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        UserDto userDto = userMapper.toDto(user);
        return userDto;
    }

    @Override
    @Transactional
    public UserDto updateRole(UUID userId,UserRoleUpdateRequest userRoleUpdateRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        user.updateRole(userRoleUpdateRequest.role());
        User updatedUser = userRepository.save(user);
        invalidateUserTokens(updatedUser.getId());

        return userMapper.toDto(updatedUser);
    }

    private void invalidateUserTokens(UUID userId) {
        try {
            // InMemoryJwtRegistry(및 DB)에 저장된 해당 유저의 토큰 정보를 모두 삭제/폐기 처리합니다.
            jwtRegistry.invalidateJwtInformationByUserId(userId);
            log.info("[AuthService] 유저(ID: {})의 모든 활성 JWT 토큰이 무효화되었습니다.", userId);
        } catch (Exception e) {
            log.error("[AuthService] JWT 무효화 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}