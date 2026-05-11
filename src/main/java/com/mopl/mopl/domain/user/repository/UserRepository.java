package com.mopl.mopl.domain.user.repository;

import com.mopl.mopl.domain.notification.entity.Notification;
import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);
    boolean existsByName(String username);
    boolean existsByRole(Role role);
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u " +
            "WHERE (:emailLike IS NULL OR u.email LIKE %:emailLike%) " +
            "AND (:roleEqual IS NULL OR u.role = :roleEqual) " +
            "AND (CAST(:cursor AS timestamp) IS NULL OR " +
            "     u.createdAt < :cursor OR " +
            "    (u.createdAt = :cursor AND (:idAfter IS NULL OR u.id < :idAfter))) " +
            "ORDER BY u.createdAt DESC, u.id DESC")
    List<User> findUsersByCursorDesc(
            @Param("emailLike") String emailLike,
            @Param("roleEqual") Role roleEqual,
            @Param("cursor") Instant cursor,
            @Param("idAfter") UUID idAfter,
            Pageable pageable
    );

    // 오름차순 (ASC) - 과거순
    @Query("SELECT u FROM User u " +
            "WHERE (:emailLike IS NULL OR u.email LIKE %:emailLike%) " +
            "AND (:roleEqual IS NULL OR u.role = :roleEqual) " +
            "AND (CAST(:cursor AS timestamp) IS NULL OR " +
            "     u.createdAt > :cursor OR " +
            "    (u.createdAt = :cursor AND (:idAfter IS NULL OR u.id > :idAfter))) " +
            "ORDER BY u.createdAt ASC, u.id ASC")
    List<User> findUsersByCursorAsc(
            @Param("emailLike") String emailLike,
            @Param("roleEqual") Role roleEqual,
            @Param("cursor") Instant cursor,
            @Param("idAfter") UUID idAfter,
            Pageable pageable
    );
}