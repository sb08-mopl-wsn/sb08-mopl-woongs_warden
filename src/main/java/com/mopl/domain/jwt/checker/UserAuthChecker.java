package com.mopl.domain.jwt.checker;

import com.mopl.domain.jwt.details.moplUserDetails;
import com.mopl.domain.user.dto.UserDto;
import com.mopl.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component("userAuthChecker")
public class UserAuthChecker {
    private final UserRepository userRepository;

    public boolean isOwner(Authentication authentication, UUID userId) {
        // 로그인 검증
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return false;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof moplUserDetails userDetails) {
            UserDto userDto = userDetails.getUserDto();
            return userDto.id().equals(userId);
        }
        return false;
    }
}