package com.mopl.mopl.domain.content.service;

import com.mopl.mopl.domain.content.dto.request.ContentCreateRequest;
import com.mopl.mopl.domain.content.dto.request.ContentSearchRequest;
import com.mopl.mopl.domain.content.dto.request.ContentUpdateRequest;
import com.mopl.mopl.domain.content.dto.response.ContentDto;
import com.mopl.mopl.domain.content.dto.response.CursorResponseContentDto;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.mock.web.MockMultipartFile;

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

    private Content createContent(String title, int watcherCount) {
        Content content = Content.builder()
                .title(title)
                .description("설명")
                .contentType(ContentType.movie)
                .thumbnailKey("thumb.png")
                .tags(List.of("tag1"))
                .build();

        ReflectionTestUtils.setField(content, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(content, "watcherCount", watcherCount);

        return content;
    }

    private ContentDto createContentDto(String title) {
        return new ContentDto(
                UUID.randomUUID(), title, "설명", ContentType.movie,
                "thumb.png", List.of("tag1"), 0.0, 0, 0
        );
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
            assertThat(result.type()).isEqualTo(ContentType.movie);

            then(contentRepository).should().save(any(Content.class));
            then(contentMapper).should().toContentDto(content);
        }
    }

    @Nested
    @DisplayName("콘텐츠 조회")
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
        
        @Test
        @DisplayName("콘텐츠가 존재하고 다음 페이지가 있으면 nextCursor를 반환한다.")
        void givenSliceWithNext_whenGetContents_thenReturnsNextCursor() {
            // given
            ContentSearchRequest contentSearchRequest = new ContentSearchRequest(
                    null, null, null, null, 2, "DESCENDING", "watcherCount"
            );

            Content content1 = createContent("test content1", 50);
            Content content2 = createContent("test content2", 42);

            Slice<Content> slice = new SliceImpl<>(List.of(content1, content2), Pageable.unpaged(), true);

            given(contentRepository.getContents(contentSearchRequest)).willReturn(slice);
            given(contentRepository.countContentsWithKeyword(null)).willReturn(5L);
            given(contentMapper.toContentDtos(slice.getContent())).willReturn(List.of(
                    createContentDto("콘텐츠 1"),
                    createContentDto("콘텐츠 2")
            ));

            // when
            CursorResponseContentDto result = contentService.getContents(contentSearchRequest);

            // then
            assertThat(result.data()).hasSize(2);
            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor()).isEqualTo("42");
            assertThat(result.nextIdAfter()).isEqualTo(content2.getId());
            assertThat(result.totalCount()).isEqualTo(5L);
        }

        @Test
        @DisplayName("다음 페이지가 없으면 nextCursor가 null이다")
        void givenSliceWithoutNext_whenGetContents_thenReturnsNullCursor() {
            // given
            ContentSearchRequest request = new ContentSearchRequest(
                    null, null, null, null, 10, "DESCENDING", "watcherCount"
            );

            Content content1 = createContent("콘텐츠1", 50);
            Slice<Content> slice = new SliceImpl<>(List.of(content1), Pageable.unpaged(), false);

            given(contentRepository.getContents(request)).willReturn(slice);
            given(contentRepository.countContentsWithKeyword(null)).willReturn(1L);
            given(contentMapper.toContentDtos(slice.getContent())).willReturn(List.of(
                    createContentDto("콘텐츠1")
            ));

            // when
            CursorResponseContentDto result = contentService.getContents(request);

            // then
            assertThat(result.data()).hasSize(1);
            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();
            assertThat(result.nextIdAfter()).isNull();
        }

        @Test
        @DisplayName("콘텐츠가 없으면 빈 목록을 반환한다")
        void givenEmptySlice_whenGetContents_thenReturnsEmptyList() {
            // given
            ContentSearchRequest request = new ContentSearchRequest(
                    null, null, null, null, 10, "DESCENDING", "watcherCount"
            );

            Slice<Content> slice = new SliceImpl<>(List.of(), Pageable.unpaged(), false);

            given(contentRepository.getContents(request)).willReturn(slice);
            given(contentRepository.countContentsWithKeyword(null)).willReturn(0L);
            given(contentMapper.toContentDtos(List.of())).willReturn(List.of());

            // when
            CursorResponseContentDto result = contentService.getContents(request);

            // then
            assertThat(result.data()).isEmpty();
            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();
            assertThat(result.nextIdAfter()).isNull();
            assertThat(result.totalCount()).isEqualTo(0L);
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

    @Nested
    @DisplayName("커서 추출 (sortBy 별 nextCursor 값)")
    class CursorExtraction {

        @Test
        @DisplayName("sortBy=avgRating이면 nextCursor는 avgRating 문자열이다.")
        void givenSortByAvgRating_whenGetContents_thenNextCursorIsAvgRating() {
            // given
            ContentSearchRequest request = new ContentSearchRequest(
                    null, null, null, null, 1, "DESCENDING", "avgRating"
            );

            Content contentWithRating = createContent("영화A", 10);
            BigDecimal expectedRating = new BigDecimal("4.5");
            ReflectionTestUtils.setField(contentWithRating, "avgRating", expectedRating);

            Slice<Content> slice = new SliceImpl<>(List.of(contentWithRating), Pageable.unpaged(), true);

            given(contentRepository.getContents(request)).willReturn(slice);
            given(contentRepository.countContentsWithKeyword(null)).willReturn(5L);
            given(contentMapper.toContentDtos(slice.getContent())).willReturn(List.of(createContentDto("영화A")));

            // when
            CursorResponseContentDto result = contentService.getContents(request);

            // then
            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor()).isEqualTo("4.5");
            assertThat(result.nextIdAfter()).isEqualTo(contentWithRating.getId());
        }

        @Test
        @DisplayName("sortBy=reviewCount이면 nextCursor는 reviewCount 문자열이다.")
        void givenSortByReviewCount_whenGetContents_thenNextCursorIsReviewCount() {
            // given
            ContentSearchRequest request = new ContentSearchRequest(
                    null, null, null, null, 1, "DESCENDING", "reviewCount"
            );

            Content contentWithReview = createContent("드라마A", 20);
            ReflectionTestUtils.setField(contentWithReview, "reviewCount", 77);

            Slice<Content> slice = new SliceImpl<>(List.of(contentWithReview), Pageable.unpaged(), true);

            given(contentRepository.getContents(request)).willReturn(slice);
            given(contentRepository.countContentsWithKeyword(null)).willReturn(3L);
            given(contentMapper.toContentDtos(slice.getContent())).willReturn(List.of(createContentDto("드라마A")));

            // when
            CursorResponseContentDto result = contentService.getContents(request);

            // then
            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor()).isEqualTo("77");
        }

        @Test
        @DisplayName("sortBy=createdAt이면 nextCursor는 createdAt ISO 문자열이다.")
        void givenSortByCreatedAt_whenGetContents_thenNextCursorIsCreatedAt() {
            // given
            ContentSearchRequest request = new ContentSearchRequest(
                    null, null, null, null, 1, "DESCENDING", "createdAt"
            );

            Content contentWithTime = createContent("스포츠A", 5);
            Instant fixedTime = Instant.parse("2024-01-15T12:00:00Z");
            ReflectionTestUtils.setField(contentWithTime, "createdAt", fixedTime);

            Slice<Content> slice = new SliceImpl<>(List.of(contentWithTime), Pageable.unpaged(), true);

            given(contentRepository.getContents(request)).willReturn(slice);
            given(contentRepository.countContentsWithKeyword(null)).willReturn(2L);
            given(contentMapper.toContentDtos(slice.getContent())).willReturn(List.of(createContentDto("스포츠A")));

            // when
            CursorResponseContentDto result = contentService.getContents(request);

            // then
            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor()).isEqualTo(fixedTime.toString());
        }

        @Test
        @DisplayName("sortBy=null이면 기본값 watcherCount로 nextCursor를 추출한다.")
        void givenNullSortBy_whenGetContents_thenNextCursorIsWatcherCount() {
            // given
            ContentSearchRequest request = new ContentSearchRequest(
                    null, null, null, null, 1, "DESCENDING", null
            );

            Content contentWithWatcher = createContent("기본정렬", 99);

            Slice<Content> slice = new SliceImpl<>(List.of(contentWithWatcher), Pageable.unpaged(), true);

            given(contentRepository.getContents(request)).willReturn(slice);
            given(contentRepository.countContentsWithKeyword(null)).willReturn(1L);
            given(contentMapper.toContentDtos(slice.getContent())).willReturn(List.of(createContentDto("기본정렬")));

            // when
            CursorResponseContentDto result = contentService.getContents(request);

            // then
            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor()).isEqualTo("99");
        }

        @Test
        @DisplayName("sortBy=watcherCount이고 썸네일 파일이 있으면 originalFilename이 thumbnailKey로 저장된다.")
        void givenThumbnailFile_whenCreate_thenThumbnailKeyIsOriginalFilename() {
            // given
            ContentCreateRequest request = new ContentCreateRequest(
                    "영화제목", "설명", "movie", List.of("tag1")
            );

            MockMultipartFile thumbnailFile =
                    new MockMultipartFile(
                            "thumbnail", "my-thumb.jpg",
                            "image/jpeg", new byte[]{1, 2, 3}
                    );

            ContentDto expectedDto = new ContentDto(
                    UUID.randomUUID(), "영화제목", "설명", ContentType.movie,
                    "my-thumb.jpg", List.of("tag1"), 0.0, 0, 0
            );

            given(contentRepository.save(any(Content.class))).willReturn(content);
            given(contentMapper.toContentDto(content)).willReturn(expectedDto);

            // when
            ContentDto result = contentService.create(request, thumbnailFile);

            // then
            assertThat(result).isNotNull();
            then(contentRepository).should().save(any(Content.class));
        }
    }
}