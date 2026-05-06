package com.mopl.domain.user.repository;

import com.mopl.domain.user.entity.Role;
import com.mopl.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByRole(Role role);

    Optional<User> findByEmail(String email);


    @Query("SELECT u FROM User u "
            + "LEFT JOIN FETCH u.profile ")
    List<User> findAllWithProfile();
}
