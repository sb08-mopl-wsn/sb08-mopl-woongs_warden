package com.mopl.mopl.domain.review.controller;

import com.mopl.mopl.domain.review.dto.request.ReviewCreateRequest;
import com.mopl.mopl.domain.review.dto.request.ReviewSearchRequest;
import com.mopl.mopl.domain.review.dto.request.ReviewUpdateRequest;
import com.mopl.mopl.domain.review.dto.response.CursorResponseReviewDto;
import com.mopl.mopl.domain.review.dto.response.ReviewDto;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.UUID;

@Tag(name = "리뷰 관리", description = "리뷰 API")
public interface ReviewApi {

  /* 리뷰 생성 */
  @Operation(summary = "리뷰 생성", description = "생성한 리뷰는 API 요청자 본인의 리뷰로 생성됩니다.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "201",
          description = "성공",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ReviewDto.class)
          )
      ),
      @ApiResponse(responseCode = "200", description = "성공", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ReviewDto.class))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "401", description = "인증 오류", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(schema = @Schema(hidden = true)))
  })
  ResponseEntity<ReviewDto> createReview(
      @Parameter(description = "리뷰 생성 요청 데이터 (JSON)", required = true)
      @Valid @RequestBody ReviewCreateRequest request,
      @Parameter(hidden = true) @AuthenticationPrincipal MoplUserDetails userDetails
  );

  /* 리뷰 수정 */
  @Operation(summary = "리뷰 수정", description = "리뷰 작성자만 수정할 수 있습니다.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "성공",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ReviewDto.class)
          )
      ),
      @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "401", description = "인증 오류", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "403", description = "권한 오류", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(schema = @Schema(hidden = true)))
  })
  ResponseEntity<ReviewDto> updateReview(
      @Parameter(description = "리뷰 UUID", required = true)
      @PathVariable UUID reviewId,
      @Parameter(description = "리뷰 수정 요청 데이터 (JSON)", required = true)
      @Valid @RequestBody ReviewUpdateRequest request,
      @Parameter(hidden = true) @AuthenticationPrincipal MoplUserDetails userDetails
  );

  /* 리뷰 단건 조회 */
  @Operation(summary = "리뷰 단건 조회", description = "리뷰 ID로 특정 리뷰를 조회합니다.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "성공",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ReviewDto.class)
          )
      ),
      @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "401", description = "인증 오류", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "404", description = "해당 리소스 없음", content = @Content(schema = @Schema(hidden = true)))
  })
  ResponseEntity<ReviewDto> findReviewById(
      @Parameter(description = "리뷰 UUID", required = true)
      @PathVariable UUID reviewId
  );

  /* 리뷰 목록 조회 */
  @Operation(summary = "리뷰 목록 조회 (커서 페이지네이션)")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "성공",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = CursorResponseReviewDto.class)
          )
      ),
      @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "401", description = "인증 오류", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(schema = @Schema(hidden = true)))
  })
  ResponseEntity<CursorResponseReviewDto> findReviews(
      @ParameterObject @Valid ReviewSearchRequest request
  );

  /* 리뷰 삭제 */
  @Operation(summary = "리뷰 삭제", description = "리뷰 작성자만 삭제할 수 있습니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "성공", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "401", description = "인증 오류", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "403", description = "권한 오류", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(schema = @Schema(hidden = true)))
  })
  ResponseEntity<Void> deleteReview(
      @Parameter(description = "리뷰 UUID", required = true)
      @PathVariable UUID reviewId,
      @Parameter(hidden = true) @AuthenticationPrincipal MoplUserDetails userDetails
  );
}
