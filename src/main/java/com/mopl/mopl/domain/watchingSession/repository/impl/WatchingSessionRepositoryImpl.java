package com.mopl.mopl.domain.watchingSession.repository.impl;

import com.mopl.mopl.domain.watchingSession.dto.response.WatchingSessionSearchCondition;
import com.mopl.mopl.domain.watchingSession.entity.SortDirection;
import com.mopl.mopl.domain.watchingSession.entity.WatchingSession;
import com.mopl.mopl.domain.watchingSession.repository.WatchingSessionRepositoryCustom;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

import static com.mopl.mopl.domain.content.entity.QContent.content;
import static com.mopl.mopl.domain.user.entity.QUser.user;
import static com.mopl.mopl.domain.watchingSession.entity.QWatchingSession.watchingSession;

@Repository
@RequiredArgsConstructor
public class WatchingSessionRepositoryImpl implements WatchingSessionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 커서 기반 페이징을 사용하여 특정 콘텐츠의 시청 세션 목록을 조회한다.
     *
     * @param condition 검색 조건 (contentId, cursor, username, idAfter, sortDirection)
     * @param pageable 페이징 정보
     * @return 검색 조건에 부합하는 시청 세션 리스트
     */
    @Override
    public List<WatchingSession> findAllByCursor(WatchingSessionSearchCondition condition, Pageable pageable) {
        return queryFactory
                .selectFrom(watchingSession)
                .leftJoin(watchingSession.content, content).fetchJoin()
                .leftJoin(watchingSession.user, user).fetchJoin()
                .where(
                        contentIdEq(condition.contentId()),
                        userNameEq(condition.username()),
                        cursorCondition(condition.cursor(), condition.idAfter(), condition.sortDirection())
                )
                .orderBy(createOrderSpecifier(condition.sortDirection()))
                .limit(pageable.getPageSize())
                .fetch();
    }

    // 완전 일치 : contentId
    private BooleanExpression contentIdEq(UUID contentId) {
        return contentId != null ? watchingSession.content.id.eq(contentId) : null;
    }

    // 완전 일치: username
    private BooleanExpression userNameEq(String username) {
        return username != null ? watchingSession.user.name.eq(username) : null;
    }

    // 커서(생성일)와 보조 식별자(ID)를 기반으로 페이징 필터링 조건을 생성한다.
    // 중복된 생성일이 존재하는 경우 보조 커서(idAfter)를 사용한다.
    private BooleanExpression cursorCondition(String cursor, UUID idAfter, SortDirection sortDirection) {
        if (cursor == null || cursor.isBlank() || idAfter == null) {
            return null;
        }

        final Instant cursorTime;

        // TODO: 추후 예외 처리 필수
        try {
            cursorTime = Instant.parse(cursor);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("유효하지 않은 cursor 형식입니다.", e);
        }

        // 오름차순: (createdAt > cursorTime) OR (createdAt == cursorTime AND id > idAfter)
        if (sortDirection == SortDirection.ASCENDING) {
            return watchingSession.createdAt.gt(cursorTime)
                    .or(watchingSession.createdAt.eq(cursorTime).and(watchingSession.id.gt(idAfter)));
        }

        // 내림차순: (createdAt < cursorTime) OR (createdAt == cursorTime AND id < idAfter)
        return watchingSession.createdAt.lt(cursorTime)
                .or(watchingSession.createdAt.eq(cursorTime).and(watchingSession.id.lt(idAfter)));
    }

    // 정렬 방향 조건
    // 성능과 정확성을 위한 [생성일, ID] 순으로 복합 정렬 적용
    private OrderSpecifier<?>[] createOrderSpecifier(SortDirection sortDirection) {

        // 오름 차순
        if (sortDirection == SortDirection.ASCENDING) {
            return new OrderSpecifier[]{
                    watchingSession.createdAt.asc(),
                    watchingSession.id.asc()
            };
        }
        // 내림차순
        return new OrderSpecifier[]{
                watchingSession.createdAt.desc(),
                watchingSession.id.desc()
        };
    }
}
