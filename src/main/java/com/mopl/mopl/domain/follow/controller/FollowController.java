package com.mopl.mopl.domain.follow.controller;

import com.mopl.mopl.domain.follow.dto.FollowDto;
import com.mopl.mopl.domain.follow.dto.FollowRequest;
import com.mopl.mopl.domain.follow.service.FollowService;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class FollowController {

  private final FollowService followService;

  @PostMapping
  public ResponseEntity<FollowDto> follow(
    @AuthenticationPrincipal(errorOnInvalidType = true) MoplUserDetails userDetails,
    @Valid @RequestBody FollowRequest request
  ) {
    UUID followerId = userDetails.getUserDto().id();
    FollowDto response = followService.follow(followerId, request);
    return ResponseEntity.status(201).body(response);
  }

  @DeleteMapping("/{followeeId}")
  public ResponseEntity<Void> unfollow(
      @AuthenticationPrincipal(errorOnInvalidType = true) MoplUserDetails userDetails,
      @PathVariable("followeeId") UUID followeeId
  ) {
    UUID followerId = userDetails.getUserDto().id();
    followService.unfollow(followerId, followeeId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/followed-by-me")
  public ResponseEntity<Boolean> isFollowedByMe(
      @AuthenticationPrincipal(errorOnInvalidType = true) MoplUserDetails userDetails,
      @RequestParam("followeeId") UUID followeeId
  ) {
    UUID followerId = userDetails.getUserDto().id();
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
