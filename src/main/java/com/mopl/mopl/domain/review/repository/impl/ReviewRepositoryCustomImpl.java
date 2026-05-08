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

    try {
      if ("rating".equalsIgnoreCase(sortBy)) {
        double ratingCursor = Double.parseDouble(cursor);
        BooleanExpression primaryCondition = isAsc ? review.rating.gt(ratingCursor) : review.rating.lt(ratingCursor);
        BooleanExpression tieCondition = review.rating.eq(ratingCursor).and(isAsc ? review.id.gt(idAfter) : review.id.lt(idAfter));
        return primaryCondition.or(tieCondition);
      } else { // 기본값: createdAt
        Instant createdAtCursor = Instant.parse(cursor);
        BooleanExpression primaryCondition = isAsc ? review.createdAt.gt(createdAtCursor) : review.createdAt.lt(createdAtCursor);
        BooleanExpression tieCondition = review.createdAt.eq(createdAtCursor).and(isAsc ? review.id.gt(idAfter) : review.id.lt(idAfter));
        return primaryCondition.or(tieCondition);
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
