package com.mopl.mopl.global.auth.details;

import com.mopl.mopl.domain.auth.service.AuthenticationAttemptService;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.mapper.UserMapper;
import com.mopl.mopl.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class MoplUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AuthenticationAttemptService authenticationAttemptService;

    @Override
    @Transactional(noRollbackFor = CredentialsExpiredException.class)
    public UserDetails loadUserByUsername(String username) {
        authenticationAttemptService.validateLoginAvailable(username);
        return loadUserDetails(username);
    }

    public UserDetails loadUserByUsernameWithoutLoginAttemptCheck(String username) {
        return loadUserDetails(username);
    }

    private UserDetails loadUserDetails(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        Instant expiredAt = user.getTemporaryPasswordExpiredAt();

        if (user.isInitPassword() && expiredAt != null && !expiredAt.isAfter(Instant.now())) {
            throw new CredentialsExpiredException("임시 비밀번호가 만료되었습니다.");
        }

        return new MoplUserDetails(userMapper.toDto(user), user.getPassword());
    }
}