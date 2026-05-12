package com.mopl.mopl.global.auth.details;

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

    @Override
    @Transactional(noRollbackFor = CredentialsExpiredException.class)
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        if (user.isInit_password()&&
                user.getTemporaryPasswordExpiredAt().isBefore(Instant.now())) {
            // 임시 비밀번호로 되어있던것 다시 원래 비밀번호로 롤백
            String originalPassword = user.getTemporaryPassword();
            user.updatePassword(originalPassword);

            throw new CredentialsExpiredException("임시 비밀번호가 만료되었습니다. 원래 비밀번로를 입력하세.");
        }

        return new MoplUserDetails(userMapper.toDto(user), user.getPassword());
    }
}