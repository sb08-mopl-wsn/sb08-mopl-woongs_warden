package com.mopl.mopl.domain.conversation.repository;

import static com.mopl.mopl.domain.conversation.entity.QConversation.conversation;
import static com.mopl.mopl.domain.dm.entity.QDirectMessage.directMessage;

import com.mopl.mopl.domain.conversation.entity.Conversation;
import com.mopl.mopl.domain.user.entity.QUser;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ConversationRepositoryCustomImpl implements ConversationRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<Conversation> findMyConversationsByCursor(
      UUID userId, String keywordLike, String sortBy, boolean isAsc, Instant cursor, UUID idAfter, Pageable pageable) {

    QUser senderUser = new QUser("senderUser");
    QUser receiverUser = new QUser("receiverUser");

    return queryFactory
        .selectFrom(conversation)
        .join(conversation.sender, senderUser).fetchJoin()
        .join(conversation.receiver, receiverUser).fetchJoin()
        .where(
            // 내가 보냈거나 받은 방
            conversation.sender.id.eq(userId).or(conversation.receiver.id.eq(userId)),
            keywordCondition(userId, keywordLike, senderUser, receiverUser),
            cursorCondition(sortBy, isAsc, cursor, idAfter)
        )
        .orderBy(dynamicOrder(sortBy, isAsc), isAsc ? conversation.id.asc() : conversation.id.desc())
        .limit(pageable.getPageSize())
        .fetch();
  }

  // 동적 쿼리 헬퍼 메서드
  private BooleanExpression cursorCondition(String sortBy, boolean isAsc, Instant cursor, UUID idAfter) {

    // 첫 페이지 조회 시 조건 무시
    if (cursor == null || idAfter == null) {
      return null;
    }

    if ("createdAt".equals(sortBy)) {
      return isAsc ?
          conversation.createdAt.gt(cursor).or(conversation.createdAt.eq(cursor).and(conversation.id.gt(idAfter))) :
          conversation.createdAt.lt(cursor).or(conversation.createdAt.eq(cursor).and(conversation.id.lt(idAfter)));
    } else {
      return isAsc ?
          conversation.updatedAt.gt(cursor).or(conversation.updatedAt.eq(cursor).and(conversation.id.gt(idAfter))) :
          conversation.updatedAt.lt(cursor).or(conversation.updatedAt.eq(cursor).and(conversation.id.lt(idAfter)));
    }
  }

  private OrderSpecifier<?> dynamicOrder(String sortBy, boolean isAsc) {
    if ("createdAt".equals(sortBy)) {
      return isAsc ? conversation.createdAt.asc() : conversation.createdAt.desc();
    }

    // 기본값 updatedAt
    return isAsc ? conversation.updatedAt.asc() : conversation.updatedAt.desc();
  }

  private BooleanExpression keywordCondition(UUID myId, String keywordLike, QUser senderUser, QUser receiverUser) {
    if (keywordLike == null || keywordLike.isBlank()) {
      return null;
    }

    // 1. 상대방 이름 검색
    BooleanExpression nameMatch = conversation.sender.id.eq(myId).and(receiverUser.name.containsIgnoreCase(keywordLike))
        .or(conversation.receiver.id.eq(myId).and(senderUser.name.containsIgnoreCase(keywordLike)));

    // 2. 대화 내용 검색 (서브쿼리 활용: 해당 방의 메시지 중 하나라도 검색어가 포함되어 있는지)
    BooleanExpression contentMatch = JPAExpressions.selectOne()
        .from(directMessage)
        .where(directMessage.conversation.id.eq(conversation.id)
            .and(directMessage.content.containsIgnoreCase(keywordLike)))
        .exists();

    // 이름이나 내용 중 하나라도 매칭되면 반환
    return nameMatch.or(contentMatch);
  }
}
