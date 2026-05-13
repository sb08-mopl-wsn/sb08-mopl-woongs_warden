package com.mopl.mopl.domain.dm.repository;

import static com.mopl.mopl.domain.dm.entity.QDirectMessage.directMessage;
import static com.mopl.mopl.domain.user.entity.QUser.user;

import com.mopl.mopl.domain.dm.entity.DirectMessage;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DirectMessageRepositoryCustomImpl implements DirectMessageRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public DirectMessage findLatestMessage(UUID conversationId) {
    return queryFactory
        .selectFrom(directMessage)
        .join(directMessage.sender, user).fetchJoin()
        .where(directMessage.conversation.id.eq(conversationId))
        .orderBy(directMessage.createdAt.desc(), directMessage.id.desc())
        .limit(1)
        .fetchOne();
  }

  @Override
  public List<DirectMessage> findMessagesByCursor(UUID conversationId, Instant cursor, UUID idAfter,
      int limit, String sortDirection) {
    boolean isAsc = "ASCENDING".equalsIgnoreCase(sortDirection);

    return queryFactory
        .selectFrom(directMessage)
        .join(directMessage.sender, user).fetchJoin()
        .where(
            directMessage.conversation.id.eq(conversationId),
            cursorCondition(cursor, idAfter, isAsc)
        )
        .orderBy(
            isAsc ? directMessage.createdAt.asc() : directMessage.createdAt.desc(),
            isAsc ? directMessage.id.asc() : directMessage.id.desc()
        )
        .limit(limit + 1)
        .fetch();
  }

  // 1차 커서(createdAt)와 2차 커서(id)를 조합한 동적 조건 생성
  private BooleanExpression cursorCondition(Instant cursor, UUID idAfter, boolean isAsc) {
    if (cursor == null || idAfter == null) {
      return null;
    }

    // isAsc 일 때
    if (isAsc) {
      return directMessage.createdAt.gt(cursor)
          .or(directMessage.createdAt.eq(cursor).and(directMessage.id.gt(idAfter)));
    }
    // isDesc 일 때
    return directMessage.createdAt.lt(cursor)
        .or(directMessage.createdAt.eq(cursor).and(directMessage.id.lt(idAfter)));
  }

}
