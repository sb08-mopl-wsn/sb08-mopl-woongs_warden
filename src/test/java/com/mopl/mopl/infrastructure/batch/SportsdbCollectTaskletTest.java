package com.mopl.mopl.infrastructure.batch;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.entity.ContentType;
import com.mopl.mopl.domain.content.repository.ContentRepository;
import com.mopl.mopl.infrastructure.elasticsearch.ContentIndexService;
import com.mopl.mopl.infrastructure.external.constants.ExternalApiConstants;
import com.mopl.mopl.infrastructure.external.exception.ApiEmptyResponseException;
import com.mopl.mopl.infrastructure.external.sportsdb.SportsdbApiClient;
import com.mopl.mopl.infrastructure.external.sportsdb.dto.SportsdbEvent;
import com.mopl.mopl.infrastructure.external.sportsdb.mapper.SportsdbContentMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SportsCollectTasklet Test")
class SportsdbCollectTaskletTest
{
    @Mock private SportsdbApiClient sportsdbApiClient;
    @Mock private SportsdbContentMapper sportsdbContentMapper;
    @Mock private ContentRepository contentRepository;
    @Mock private StepContribution contribution;
    @Mock private ChunkContext chunkContext;
    @Mock private EntityManager entityManager;
    @Mock private MeterRegistry meterRegistry;
    @Mock private Counter counter;
    @Mock private ContentIndexService contentIndexService;

    private SportsdbCollectTasklet tasklet;

    @BeforeEach
    void setUp() {
        tasklet = new SportsdbCollectTasklet(sportsdbApiClient, sportsdbContentMapper, contentRepository, entityManager, meterRegistry, contentIndexService);
    }

    @Test
    @DisplayName("SportsDB 콘텐츠 수집 성공")
    void givenNewEvents_whenExecute_thenSavesAll() throws Exception {
        // given
        SportsdbEvent event = new SportsdbEvent(
                "123", "Liverpool vs Chelsea", "Soccer",
                "English Premier League", "1",
                "https://thumb.jpg", "filename", "Anfield", "2025-08-15"
        );

        Content content = Content.builder()
                .title("Liverpool vs Chelsea")
                .externalId("123")
                .contentType(ContentType.sport)
                .build();

        when(sportsdbApiClient.fetchDayEvents(anyInt())).thenReturn(List.of(event));
        when(sportsdbContentMapper.sportToContent(event)).thenReturn(content);
        when(contentRepository.existsByExternalIdAndContentType(anyString(), any())).thenReturn(false);
        when(meterRegistry.counter(anyString())).thenReturn(counter);

        // when
        RepeatStatus status = tasklet.execute(contribution, chunkContext);

        // then
        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        verify(contentRepository, times(ExternalApiConstants.LEAGUE_IDS.size())).saveAndFlush(any(Content.class));

    }

    @Test
    @DisplayName("중복 데이터는 저장하지 않음")
    void givenExistingEvent_whenExecute_thenSkipsDuplicate() throws Exception {
        // given
        SportsdbEvent event = new SportsdbEvent(
                "123", "Liverpool vs Chelsea", "Soccer",
                "English Premier League", "1",
                "https://thumb.jpg", "filename", "Anfield", "2025-08-15"
        );

        Content content = Content.builder()
                .title("Liverpool vs Chelsea")
                .externalId("123")
                .contentType(ContentType.sport)
                .build();

        when(sportsdbApiClient.fetchDayEvents(anyInt())).thenReturn(List.of(event));
        when(sportsdbContentMapper.sportToContent(event)).thenReturn(content);
        when(contentRepository.existsByExternalIdAndContentType("123", ContentType.sport)).thenReturn(true);
        when(meterRegistry.counter(anyString())).thenReturn(counter);

        // when
        RepeatStatus status = tasklet.execute(contribution, chunkContext);

        // then
        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        verify(contentRepository, never()).save(any(Content.class));
    }

    @Test
    @DisplayName("리그 하나 실패해도 나머지 계속 수집")
    void givenFirstLeagueFails_whenExecute_thenContinuesNextLeague() throws Exception {
        // given
        SportsdbEvent event = new SportsdbEvent(
                "456", "Barcelona vs Madrid", "Soccer",
                "Spanish La Liga", "1",
                "https://thumb.jpg", "filename", "Camp Nou", "2025-09-01"
        );

        Content content = Content.builder()
                .title("Barcelona vs Madrid")
                .externalId("456")
                .contentType(ContentType.sport)
                .build();

        // 프리미어 리그 실패
        when(sportsdbApiClient.fetchDayEvents(ExternalApiConstants.PREMIER_LEAGUE_ID))
                .thenThrow(new ApiEmptyResponseException());

        // 라리가 성공
        when(sportsdbApiClient.fetchDayEvents(ExternalApiConstants.LA_LIGA_ID))
                .thenReturn(List.of(event));
        when(sportsdbContentMapper.sportToContent(event)).thenReturn(content);
        when(contentRepository.existsByExternalIdAndContentType(anyString(), any())).thenReturn(false);
        when(meterRegistry.counter(anyString())).thenReturn(counter);

        // when
        RepeatStatus status = tasklet.execute(contribution, chunkContext);

        // then
        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        verify(contentRepository, times(1)).saveAndFlush(any(Content.class));
    }
}