package com.mopl.mopl.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.entity.ContentType;
import com.mopl.mopl.domain.content.repository.ContentRepository;
import com.mopl.mopl.domain.review.dto.request.ReviewCreateRequest;
import com.mopl.mopl.domain.review.dto.request.ReviewSearchRequest;
import com.mopl.mopl.domain.review.dto.request.ReviewUpdateRequest;
import com.mopl.mopl.domain.review.dto.response.CursorResponseReviewDto;
import com.mopl.mopl.domain.review.dto.response.ReviewDto;
import com.mopl.mopl.domain.review.dto.response.ReviewStatsDto;
import com.mopl.mopl.domain.review.entity.Review;
import com.mopl.mopl.domain.review.exception.ReviewErrorCode;
import com.mopl.mopl.domain.review.exception.ReviewException;
import com.mopl.mopl.domain.review.mapper.ReviewMapper;
import com.mopl.mopl.domain.review.repository.ReviewRepository;
import com.mopl.mopl.domain.review.service.impl.ReviewServiceImpl;
import com.mopl.mopl.domain.user.dto.UserSummary;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.event.ReviewCreatedEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService Unit Test")
class ReviewServiceImplTest {

  @InjectMocks private ReviewServiceImpl reviewService;
  @Mock private ReviewRepository reviewRepository;
  @Mock private ContentRepository contentRepository;
  @Mock private UserRepository userRepository;
  @Mock private ReviewMapper reviewMapper;
  @Mock private ApplicationEventPublisher eventPublisher;

  private UUID userId;
  private UUID contentId;
  private UUID reviewId;
  private User user;
  private Content content;
  private Review review;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    contentId = UUID.randomUUID();
    reviewId = UUID.randomUUID();

    user = User.builder()
        .name("test user")
        .email("test@test.com")
        .build();
    ReflectionTestUtils.setField(user, "id", userId);

    content = Content.builder()
        .title("test content")
        .contentType(ContentType.movie)
        .build();
    ReflectionTestUtils.setField(content, "id", contentId);

