package com.mopl.mopl.domain.review.controller;

import com.mopl.mopl.domain.review.dto.request.ReviewCreateRequest;
import com.mopl.mopl.domain.review.dto.request.ReviewSearchRequest;
import com.mopl.mopl.domain.review.dto.request.ReviewUpdateRequest;
import com.mopl.mopl.domain.review.dto.response.CursorResponseReviewDto;
import com.mopl.mopl.domain.review.dto.response.ReviewDto;
import com.mopl.mopl.domain.review.service.ReviewService;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
      @AuthenticationPrincipal MoplUserDetails userDetails
  ) {
    UUID userId = userDetails.getUserDto().id();
    ReviewDto responseDto = reviewService.createReview(request, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
  }

  @PatchMapping("/{reviewId}")
  public ResponseEntity<ReviewDto> updateReview(
      @PathVariable UUID reviewId,
      @Valid @RequestBody ReviewUpdateRequest request,
      @AuthenticationPrincipal MoplUserDetails userDetails
  ) {
    UUID userId = userDetails.getUserDto().id();
    ReviewDto responseDto = reviewService.updateReview(reviewId, request, userId);
    return ResponseEntity.ok(responseDto);
  }

  @GetMapping("/{reviewId}")
  public ResponseEntity<ReviewDto> findReviewById(@PathVariable UUID reviewId) {
    ReviewDto responseDto = reviewService.findReviewById(reviewId);
    return ResponseEntity.ok(responseDto);
  }

  @GetMapping
  public ResponseEntity<CursorResponseReviewDto> findReviews(
      @Valid ReviewSearchRequest request
  ) {
    CursorResponseReviewDto response = reviewService.findReviews(request);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{reviewId}")
  public ResponseEntity<Void> deleteReview(
      @PathVariable UUID reviewId,
      @AuthenticationPrincipal MoplUserDetails userDetails
  ) {
    UUID userId = userDetails.getUserDto().id();
    reviewService.deleteReview(reviewId, userId);
    return ResponseEntity.noContent().build();
  }
}