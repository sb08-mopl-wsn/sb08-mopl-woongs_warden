package com.mopl.mopl.domain.notification.repository;

import static com.mopl.mopl.domain.notification.entity.QNotification.notification;

import com.mopl.mopl.domain.notification.entity.Notification;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryCustomImpl implements NotificationRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<Notification> findNotificationsByCursor(UUID userId, String sortBy, boolean isAsc,
      Instant cursor, UUID idAfter, Pageable pageable) {
    return queryFactory
        .selectFrom(notification)
        .where(
            notification.user.id.eq(userId),
            cursorCondition(isAsc, cursor, idAfter)
        )
        .orderBy(
            isAsc ? notification.createdAt.asc() : notification.createdAt.desc(),
            isAsc ? notification.id.asc() : notification.id.desc()
        )
        .limit(pageable.getPageSize())
        .fetch();
  }

  private BooleanExpression cursorCondition(boolean isAsc, Instant cursor, UUID idAfter) {
    if (cursor == null || idAfter == null) {
      return null;
    }

    if (isAsc) {
      return notification.createdAt.gt(cursor)
          .or(notification.createdAt.eq(cursor).and(notification.id.gt(idAfter)));
    } else {
      return notification.createdAt.lt(cursor)
          .or(notification.createdAt.eq(cursor).and(notification.id.lt(idAfter)));
    }
  }

}
