package com.mopl.mopl.domain.content.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.domain.content.dto.request.ContentUpdateRequest;
import com.mopl.mopl.domain.content.dto.response.ContentDto;
import com.mopl.mopl.domain.content.dto.response.CursorResponseContentDto;
import com.mopl.mopl.domain.content.entity.ContentType;
import com.mopl.mopl.domain.content.exception.ContentNotFoundException;
import com.mopl.mopl.domain.content.service.ContentService;
import com.mopl.mopl.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContentController 단위 테스트")
class ContentControllerTest {

    @InjectMocks
    private ContentController contentController;

    @Mock
    private ContentService contentService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UUID contentId;
    private ContentDto sampleContentDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(contentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        contentId = UUID.randomUUID();
        sampleContentDto = new ContentDto(
                contentId,
                "테스트 영화",
                "영화 설명입니다",
                ContentType.movie,
                "thumb.jpg",
                List.of("action", "drama"),
                4.5,
                10,
                100
        );
    }

    @Nested
    @DisplayName("getContent - 콘텐츠 단건 조회")
    class GetContent {

        @Test
        @DisplayName("존재하는 콘텐츠 ID로 조회하면 200과 ContentDto를 반환한다.")
        void givenExistingContentId_whenGetContent_thenReturns200WithContentDto() throws Exception {
            given(contentService.getContent(contentId)).willReturn(sampleContentDto);

            mockMvc.perform(get("/api/contents/{contentId}", contentId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(contentId.toString()))
                    .andExpect(jsonPath("$.title").value("테스트 영화"))
                    .andExpect(jsonPath("$.type").value("movie"));
        }

        @Test
        @DisplayName("존재하지 않는 콘텐츠 ID로 조회하면 404를 반환한다.")
        void givenNonExistingContentId_whenGetContent_thenReturns404() throws Exception {
            given(contentService.getContent(contentId)).willThrow(new ContentNotFoundException(contentId));

            mockMvc.perform(get("/api/contents/{contentId}", contentId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("getContents - 콘텐츠 목록 조회")
    class GetContents {

        @Test
        @DisplayName("유효한 파라미터로 목록 조회 시 200과 CursorResponseContentDto를 반환한다.")
        void givenValidParams_whenGetContents_thenReturns200() throws Exception {
            CursorResponseContentDto responseDto = new CursorResponseContentDto(
                    List.of(sampleContentDto),
                    null,
                    null,
                    false,
                    1L,
                    "watcherCount",
                    "DESCENDING"
            );

            given(contentService.getContents(any())).willReturn(responseDto);

            mockMvc.perform(get("/api/contents")
                            .param("limit", "10")
                            .param("sortDirection", "DESCENDING")
                            .param("sortBy", "watcherCount"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].title").value("테스트 영화"))
                    .andExpect(jsonPath("$.hasNext").value(false))
                    .andExpect(jsonPath("$.totalCount").value(1));
        }

        @Test
        @DisplayName("다음 페이지가 있으면 nextCursor와 nextIdAfter를 반환한다.")
        void givenSliceWithNextPage_whenGetContents_thenReturnsNextCursor() throws Exception {
            UUID nextId = UUID.randomUUID();
            CursorResponseContentDto responseDto = new CursorResponseContentDto(
                    List.of(sampleContentDto),
                    "100",
                    nextId,
                    true,
                    50L,
                    "watcherCount",
                    "DESCENDING"
            );

            given(contentService.getContents(any())).willReturn(responseDto);

            mockMvc.perform(get("/api/contents")
                            .param("limit", "1")
                            .param("sortDirection", "DESCENDING")
                            .param("sortBy", "watcherCount"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasNext").value(true))
                    .andExpect(jsonPath("$.nextCursor").value("100"))
                    .andExpect(jsonPath("$.nextIdAfter").value(nextId.toString()));
        }
    }

    @Nested
    @DisplayName("updateContent - 콘텐츠 수정")
    class UpdateContent {

        @Test
        @DisplayName("유효한 요청으로 콘텐츠 수정 시 200과 수정된 ContentDto를 반환한다.")
        void givenValidRequest_whenUpdateContent_thenReturns200WithUpdatedDto() throws Exception {
            ContentUpdateRequest updateRequest = new ContentUpdateRequest(
                    "수정된 제목", "수정된 설명", List.of("newTag")
            );

            ContentDto updatedDto = new ContentDto(
                    contentId, "수정된 제목", "수정된 설명", ContentType.movie,
                    "thumb.jpg", List.of("newTag"), 4.5, 10, 100
            );

            given(contentService.update(eq(contentId), any(ContentUpdateRequest.class))).willReturn(updatedDto);

            mockMvc.perform(patch("/api/contents/{contentId}", contentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("수정된 제목"))
                    .andExpect(jsonPath("$.description").value("수정된 설명"));
        }

        @Test
        @DisplayName("존재하지 않는 콘텐츠 수정 시 404를 반환한다.")
        void givenNonExistingContentId_whenUpdateContent_thenReturns404() throws Exception {
            ContentUpdateRequest updateRequest = new ContentUpdateRequest(
                    "제목", "설명", List.of("tag")
            );

            given(contentService.update(eq(contentId), any(ContentUpdateRequest.class)))
                    .willThrow(new ContentNotFoundException(contentId));

            mockMvc.perform(patch("/api/contents/{contentId}", contentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("title이 비어있으면 400을 반환한다. (validation)")
        void givenBlankTitle_whenUpdateContent_thenReturns400() throws Exception {
            ContentUpdateRequest updateRequest = new ContentUpdateRequest(
                    "", "설명", List.of("tag")
            );

            mockMvc.perform(patch("/api/contents/{contentId}", contentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("deleteContent - 콘텐츠 삭제")
    class DeleteContent {

        @Test
        @DisplayName("존재하는 콘텐츠 삭제 시 204를 반환한다.")
        void givenExistingContentId_whenDeleteContent_thenReturns204() throws Exception {
            willDoNothing().given(contentService).delete(contentId);

            mockMvc.perform(delete("/api/contents/{contentId}", contentId))
                    .andExpect(status().isNoContent());

            verify(contentService).delete(contentId);
        }

        @Test
        @DisplayName("존재하지 않는 콘텐츠 삭제 시 404를 반환한다.")
        void givenNonExistingContentId_whenDeleteContent_thenReturns404() throws Exception {
            willThrow(new ContentNotFoundException(contentId)).given(contentService).delete(contentId);

            mockMvc.perform(delete("/api/contents/{contentId}", contentId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("createContent - 콘텐츠 생성")
    class CreateContent {

        @Test
        @DisplayName("유효한 멀티파트 요청으로 콘텐츠 생성 시 201을 반환한다.")
        void givenValidMultipartRequest_whenCreateContent_thenReturns201() throws Exception {
            given(contentService.create(any(), any())).willReturn(sampleContentDto);

            String requestJson = objectMapper.writeValueAsString(
                    new com.mopl.mopl.domain.content.dto.request.ContentCreateRequest(
                            "테스트 영화", "영화 설명입니다", "movie", List.of("action")
                    )
            );

            MockMultipartFile requestPart = new MockMultipartFile(
                    "request", "request.json", MediaType.APPLICATION_JSON_VALUE,
                    requestJson.getBytes()
            );

            MockMultipartFile thumbnailFile = new MockMultipartFile(
                    "thumbnail", "thumb.jpg", "image/jpeg", new byte[]{1, 2, 3}
            );

            mockMvc.perform(multipart("/api/contents")
                            .file(requestPart)
                            .file(thumbnailFile)
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("테스트 영화"));
        }
    }
}