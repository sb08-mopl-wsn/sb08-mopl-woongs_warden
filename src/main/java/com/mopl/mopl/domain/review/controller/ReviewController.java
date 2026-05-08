package com.mopl.mopl.domain.review.controller;

import com.mopl.mopl.domain.review.dto.request.ReviewCreateRequest;
import com.mopl.mopl.domain.review.dto.request.ReviewUpdateRequest;
import com.mopl.mopl.domain.review.dto.response.ReviewDto;
import com.mopl.mopl.domain.review.service.ReviewService;
import com.mopl.mopl.domain.user.entity.User;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

  private final ReviewService reviewService;

  //생성
  @PostMapping
  public ResponseEntity<ReviewDto> createReview(
      @Valid @RequestBody ReviewCreateRequest request,
      @AuthenticationPrincipal User user // Spring Security가 현재 로그인한 사용자 정보를 주입해 줍니다.
  ) {
    // TODO: Service의 createReview 구현하면 정상작동 함
    ReviewDto responseDto = reviewService.createReview(request, user);
    return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
  }

  @PatchMapping("/{reviewId}")
  public ResponseEntity<ReviewDto> updateReview(
      @PathVariable UUID reviewId,
      @Valid @RequestBody ReviewUpdateRequest request,
      @AuthenticationPrincipal User user
  ) {
    ReviewDto responseDto = reviewService.updateReview(reviewId, request, user);
    return ResponseEntity.ok(responseDto);
  }

  @DeleteMapping("/{reviewId}")
  public ResponseEntity<Void> deleteReview(
      @PathVariable UUID reviewId,
      @AuthenticationPrincipal User user
  ) {
    reviewService.deleteReview(reviewId, user);
    return ResponseEntity.noContent().build();
  }
}