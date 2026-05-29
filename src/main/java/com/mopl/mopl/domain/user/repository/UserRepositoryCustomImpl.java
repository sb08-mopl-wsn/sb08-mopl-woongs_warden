package com.mopl.mopl.domain.user.repository;

import com.mopl.mopl.domain.user.entity.QUser;
import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.domain.user.entity.SortDirection;
import com.mopl.mopl.domain.user.entity.User;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QUser user = QUser.user;

    @Override
    public List<User> findUsersByCursor(
            String emailLike,
            Role roleEqual,
            Instant cursor,
            UUID idAfter,
            SortDirection sortDirection,
            Pageable pageable
    ) {
        SortDirection direction = sortDirection == null
                ? SortDirection.DESCENDING
                : sortDirection;

        return switch (direction) {
            case ASCENDING -> findUsersByCursorAsc(
                    emailLike,
                    roleEqual,
                    cursor,
                    idAfter,
                    pageable
            );
            case DESCENDING -> findUsersByCursorDesc(
                    emailLike,
                    roleEqual,
                    cursor,
                    idAfter,
                    pageable
            );
        };
    }

    private List<User> findUsersByCursorDesc(
            String emailLike,
            Role roleEqual,
            Instant cursor,
            UUID idAfter,
            Pageable pageable
    ) {
        return queryFactory
                .selectFrom(user)
                .where(
                        emailContains(emailLike),
                        roleEq(roleEqual),
                        cursorDesc(cursor, idAfter)
                )
                .orderBy(
                        user.createdAt.desc(),
                        user.id.desc()
                )
                .limit(pageable.getPageSize())
                .fetch();
    }

    private List<User> findUsersByCursorAsc(
            String emailLike,
            Role roleEqual,
            Instant cursor,
            UUID idAfter,
            Pageable pageable
    ) {
        return queryFactory
                .selectFrom(user)
                .where(
                        emailContains(emailLike),
                        roleEq(roleEqual),
                        cursorAsc(cursor, idAfter)
                )
                .orderBy(
                        user.createdAt.asc(),
                        user.id.asc()
                )
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public long countUsersByEmailAndRole(String emailLike, Role roleEqual) {
        Long count = queryFactory
                .select(user.count())
                .from(user)
                .where(
                        emailContains(emailLike),
                        roleEq(roleEqual)
                )
                .fetchOne();

        return count == null ? 0L : count;
    }

    private BooleanExpression emailContains(String emailLike) {
        if (emailLike == null || emailLike.isBlank()) {
            return null;
        }

        return user.email.contains(emailLike);
    }

    private BooleanExpression roleEq(Role roleEqual) {
        if (roleEqual == null) {
            return null;
        }

        return user.role.eq(roleEqual);
    }

    private BooleanExpression cursorDesc(Instant cursor, UUID idAfter) {
        if (cursor == null) {
            return null;
        }

        return user.createdAt.lt(cursor)
                .or(
                        user.createdAt.eq(cursor)
                                .and(user.id.lt(idAfter))
                );
    }

    private BooleanExpression cursorAsc(Instant cursor, UUID idAfter) {
        if (cursor == null) {
            return null;
        }

        return user.createdAt.gt(cursor)
                .or(
                        user.createdAt.eq(cursor)
                                .and(user.id.gt(idAfter))
                );
    }
}