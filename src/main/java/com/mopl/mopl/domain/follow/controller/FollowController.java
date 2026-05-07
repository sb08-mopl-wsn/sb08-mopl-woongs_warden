package com.mopl.mopl.domain.follow.controller;

import com.mopl.mopl.domain.follow.dto.FollowDto;
import com.mopl.mopl.domain.follow.dto.FollowRequest;
import com.mopl.mopl.domain.follow.service.FollowService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class FollowController {

  private final FollowService followService;

  // TODO: 인증 연동 시 헤더 지우고 @AuthenticationPrincipal 사용

  @PostMapping
  public ResponseEntity<FollowDto> follow(
    @RequestHeader(value = "X-Temp-User-Id", required = true)UUID followerId, // 임시 헤더
    @Valid @RequestBody FollowRequest request
  ) {
    FollowDto response = followService.follow(followerId, request);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{followId}")
  public ResponseEntity<Void> unfollow(
      @RequestHeader(value = "X-Temp-User-Id", required = true) UUID followerId, // 임시 헤더
      @PathVariable("followId") UUID followId
  ) {
    followService.unfollow(followerId, followId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/followed-by-me")
  public ResponseEntity<Boolean> isFollowedByMe(
      @RequestHeader(value = "X-Temp-User-Id", required = true) UUID followerId, // 임시 헤더
      @RequestParam("followeeId") UUID followeeId
  ) {
    boolean isFollowed = followService.isFollowedByMe(followerId, followeeId);
    return ResponseEntity.ok(isFollowed);
  }

  @GetMapping("/count")
  public ResponseEntity<Long> getFollowerCount(
      @RequestParam("followeeId") UUID followeeId
  ) {
    long count = followService.getFollowerCount(followeeId);
    return ResponseEntity.ok(count);
  }
}
