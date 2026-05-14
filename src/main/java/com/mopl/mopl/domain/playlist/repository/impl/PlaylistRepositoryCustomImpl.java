package com.mopl.mopl.domain.playlist.repository.impl;

import static com.mopl.mopl.domain.playlist.entity.QPlaylist.playlist;
import static com.mopl.mopl.domain.playlist.entity.QPlaylistSubscription.playlistSubscription;
import static com.mopl.mopl.domain.user.entity.QUser.user;

import com.mopl.mopl.domain.playlist.dto.request.PlaylistSearchRequest;
import com.mopl.mopl.domain.playlist.entity.Playlist;
import com.mopl.mopl.domain.playlist.exception.PlaylistCursorException;
import com.mopl.mopl.domain.playlist.repository.PlaylistRepositoryCustom;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PlaylistRepositoryCustomImpl implements PlaylistRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public Slice<Playlist> findPlaylists(PlaylistSearchRequest request) {
    int limit = request.limit() != null ? request.limit() : 10;
    PageRequest pageRequest = PageRequest.of(0, limit);

    List<Playlist> playlists = queryFactory
        .selectFrom(playlist)
        .join(playlist.user, user).fetchJoin()
        .where(
            keywordContains(request.keyword()),
            userIdEq(request.ownerIdEqual()),
            subscriberIdEq(request.subscriberIdEqual()),
            cursorCondition(request)
        )
        .orderBy(createOrderSpecifier(request.sortBy(), request.sortDirection()))
        .limit(limit + 1)
        .fetch();

    boolean hasNext = false;
    if (playlists.size() > limit) {
      playlists.remove(limit);
      hasNext = true;
    }

    return new SliceImpl<>(playlists, pageRequest, hasNext);
  }

  @Override
  public long countPlaylists(PlaylistSearchRequest request) {
    Long count = queryFactory
        .select(playlist.count())
        .from(playlist)
        .where(
            keywordContains(request.keyword()),
            userIdEq(request.ownerIdEqual()),
            subscriberIdEq(request.subscriberIdEqual())
        )
        .fetchOne();
    return count == null ? 0L : count;
  }

  private BooleanExpression keywordContains(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return null;
    }
    return playlist.title.containsIgnoreCase(keyword)
        .or(playlist.description.containsIgnoreCase(keyword));
  }

  private BooleanExpression cursorCondition(PlaylistSearchRequest request) {
    if (request.cursor() == null || request.cursor().isBlank()) {
      return null;
    }

    if (request.idAfter() == null) {
      throw new PlaylistCursorException();
    }

    String sortBy = request.sortBy() != null ? request.sortBy().toLowerCase() : "updatedat";
    boolean isAsc = "asc".equalsIgnoreCase(request.sortDirection());

    if ("subscribercount".equals(sortBy)) {
      final Long cursorValue;
      try {
        cursorValue = Long.parseLong(request.cursor());
      } catch (NumberFormatException e) {
        throw new PlaylistCursorException();
      }
      UUID idAfter = request.idAfter();
      if (isAsc) {
        return playlist.subscriberCount.gt(cursorValue)
            .or(playlist.subscriberCount.eq(cursorValue).and(playlist.id.gt(idAfter)));
      } else {
        return playlist.subscriberCount.lt(cursorValue)
            .or(playlist.subscriberCount.eq(cursorValue).and(playlist.id.gt(idAfter)));
      }
    }

    final Instant cursorValue;
    try {
      cursorValue = Instant.parse(request.cursor());
    } catch (DateTimeParseException e) {
      throw new PlaylistCursorException();
    }
    UUID idAfter = request.idAfter();
    if (isAsc) {
      return playlist.updatedAt.gt(cursorValue)
          .or(playlist.updatedAt.eq(cursorValue).and(playlist.id.gt(idAfter)));
    } else {
      return playlist.updatedAt.lt(cursorValue)
          .or(playlist.updatedAt.eq(cursorValue).and(playlist.id.gt(idAfter)));
    }
  }

  private OrderSpecifier<?>[] createOrderSpecifier(String sortBy, String sortDirection) {
    Order direction = "asc".equalsIgnoreCase(sortDirection) ? Order.ASC : Order.DESC;
    String sortProperty = sortBy != null ? sortBy.toLowerCase() : "updatedat";

    OrderSpecifier<?> mainOrder;
    if ("subscribercount".equals(sortProperty)) {
      mainOrder = new OrderSpecifier<>(direction, playlist.subscriberCount);
    } else {
      // 기본값: updatedAt (최신순)
      mainOrder = new OrderSpecifier<>(direction, playlist.updatedAt);
    }

    OrderSpecifier<UUID> secondaryOrder = new OrderSpecifier<>(Order.ASC, playlist.id);

    return new OrderSpecifier[]{mainOrder, secondaryOrder};
  }

  private BooleanExpression userIdEq(UUID userId) {
    return userId != null ? playlist.user.id.eq(userId) : null;
  }

  private BooleanExpression subscriberIdEq(UUID subscriberId) {
    if (subscriberId == null) {
      return null;
    }
    return playlist.id.in(
        JPAExpressions.select(playlistSubscription.playlist.id)
            .from(playlistSubscription)
            .where(playlistSubscription.user.id.eq(subscriberId))
    );
  }
}