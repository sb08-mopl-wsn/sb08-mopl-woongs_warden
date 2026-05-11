package com.mopl.mopl.domain.user.repository;

import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.domain.user.entity.SortDirection;
import com.mopl.mopl.domain.user.entity.User;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface UserRepositoryCustom {

    List<User> findUsersByCursor(
            String emailLike,
            Role roleEqual,
            Instant cursor,
            UUID idAfter,
            SortDirection sortDirection,
            Pageable pageable
    );

    long countUsersByEmailAndRole(
            String emailLike,
            Role roleEqual
    );
}