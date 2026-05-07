package com.mopl.mopl.domain.content.service;

import com.mopl.mopl.domain.content.dto.request.ContentCreateRequest;
import com.mopl.mopl.domain.content.dto.request.ContentUpdateRequest;
import com.mopl.mopl.domain.content.dto.response.ContentDto;
import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.entity.ContentType;
import com.mopl.mopl.domain.content.exception.ContentNotFoundException;
import com.mopl.mopl.domain.content.mapper.ContentMapper;
import com.mopl.mopl.domain.content.repository.ContentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContentService Unit Test")
class ContentServiceTest
{
    @InjectMocks private ContentServiceImpl contentService;
    @Mock private ContentRepository contentRepository;
    @Mock private ContentMapper contentMapper;

    private UUID contentId;
    private Content content;

    @BeforeEach
    void setUp() {
        this.contentId = UUID.randomUUID();

        content = Content.builder()
                .title("test content")
                .description("test description")
                .contentType(ContentType.movie)
                .tags(List.of("tag1", "tag2"))
                .releaseDate(null)
                .thumbnailKey(null)
                .build();

        ReflectionTestUtils.setField(content, "id", contentId);
    }

    @Nested
    @DisplayName("콘텐츠 생성")
    class Create {
        @Test
        @DisplayName("콘텐츠를 정상적으로 생성한다.")
        void givenValidRequest_whenCreate_thenSuccess() {
            // given
            ContentCreateRequest contentCreateRequest = new ContentCreateRequest(
                    "test content",
                    "test description",
                    "movie",
                    List.of("tag1")
            );

            ContentDto contentDto = new ContentDto(
                    contentId,
                    "test content",
                    "test description",
                    ContentType.movie,
                    null,
                    List.of("tage1"),
                    0,
                    0,
                    0
            );

            given(contentRepository.save(any(Content.class))).willReturn(content);
            given(contentMapper.toContentDto(content)).willReturn(contentDto);

            // when
            ContentDto result = contentService.create(contentCreateRequest, null);

            // then
            assertThat(result.title()).isEqualTo("test content");
            assertThat(result.description()).isEqualTo("test description");
            assertThat(result.contentType()).isEqualTo(ContentType.movie);

            then(contentRepository).should().save(any(Content.class));
            then(contentMapper).should().toContentDto(content);
        }
    }

    @Nested
    @DisplayName("콘텐츠 단건 조회")
    class Read {
        @Test
        @DisplayName("존재하는 콘텐츠 ID로 조회하면 콘텐츠를 반환한다.")
        void givenExistingContentId_whenGetById_thenReturnsContent() {
            // given
            ContentDto contentDto = new ContentDto(
                    contentId,
                    "test content",
                    "test description",
                    ContentType.movie,
                    null,
                    List.of("tage1"),
                    0,
                    0,
                    0
            );

            given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
            given(contentMapper.toContentDto(content)).willReturn(contentDto);

            // when
            ContentDto result = contentService.getContent(contentId);
            
            // then
            assertThat(result.title()).isEqualTo("test content");
            assertThat(result.description()).isEqualTo("test description");

            then(contentRepository).should().findById(contentId);
            then(contentMapper).should().toContentDto(content);
        }
        
        @Test
        @DisplayName("존재하지 않는 콘텐츠 ID로 조회하면 예외가 발생한다.")
        void givenNonExistingContentId_whenGetById_thenThrowsContentNotFoundException() {
            // given
            given(contentRepository.findById(contentId)).willReturn(Optional.empty());
            
            // when & then
            assertThatThrownBy(() -> contentService.getContent(contentId))
                    .isInstanceOf(ContentNotFoundException.class);
        }
    }
    
    @Nested
    @DisplayName("콘텐츠 수정")
    class Update {
        @Test
        @DisplayName("존재하는 콘텐츠 ID로 수정하면 수정된 콘텐츠를 반환한다.")
        void givenExistingContentId_whenUpdate_thenSuccess() {
            // given
            ContentUpdateRequest contentUpdateRequest = new ContentUpdateRequest(
                    "test content",
                    "test description",
                    List.of("tag1")
            );

            ContentDto contentDto = new ContentDto(
                    contentId,
                    "test content",
                    "test description",
                    ContentType.movie,
                    null,
                    List.of("tag1"),
                    0,
                    0,
                    0
            );

            given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
            given(contentRepository.save(any(Content.class))).willReturn(content);
            given(contentMapper.toContentDto(content)).willReturn(contentDto);
            
            // when
            ContentDto result = contentService.update(contentId, contentUpdateRequest);
            
            // then
            assertThat(result.title()).isEqualTo("test content");
            assertThat(result.description()).isEqualTo("test description");

            then(contentRepository).should().findById(contentId);
            then(contentMapper).should().toContentDto(content);
        }
        
        @Test
        @DisplayName("존재하지 않는 콘텐츠 ID로 수정하면 예외가 발생한다.")
        void givenNonExistingContentId_whenUpdate_thenThrowsContentNotFoundException() {
            // given
            ContentUpdateRequest contentUpdateRequest = new ContentUpdateRequest(
                    "test content",
                    "test description",
                    List.of("tag1")
            );

            given(contentRepository.findById(contentId)).willReturn(Optional.empty());
            
            // when & then
            assertThatThrownBy(() -> contentService.update(contentId, contentUpdateRequest))
                    .isInstanceOf(ContentNotFoundException.class);
        }
    }
    
    @Nested
    @DisplayName("콘텐츠 삭제")
    class Delete {
        @Test
        @DisplayName("존재하는 콘텐츠 ID로 콘텐츠 삭제에 성공한다.")
        void givenExistingContentId_whenDelete_thenSuccess() {
            // given
            given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
            
            // when
            contentService.delete(contentId);
            
            // then
            then(contentRepository).should().delete(content);
        }
        
        @Test
        @DisplayName("존재하지 않는 콘텐츠 ID로 콘텐츠 삭제시 예외가 발생한다.")
        void givenNonExistingContentId_whenDelete_thenThrowsContentNotFoundException() {
            // given
            given(contentRepository.findById(contentId)).willReturn(Optional.empty());
            
            // when & then
            assertThatThrownBy(() -> contentService.delete(contentId))
                    .isInstanceOf(ContentNotFoundException.class);
        }
    }
}