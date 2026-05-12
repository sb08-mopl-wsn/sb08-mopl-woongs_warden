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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/contents")
public class ContentController
{
    private final ContentService contentService;
    private final WatchingSessionService watchingSessionService;

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
                                                    @Valid @RequestBody ContentUpdateRequest contentUpdateRequest)
    {
        ContentDto contentDto = contentService.update(contentId, contentUpdateRequest);

        return ResponseEntity.status(HttpStatus.OK).body(contentDto);
    }

    @DeleteMapping("/{contentId}")
    public ResponseEntity<Void> deleteContent(@PathVariable UUID contentId)
    {
        contentService.delete(contentId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/{contentId}/watching-sessions")
    public ResponseEntity<CursorResponseWatchingSessionDto> findWatchingSession(
            @PathVariable UUID contentId,
            @Valid @ModelAttribute WatchingSessionPageRequest request
    ) {
        CursorResponseWatchingSessionDto sessionDto = watchingSessionService.findByContentInWatchingSession(contentId, request);
        return ResponseEntity.status(HttpStatus.OK).body(sessionDto);
    }
}
