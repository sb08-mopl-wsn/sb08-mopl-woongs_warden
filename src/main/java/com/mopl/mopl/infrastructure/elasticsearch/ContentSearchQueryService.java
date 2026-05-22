package com.mopl.mopl.infrastructure.elasticsearch;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import com.mopl.mopl.infrastructure.elasticsearch.document.ContentDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ContentSearchQueryService
{
    private static final int CANDIDATE_SIZE = 30;

    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 의도 분석 결과를 기반으로 Elasticsearch에서 추천 후보 콘텐츠 ID 목록을 검색한다.
     * <p>
     * nori를 활용한 한국어 검색을 지원하며,
     * tags(3), description(2), title(1) 순으로 가중치를 두어 관련도를 평가한다.
     * fuzziness 옵션으로 오타가 있어도 유사한 결과를 반환한다.
     *
     * <p>
     * contentType이 null이면 전체 타입을 대상으로 검색하고,
     * keywords가 비어있으면 전체 콘텐츠를 대상으로 검색한다.
     * 결과는 관련도 기준 상위 {@value #CANDIDATE_SIZE}건으로 제한된다.
     *
     * @param contentType   콘텐츠 타입 필터 (예: "movie", "tvSeries", "sport"). null이면 전체 타입 검색
     * @param keywords      검색 키워드 목록. null 또는 빈 리스트이면 키워드 조건 없이 검색
     * @return 후보 콘텐츠 ID(UUID 문자열) 목록. 관련도 높은 순으로 정렬됨
     */
    public List<String> searchCandidateIds(String contentType, List<String> keywords) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        // 제일 먼저 contentType와 정확히 일치하는지 필터링
        if (contentType != null && !contentType.isBlank()) {
            boolQuery.filter(f -> f.term(t -> t.field("contentType").value(contentType)));
        }

        // 키워드 검색
        // 가중치 부여, 오타 허용, 최소 1개는 매칭
        if (keywords != null && !keywords.isEmpty()) {
            String keywordQuery = String.join(" ", keywords);
            boolQuery.should(s -> s.multiMatch(m -> m
                    .query(keywordQuery)
                    .fields("tags^3", "description^2", "title")
                    .fuzziness("AUTO")
            ));
            boolQuery.minimumShouldMatch("1");
        }

        // 상위 30개만
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQuery.build()))
                .withPageable(PageRequest.of(0, CANDIDATE_SIZE))
                .build();

        SearchHits<ContentDocument> hits = elasticsearchOperations.search(query, ContentDocument.class);

        // id만 추출 -> DB 조회
        List<String> ids = hits.stream()
                .map(hit -> hit.getContent().getId())
                .toList();

        log.info("[ES] 후보 검색 — contentType={}, keywords={}, 결과={}건", contentType, keywords, ids.size());

        return ids;
    }
}
