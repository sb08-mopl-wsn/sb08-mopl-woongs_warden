package com.mopl.mopl.global.auth.checker;

import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitAdmin implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // admin 초기 메일
    @Value("${initAdmin.create.admin}")
    private String targetAdminEmail;

    // admin 초기 비밀번호
    @Value("${initAdmin.create.pw}")
    private String passwordInitAdmin;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // ADMIN 권한을 가진 유저가 있는지 확인
        if (!userRepository.existsByRole(Role.ADMIN)) {
            log.debug("시스템에 ADMIN 유저가 없습니다. 초기 어드민 설정을 시도합니다.");

            // 어드민 권한을 부여할 타겟 유저 찾기
            userRepository.findByEmail(targetAdminEmail).ifPresentOrElse(
                    user -> {
                        user.updateRole(Role.ADMIN);
                        userRepository.save(user);
                        log.debug("[{}] 유저에게 ADMIN 권한이 부여되었습니다.", targetAdminEmail);
                    },
                    // 없으면 만들기
                    () -> {
                        log.warn("어드민으로 지정할 대상 유저([{}])가 DB에 존재하지 않아 어드민 계정을 만듭니다.", targetAdminEmail);
                        String encodedPassword = passwordEncoder.encode(passwordInitAdmin);
                        User admin = new User("admin", targetAdminEmail, encodedPassword);
                        userRepository.save(admin);
                    }
            );
        } else {
            log.debug("이미 시스템에 ADMIN 유저가 존재합니다. 초기화 작업을 건너뜁니다.");
        }
    }
}
