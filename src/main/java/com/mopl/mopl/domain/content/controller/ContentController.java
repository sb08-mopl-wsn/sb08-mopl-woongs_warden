package com.mopl.mopl.domain.content.controller;

import com.mopl.mopl.domain.content.dto.request.ContentCreateRequest;
import com.mopl.mopl.domain.content.dto.request.ContentSearchRequest;
import com.mopl.mopl.domain.content.dto.request.ContentUpdateRequest;
import com.mopl.mopl.domain.content.dto.response.ContentDto;
import com.mopl.mopl.domain.content.dto.response.CursorResponseContentDto;
import com.mopl.mopl.domain.content.service.ContentService;
import com.mopl.mopl.domain.watchingSession.dto.request.WatchingSessionPageRequest;
import com.mopl.mopl.domain.watchingSession.dto.response.CursorResponseWatchingSessionDto;
import com.mopl.mopl.domain.watchingSession.service.WatchingSessionService;
import com.mopl.mopl.infrastructure.ai.ContentRecommendService;
import com.mopl.mopl.infrastructure.ai.dto.ContentRecommendRequest;
import com.mopl.mopl.infrastructure.ai.dto.ContentRecommendResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/contents")
public class ContentController implements ContentApi
{
    private final ContentService contentService;
    private final WatchingSessionService watchingSessionService;
    private final ContentRecommendService contentRecommendService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContentDto> createContent(@Valid @RequestPart("request") ContentCreateRequest contentCreateRequest,
                                                    @RequestPart(value = "thumbnail", required = true) MultipartFile thumbnailImage)
    {
        ContentDto contentDto = contentService.create(contentCreateRequest, thumbnailImage);

        return ResponseEntity.status(HttpStatus.CREATED).body(contentDto);
    }

    @GetMapping("/{contentId}")
    public ResponseEntity<ContentDto> getContent(@PathVariable UUID contentId)
    {
        ContentDto contentDto = contentService.getContent(contentId);

        return ResponseEntity.status(HttpStatus.OK).body(contentDto);
    }

    @GetMapping
    public ResponseEntity<CursorResponseContentDto> getContents(@Valid @ParameterObject ContentSearchRequest contentSearchRequest)
    {
        CursorResponseContentDto contents = contentService.getContents(contentSearchRequest);

        return ResponseEntity.status(HttpStatus.OK).body(contents);
    }

    @PatchMapping("/{contentId}")
    public ResponseEntity<ContentDto> updateContent(@PathVariable UUID contentId,
                                                    @Valid @RequestPart("request") ContentUpdateRequest contentUpdateRequest,
                                                    @RequestPart(value = "thumbnail", required = true) MultipartFile thumbnailImage)
    {
        ContentDto contentDto = contentService.update(contentId, contentUpdateRequest, thumbnailImage);

        return ResponseEntity.status(HttpStatus.OK).body(contentDto);
    }

    @DeleteMapping("/{contentId}")
    public ResponseEntity<Void> deleteContent(@PathVariable UUID contentId)
    {
        contentService.delete(contentId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/{contentId}/watching-sessions")
    public ResponseEntity<CursorResponseWatchingSessionDto> findWatchingSession(@PathVariable UUID contentId,
                                                                                @Valid @ModelAttribute WatchingSessionPageRequest request)
    {
        CursorResponseWatchingSessionDto sessionDto = watchingSessionService.findByContentInWatchingSession(contentId, request);

        return ResponseEntity.status(HttpStatus.OK).body(sessionDto);
    }

    @PostMapping("/recommend")
    public ResponseEntity<List<ContentRecommendResponse>> recommend(@RequestBody @Valid ContentRecommendRequest contentRecommendRequest)
    {
        return ResponseEntity.ok(contentRecommendService.recommend(contentRecommendRequest));
    }
}
