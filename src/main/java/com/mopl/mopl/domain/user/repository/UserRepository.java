package com.mopl.mopl.domain.user.repository;

import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.domain.user.entity.Social;
import com.mopl.mopl.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, UserRepositoryCustom {

    boolean existsByEmail(String email);

    boolean existsByName(String username);

    boolean existsByRole(Role role);

    Optional<User> findByEmail(String email);

    Optional<User> findBySocialTypeAndSocialId(Social socialType, String socialId);

    List<User> findAllByIsBannedTrueAndBanExpiresAtBeforeAndIsLockedFalse(LocalDateTime now);
}