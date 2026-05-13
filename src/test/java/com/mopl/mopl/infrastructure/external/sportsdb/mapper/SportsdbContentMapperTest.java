package com.mopl.mopl.infrastructure.external.sportsdb.mapper;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.entity.ContentType;
import com.mopl.mopl.infrastructure.external.sportsdb.SportsdbApiClient;
import com.mopl.mopl.infrastructure.external.sportsdb.dto.SportsdbEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("SportsdbContentMapper 테스트")
class SportsdbContentMapperTest
{
    @Mock private SportsdbApiClient apiClient;
    private SportsdbContentMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new SportsdbContentMapper(apiClient);
    }

    @Test
    @DisplayName("스포츠 이벤트 변환 성공")
    void givenSportsdbEvent_whenSportToContent_thenReturnsContent() {
        SportsdbEvent event = new SportsdbEvent(
                "Liverpool vs Bournemouth",
                "1",
                "https://r2.thesportsdb.com/images/thumb.jpg",
                "English Premier League 2025-08-15 Liverpool vs Bournemouth",
                "Anfield",
                "English Premier League",
                "Soccer",
                "1234567",
                "2025-08-15"
        );

        Content content = mapper.sportToContent(event);

        assertThat(content.getTitle()).isEqualTo("Liverpool vs Bournemouth");
        assertThat(content.getDescription()).isEqualTo("English Premier League 2025-08-15 Liverpool vs Bournemouth");
        assertThat(content.getContentType()).isEqualTo(ContentType.sport);
        assertThat(content.getThumbnailKey()).isEqualTo("https://r2.thesportsdb.com/images/thumb.jpg");
        assertThat(content.getExternalId()).isEqualTo("1234567");
    }

    @Test
    @DisplayName("dateEvent가 null이면 releaseDate도 null")
    void givenNullDateEvent_whenSportToContent_thenReleaseDateIsNull() {
        SportsdbEvent event = new SportsdbEvent(
                "Test Match", "1", "https://thumb.jpg", "filename",
                "venue", "La Liga", "Soccer", "123", null
        );

        Content content = mapper.sportToContent(event);

        assertThat(content.getReleaseDate()).isNull();
    }

    @Test
    @DisplayName("strThumb가 null이면 thumbnailKey도 null")
    void givenNullThumb_whenSportToContent_thenThumbnailKeyIsNull() {
        SportsdbEvent event = new SportsdbEvent(
                "Test Match", "1", null, "filename",
                "venue", "La Liga", "Soccer", "456", "2025-09-01"
        );

        Content content = mapper.sportToContent(event);

        assertThat(content.getThumbnailKey()).isNull();
    }
}