    review = Review.builder()
        .user(user)
        .content(content)
        .rating(4.0)
        .description("재밌네요!")
        .build();
    ReflectionTestUtils.setField(review, "id", reviewId);
  }

  @Nested
  @DisplayName("리뷰 생성")
  class Create {
    @Test
    @DisplayName("리뷰를 정상적으로 생성하고 평점을 업데이트한다.")
    void givenValidRequest_whenCreate_thenSuccess() {
      // given
      ReviewCreateRequest request = new ReviewCreateRequest(contentId, "재밌네요!", 4.0);
      ReviewDto reviewDto = new ReviewDto(reviewId, contentId, new UserSummary(userId, "test user", null), "재밌네요!", 4.0);
      ReviewStatsDto statsDto = new ReviewStatsDto(1L, 4.0);

      given(userRepository.findById(userId)).willReturn(Optional.of(user));
      given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
      given(reviewMapper.toEntity(request, user, content)).willReturn(review);
      given(reviewRepository.saveAndFlush(any(Review.class))).willReturn(review);

      // 통계 업데이트 Mocking
      given(contentRepository.findByIdForUpdate(contentId)).willReturn(Optional.of(content));
      given(reviewRepository.getReviewStats(contentId)).willReturn(statsDto);

      given(reviewMapper.toDto(review)).willReturn(reviewDto);

      // when
      ReviewDto result = reviewService.createReview(request, userId);

      // then
      assertThat(result.text()).isEqualTo("재밌네요!");
      assertThat(result.rating()).isEqualTo(4.0);

      // 검증
      then(contentRepository).should().findByIdForUpdate(contentId);
      then(reviewRepository).should().getReviewStats(contentId);
      then(eventPublisher).should().publishEvent(any(ReviewCreatedEvent.class));

      assertThat(content.getAvgRating()).isEqualByComparingTo(new BigDecimal("4.0"));
      assertThat(content.getReviewCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("이미 리뷰를 작성한 콘텐츠에 또 작성하면 예외가 발생한다.")
    void givenDuplicateReview_whenCreate_thenThrowsException() {
      // given
      ReviewCreateRequest request = new ReviewCreateRequest(contentId, "재밌네요!", 4.0);

      given(userRepository.findById(userId)).willReturn(Optional.of(user));
      given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
      given(reviewMapper.toEntity(request, user, content)).willReturn(review);

      given(reviewRepository.saveAndFlush(any(Review.class)))
          .willThrow(new DataIntegrityViolationException("duplicate review"));

      // when & then
      assertThatThrownBy(() -> reviewService.createReview(request, userId))
          .isInstanceOf(ReviewException.class)
          .hasMessageContaining(ReviewErrorCode.DUPLICATE_REVIEW.getMessage());
    }
  }

  @Nested @DisplayName("리뷰 조회") class Read {
    @Test
    @DisplayName("단건 조회: 존재하는 리뷰 ID로 조회하면 정상적으로 반환한다.")
    void givenExistingReviewId_whenFindById_thenSuccess() {
      // given
      ReviewDto reviewDto = new ReviewDto(reviewId, contentId, new UserSummary(userId, "test user", null), "재밌네요!", 4.0);

      given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
      given(reviewMapper.toDto(review)).willReturn(reviewDto);

      // when
      ReviewDto result = reviewService.findReviewById(reviewId);

      // then
      assertThat(result.text()).isEqualTo("재밌네요!");
      assertThat(result.rating()).isEqualTo(4.0);
      then(reviewRepository).should().findById(reviewId);
    }

    @Test
    @DisplayName("단건 조회: 존재하지 않는 리뷰 ID로 조회하면 예외가 발생한다.")
    void givenNonExistingReviewId_whenFindById_thenThrowsException() {
      // given
      given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> reviewService.findReviewById(reviewId))
          .isInstanceOf(ReviewException.class)
          .hasMessageContaining(ReviewErrorCode.REVIEW_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("다건 조회: 다음 페이지가 있는 경우 커서 정보를 포함하여 반환한다.")
    void givenSliceWithNext_whenFindReviews_thenReturnsCursor() {
      // given
      ReviewSearchRequest request = new ReviewSearchRequest(contentId, 10, "rating", "DESCENDING", null, null);
      ReviewDto reviewDto = new ReviewDto(reviewId, contentId, new UserSummary(userId, "test user", null), "재밌네요!", 4.0);

      Slice<Review> slice = new SliceImpl<>(
          List.of(review), Pageable.unpaged(), true);

      given(reviewRepository.findReviews(request)).willReturn(slice);
      given(reviewMapper.toDto(review)).willReturn(reviewDto);
      given(reviewRepository.countReviews(contentId)).willReturn(5L);

      // when
      CursorResponseReviewDto result = reviewService.findReviews(request);

      // then
      assertThat(result.data()).hasSize(1);
      assertThat(result.hasNext()).isTrue();
      assertThat(result.nextCursor()).isEqualTo("4.0");
      assertThat(result.totalCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("다건 조회: createdAt 기준으로 정렬 시 커서 정보를 포함하여 반환한다.")
    void givenSortByCreatedAt_whenFindReviews_thenReturnsCursor() {
      // given
      ReviewSearchRequest request = new ReviewSearchRequest(contentId, 10, "createdAt", "DESCENDING", null, null);
      ReviewDto reviewDto = new ReviewDto(reviewId, contentId, new UserSummary(userId, "test user", null), "재밌네요!", 4.0);

      // createdAt이 null이면 에러가 날 수 있으니, 테스트용 시간 설정해줌
      Instant fixedCreatedAt = Instant.parse("2026-05-19T10:00:00Z");
      ReflectionTestUtils.setField(review, "createdAt", fixedCreatedAt);

      Slice<Review> slice = new SliceImpl<>(List.of(review), Pageable.unpaged(), true);

      given(reviewRepository.findReviews(request)).willReturn(slice);
      given(reviewMapper.toDto(review)).willReturn(reviewDto);
      given(reviewRepository.countReviews(contentId)).willReturn(5L);

      // when
      CursorResponseReviewDto result = reviewService.findReviews(request);

      // then
      assertThat(result.hasNext()).isTrue();
      assertThat(result.nextCursor()).isEqualTo(fixedCreatedAt.toString()); // createdAt 커서 검증
    }
  }

  @Nested
  @DisplayName("리뷰 수정")
  class Update {
    @Test
    @DisplayName("자신의 리뷰를 정상적으로 수정하고 평점을 업데이트한다.")
    void givenValidRequest_whenUpdate_thenSuccess() {
      // given
      ReviewUpdateRequest request = new ReviewUpdateRequest("수정됨", 5.0);
      ReviewDto reviewDto = new ReviewDto(reviewId, contentId, new UserSummary(userId, "test user", null), "수정됨", 5.0);
      ReviewStatsDto statsDto = new ReviewStatsDto(1L, 5.0);

      given(userRepository.findById(userId)).willReturn(Optional.of(user));
      given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

      // 통계 업데이트 Mocking
      given(contentRepository.findByIdForUpdate(contentId)).willReturn(Optional.of(content));
      given(reviewRepository.getReviewStats(contentId)).willReturn(statsDto);

      given(reviewMapper.toDto(review)).willReturn(reviewDto);

      // when
      ReviewDto result = reviewService.updateReview(reviewId, request, userId);

      // then
      assertThat(result.text()).isEqualTo("수정됨");
      assertThat(result.rating()).isEqualTo(5.0);

      then(reviewRepository).should().flush();
      then(contentRepository).should().findByIdForUpdate(contentId);
      then(reviewRepository).should().getReviewStats(contentId);

      assertThat(content.getAvgRating()).isEqualByComparingTo(new BigDecimal("5.0"));
      assertThat(content.getReviewCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("다른 사람의 리뷰를 수정하려 하면 예외가 발생한다.")
    void givenOtherUser_whenUpdate_thenThrowsException() {
      // given
      ReviewUpdateRequest request = new ReviewUpdateRequest("수정됨", 5.0);
      User otherUser = User.builder().name("other").email("other@test.com").build();
      ReflectionTestUtils.setField(otherUser, "id", UUID.randomUUID());

      given(userRepository.findById(otherUser.getId())).willReturn(Optional.of(otherUser));
      given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

      // when & then
      assertThatThrownBy(() -> reviewService.updateReview(reviewId, request, otherUser.getId()))
          .isInstanceOf(ReviewException.class)
          .hasMessageContaining(ReviewErrorCode.REVIEW_FORBIDDEN.getMessage());
    }
  }

  @Nested
  @DisplayName("리뷰 삭제")
  class Delete {
    @Test
    @DisplayName("자신의 리뷰를 정상적으로 삭제하고 평점을 업데이트한다.")
    void givenValidRequest_whenDelete_thenSuccess() {
      // given
      ReviewStatsDto statsDto = new ReviewStatsDto(0L, 0.0);

      given(userRepository.findById(userId)).willReturn(Optional.of(user));
      given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

      // 통계 업데이트 Mocking
      given(contentRepository.findByIdForUpdate(contentId)).willReturn(Optional.of(content));
      given(reviewRepository.getReviewStats(contentId)).willReturn(statsDto);

      // when
      reviewService.deleteReview(reviewId, userId);

      // then
      then(reviewRepository).should().delete(review);
      then(reviewRepository).should().flush();
      then(contentRepository).should().findByIdForUpdate(contentId);
      then(reviewRepository).should().getReviewStats(contentId);

      assertThat(content.getAvgRating()).isEqualByComparingTo(new BigDecimal("0.0"));
      assertThat(content.getReviewCount()).isEqualTo(0);
    }
  }
}