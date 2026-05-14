package com.mopl.mopl.domain.content.controller;

import com.mopl.mopl.domain.content.dto.request.ContentCreateRequest;
import com.mopl.mopl.domain.content.dto.request.ContentSearchRequest;
import com.mopl.mopl.domain.content.dto.request.ContentUpdateRequest;
import com.mopl.mopl.domain.content.dto.response.ContentDto;
import com.mopl.mopl.domain.content.dto.response.CursorResponseContentDto;
import com.mopl.mopl.domain.watchingSession.dto.request.WatchingSessionPageRequest;
import com.mopl.mopl.domain.watchingSession.dto.response.CursorResponseWatchingSessionDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Tag(name = "콘텐츠 관리", description = "콘텐츠 API")
public interface ContentApi
{
    /* 콘텐츠 생성 */
    @Operation(summary = "[어드민] 콘텐츠 생성", description = "썸네일 이미지와 함께 새로운 콘텐츠를 생성합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "콘텐츠 생성 성공",
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = ContentDto.class)
                    )
            )
    })
    ResponseEntity<ContentDto> createContent(
            @Parameter(description = "콘텐츠 생성 요청 데이터 (JSON)", required = true)
            @Valid @RequestPart("request")ContentCreateRequest contentCreateRequest,

            @Parameter(
                    description = "썸네일 이미지 파일",
                    required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "string", format = "binary"))
            )
            @RequestPart(value = "thumbnail") MultipartFile thumbnailImage
    );

    /* 콘텐츠 단건 조회 */
    @Operation(
            summary = "콘텐츠 단건 조회", description = "콘텐츠 ID로 특정 콘텐츠를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ContentDto.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "콘텐츠를 찾을 수 없음",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<ContentDto> getContent(
            @Parameter(description = "콘텐츠 UUID", required = true, example = "01932a4b-1234-7abc-8def-0123456789ab")
            @PathVariable UUID contentId
    );

    /* 콘텐츠 목록 조회 */
    @Operation(
            summary = "콘텐츠 목록 조회", description = "커서 기반 페이지네이션으로 콘텐츠 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CursorResponseContentDto.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<CursorResponseContentDto> getContents(
            @ParameterObject @Valid ContentSearchRequest contentSearchRequest
    );

    /* 콘텐츠 수정 */
    @Operation(
            summary = "[어드민] 콘텐츠 수정", description = "콘텐츠 정보와 썸네일 이미지를 수정합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ContentDto.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "콘텐츠를 찾을 수 없음",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<ContentDto> updateContent(
            @Parameter(description = "콘텐츠 UUID", required = true, example = "01932a4b-1234-7abc-8def-0123456789ab")
            @PathVariable UUID contentId,

            @Parameter(description = "콘텐츠 수정 요청 데이터 (JSON)", required = true)
            @Valid @RequestPart("request") ContentUpdateRequest contentUpdateRequest,

            @Parameter(
                    description = "썸네일 이미지 파일",
                    required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "string", format = "binary"))
            )
            @RequestPart(value = "thumbnail") MultipartFile thumbnailImage
    );

    /* 콘텐츠 삭제 */
    @Operation(
            summary = "[어드민] 콘텐츠 삭제",
            description = "콘텐츠 ID로 콘텐츠를 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "콘텐츠를 찾을 수 없음",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<Void> deleteContent(
            @Parameter(description = "콘텐츠 UUID", required = true, example = "01932a4b-1234-7abc-8def-0123456789ab")
            @PathVariable UUID contentId
    );

    /* 콘첸츠별 시청 세션 목록 조회 */
    @Operation(
            summary = "콘텐츠별 시청 세션 목록 조회",
            description = "특정 콘텐츠에 대한 시청 세션 목록을 커서 기반으로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CursorResponseWatchingSessionDto.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "콘텐츠를 찾을 수 없음",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<CursorResponseWatchingSessionDto> findWatchingSession(
            @Parameter(description = "콘텐츠 UUID", required = true, example = "01932a4b-1234-7abc-8def-0123456789ab")
            @PathVariable UUID contentId,

            @ParameterObject @Valid @ModelAttribute WatchingSessionPageRequest request
    );
}
