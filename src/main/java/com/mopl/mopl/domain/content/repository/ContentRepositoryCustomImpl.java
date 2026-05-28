package com.mopl.mopl.domain.content.repository;

import com.mopl.mopl.domain.content.dto.request.ContentSearchRequest;
import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.entity.ContentType;
import com.mopl.mopl.domain.content.exception.ContentCursorException;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

import static com.mopl.mopl.domain.content.entity.QContent.content;

@RequiredArgsConstructor
@Repository
public class ContentRepositoryCustomImpl implements ContentRepositoryCustom
{
    private final JPAQueryFactory jpaQueryFactory;

    /**
     * 콘텐츠 목록을 커서 페이지네이션으로 조회한다.
     *
     * @param contentSearchRequest 검색 정보
     * @return 콘텐츠 목록과 다음 페이지 존재 여부 등
     */
    @Override
    public Slice<Content> getContents(ContentSearchRequest contentSearchRequest, List<UUID> searchedIds) {
        boolean isAsc = "ASCENDING".equalsIgnoreCase(contentSearchRequest.sortDirection());

        List<Content> result = jpaQueryFactory
                .selectFrom(content)
                .where(
                        typeCondition(contentSearchRequest.typeEqual()),
                        searchedIdsCondition(searchedIds),
                        cursorPageCondition(
                                contentSearchRequest.sortBy(),
                                contentSearchRequest.cursor(),
                                contentSearchRequest.idAfter(),
                                isAsc
                        )
                )
                .orderBy(
                        orderByCondition(contentSearchRequest.sortBy(), isAsc),
                        isAsc ? content.id.asc() : content.id.desc()
                )
                .limit(contentSearchRequest.limit() + 1)
                .fetch();

        boolean hasNext = result.size() > contentSearchRequest.limit();
        if (hasNext) result.removeLast();

        return new SliceImpl<>(result, Pageable.unpaged(), hasNext);
    }

    private BooleanExpression searchedIdsCondition(List<UUID> searchedIds) {
        if (searchedIds == null) return null;
        return content.id.in(searchedIds);
    }

    /**
     * 키워드 조건만 적용해 전체 콘텐츠 수를 반환한다.
     *
     * @param keywordLike 검색 키워드
     * @return 전체 콘텐츠 수
     */
    @Override
    public long countContentsWithKeyword(String keywordLike) {
        Long count = jpaQueryFactory
                .select(content.count())
                .from(content)
                .where(keywordCondition(keywordLike))
                .fetchOne();

        return count != null ? count : 0L;
    }

    /**
     * 콘텐츠 타입 필터 조건
     *
     * @param typeEqual 검색 조건(타입)
     * @return QueryDSL 조건식
     */
    private BooleanExpression typeCondition(String typeEqual) {
        if (typeEqual == null || typeEqual.isBlank()) return null;
        return content.contentType.eq(ContentType.from(typeEqual));
    }

    /**
     * 키워드로 제목, 설명, 태그를 대소문자 구분 없이 부분일치 검색한다.
     *
     * @param keywordLike 검색 조건(키워드)
     * @return QueryDSL 조건식
     */
    private BooleanExpression keywordCondition(String keywordLike) {
        if (keywordLike == null || keywordLike.isBlank()) return null;

        return content.title.containsIgnoreCase(keywordLike)
                .or(content.description.containsIgnoreCase(keywordLike));
    }

    /**
     * 1차 커서(cursor)와 2차 커서(idAfter)를 조합해 페이지네이션 조건을 생성한다.
     * <p>
     * cursor와 idAfter가 둘 다 null이면 첫 페이지로 간주한다.
     *
     * @param sortBy    정렬 기준
     * @param cursor    1차 커서 값
     * @param idAfter   2차 커서 값
     * @param isAsc     정렬 방향
     * @return QueryDSL 조건식
     */
    private BooleanExpression cursorPageCondition(String sortBy, String cursor, UUID idAfter, boolean isAsc) {
        if ((cursor == null) ^ (idAfter == null)) {
            throw new ContentCursorException();
        }

        if (cursor == null) return null;

        BooleanExpression primaryEq;
        BooleanExpression primaryGtLt;

        try {
            switch (sortBy == null ? "createdAt" : sortBy) {
                case "rate" -> {
                    BigDecimal value = new BigDecimal(cursor);
                    primaryEq = content.avgRating.eq(value);
                    primaryGtLt = isAsc ? content.avgRating.gt(value) : content.avgRating.lt(value);
                }
                case "watcherCount" -> {
                    int value = Integer.parseInt(cursor);
                    primaryEq = content.watcherCount.eq(value);
                    primaryGtLt = isAsc ? content.watcherCount.gt(value) : content.watcherCount.lt(value);
                }
                default -> {
                    Instant value = Instant.parse(cursor);
                    primaryEq = content.createdAt.eq(value);
                    primaryGtLt = isAsc ? content.createdAt.gt(value) : content.createdAt.lt(value);
                }
            }
        } catch (NumberFormatException | DateTimeParseException e) {
            throw new ContentCursorException();
        }

        BooleanExpression secondaryGtLt = isAsc
                ? content.id.gt(idAfter)
                : content.id.lt(idAfter);

        return primaryGtLt.or(primaryEq.and(secondaryGtLt));
    }

    /**
     * 정렬 기준과 방향으로 OrderSpecifier를 생성한다.
     *
     * @param sortBy    정렬 기준
     * @param isAsc     정렬 방향
     * @return 정렬 조건
     */
    private OrderSpecifier<?> orderByCondition(String sortBy, boolean isAsc) {
        return switch (sortBy == null ? "createdAt" : sortBy) {
            case "rate" -> isAsc ? content.avgRating.asc() : content.avgRating.desc();
            case "watcherCount" -> isAsc ? content.watcherCount.asc() : content.watcherCount.desc();
            default -> isAsc ? content.createdAt.asc() : content.createdAt.desc();
        };
    }
}
