package com.mopl.mopl.domain.auth.service;

import com.mopl.mopl.domain.auth.exception.AuthExpiredTokenException;
import com.mopl.mopl.domain.auth.exception.AuthFailedRefreshToken;
import com.mopl.mopl.domain.auth.exception.AuthInvalidTokenException;
import com.mopl.mopl.domain.jwt.dto.JwtDTO;
import com.mopl.mopl.domain.jwt.dto.JwtInformation;
import com.mopl.mopl.domain.jwt.registry.JwtRegistry;
import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.exception.UserNotFoundException;
import com.mopl.mopl.domain.user.mapper.UserMapper;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.auth.JwtTokenProvider;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import com.mopl.mopl.global.auth.details.MoplUserDetailsService;
import com.mopl.mopl.global.event.user.UserPasswordInitEvent;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthServiceImpl implements AuthService {
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final JwtRegistry jwtRegistry;
    private final JwtTokenProvider jwtTokenProvider;
    private final MoplUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

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
    @Transactional(readOnly = true)
    public UserDto getCurrentUserInfo(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }

        String username = userDetails.getUsername();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public JwtDTO refresh(String refreshToken, HttpServletResponse response) {
        if (refreshToken == null || !jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new AuthInvalidTokenException();
        }

        if (!jwtRegistry.hasActiveJwtInformationByRefreshToken(refreshToken)) {
            jwtTokenProvider.expireRefreshCookie(response);
            throw new AuthExpiredTokenException();
        }

        String userEmail = jwtTokenProvider.getUserEmailFromToken(refreshToken);
        JwtInformation oldJwtInfo = jwtRegistry.getJwtInformationByRefreshToken(refreshToken);


        MoplUserDetails userDetails =
                (MoplUserDetails) userDetailsService.loadUserByUsername(userEmail);

        String newRefreshToken = null;

        try {
            String newAccessToken = jwtTokenProvider.generateAccessToken(userDetails);
            newRefreshToken = jwtTokenProvider.generateRefreshToken(userDetails);
            UserDto userDto = userDetails.getUserDto();

            JwtInformation newJwtInfo = new JwtInformation(
                    userDto,
                    newAccessToken,
                    newRefreshToken
            );

            jwtRegistry.rotateJwtInformation(refreshToken, newJwtInfo);
            jwtTokenProvider.addRefreshCookie(response, newRefreshToken);

            return new JwtDTO(userDto, newAccessToken);

        } catch (Exception e) {
            jwtRegistry.rollbackRotateJwtInformation(
                    refreshToken,
                    oldJwtInfo,
                    newRefreshToken
            );

            throw new AuthFailedRefreshToken();
        }
    }

    /**
     * 유저 비밀번호 초기화 및
     */
    @Override
    @Transactional
    public void initUserPassword(String email) {
        User target = userRepository.findByEmail(email).
                orElseThrow(() -> new UserNotFoundException());

        String originPassword = target.getPassword();
        String rawPassword = generateInitPassword();
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // 임시 비밀번호 유효시간 전달
        Instant expiredAt = Instant.now().plus(Duration.ofMinutes(3));

        // 임시 비번 적용 및 원본 비번 저장
        target.updateTemporaryPassword(encodedPassword, originPassword, expiredAt);

        eventPublisher.publishEvent(UserPasswordInitEvent.of(target,expiredAt,rawPassword));
    }

    /**
     * 임시 비밀번호 생성
     */
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

        List<Character> chars = password.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toList());

        Collections.shuffle(chars, secureRandom);
        StringBuilder shuffled = new StringBuilder(chars.size());
        chars.forEach(shuffled::append);

        return shuffled.toString();
    }
}