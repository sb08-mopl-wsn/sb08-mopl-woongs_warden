package com.mopl.mopl.domain.review.repository.impl;

import static com.mopl.mopl.domain.review.entity.QReview.review;

import com.mopl.mopl.domain.review.dto.request.ReviewSearchRequest;
import com.mopl.mopl.domain.review.entity.Review;
import com.mopl.mopl.domain.review.exception.ReviewCursorException;
import com.mopl.mopl.domain.review.repository.ReviewRepositoryCustom;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
@Repository
public class ReviewRepositoryCustomImpl implements ReviewRepositoryCustom {

  private final JPAQueryFactory jpaQueryFactory;

  @Override
  public Slice<Review> findReviews(ReviewSearchRequest request) {
    boolean isAsc = "ASC".equalsIgnoreCase(request.sortDirection());

    List<Review> result = jpaQueryFactory
        .selectFrom(review)
        .where(
            review.content.id.eq(request.contentId()),
            cursorPageCondition(request.sortBy(), request.cursor(), request.idAfter(), isAsc)
        )
        .orderBy(
            orderByCondition(request.sortBy(), isAsc),
            isAsc ? review.id.asc() : review.id.desc() // 2차 정렬
        )
        .limit(getLimit(request.limit()) + 1)
        .fetch();

    boolean hasNext = result.size() > getLimit(request.limit());
    if (hasNext) {
      result.removeLast();
    }

    return new SliceImpl<>(result, Pageable.unpaged(), hasNext);
  }

  @Override
  public long countReviews(UUID contentId) {
    Long count = jpaQueryFactory
        .select(review.count())
        .from(review)
        .where(review.content.id.eq(contentId))
        .fetchOne();
    return count != null ? count : 0L;
  }

  private BooleanExpression cursorPageCondition(String sortBy, String cursor, UUID idAfter, boolean isAsc) {
    if ((cursor == null) != (idAfter == null)) {
      throw new ReviewCursorException();
    }
    if (cursor == null) {
      return null; // 첫 페이지 조회
    }

    String effectiveSortBy = StringUtils.hasText(sortBy) ? sortBy : "createdAt";

    try {
      switch (effectiveSortBy.toLowerCase()) {
        case "rating":
          double ratingCursor = Double.parseDouble(cursor);
          BooleanExpression primaryRating = isAsc ? review.rating.gt(ratingCursor) : review.rating.lt(ratingCursor);
          BooleanExpression tieRating = review.rating.eq(ratingCursor).and(isAsc ? review.id.gt(idAfter) : review.id.lt(idAfter));
          return primaryRating.or(tieRating);

        case "createdat":
        default: // 기본값 처리
          Instant createdAtCursor = Instant.parse(cursor);
          BooleanExpression primaryCreatedAt = isAsc ? review.createdAt.gt(createdAtCursor) : review.createdAt.lt(createdAtCursor);
          BooleanExpression tieCreatedAt = review.createdAt.eq(createdAtCursor).and(isAsc ? review.id.gt(idAfter) : review.id.lt(idAfter));
          return primaryCreatedAt.or(tieCreatedAt);
      }
    } catch (NumberFormatException | DateTimeParseException e) {
      throw new ReviewCursorException();
    }
  }

  private OrderSpecifier<?> orderByCondition(String sortBy, boolean isAsc) {
    if ("rating".equalsIgnoreCase(sortBy)) {
      return isAsc ? review.rating.asc() : review.rating.desc();
    }
    // 기본값: createdAt
    return isAsc ? review.createdAt.asc() : review.createdAt.desc();
  }

  private int getLimit(Integer limit) {
    return limit == null ? 10 : limit;
  }
}
