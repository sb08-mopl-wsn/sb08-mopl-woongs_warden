package com.mopl.mopl.domain.content.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Content 엔티티 단위 테스트")
class ContentEntityTest {

    private Content content;

    @BeforeEach
    void setUp() {
        content = Content.builder()
                .title("original title")
                .description("original description")
                .contentType(ContentType.movie)
                .thumbnailKey("thumb.png")
                .tags(new ArrayList<>(List.of("tag1", "tag2")))
                .releaseDate(null)
                .build();
    }

    @Nested
    @DisplayName("update() - 콘텐츠 정보 수정")
    class Update {

        @Test
        @DisplayName("title, description, tags 모두 null이 아니면 모두 업데이트된다.")
        void givenAllNonNull_whenUpdate_thenAllFieldsUpdated() {
            List<String> newTags = List.of("newTag");

            content.update("new title", "new description", newTags);

            assertThat(content.getTitle()).isEqualTo("new title");
            assertThat(content.getDescription()).isEqualTo("new description");
            assertThat(content.getTags()).containsExactly("newTag");
        }

        @Test
        @DisplayName("title이 null이면 기존 title을 유지한다.")
        void givenNullTitle_whenUpdate_thenTitleUnchanged() {
            content.update(null, "new description", List.of("tag3"));

            assertThat(content.getTitle()).isEqualTo("original title");
            assertThat(content.getDescription()).isEqualTo("new description");
        }

        @Test
        @DisplayName("description이 null이면 기존 description을 유지한다.")
        void givenNullDescription_whenUpdate_thenDescriptionUnchanged() {
            content.update("new title", null, List.of("tag3"));

            assertThat(content.getDescription()).isEqualTo("original description");
            assertThat(content.getTitle()).isEqualTo("new title");
        }

        @Test
        @DisplayName("tags가 null이면 기존 tags를 유지한다.")
        void givenNullTags_whenUpdate_thenTagsUnchanged() {
            content.update("new title", "new description", null);

            assertThat(content.getTags()).containsExactly("tag1", "tag2");
        }

        @Test
        @DisplayName("모든 파라미터가 null이면 아무것도 변경되지 않는다.")
        void givenAllNull_whenUpdate_thenNothingChanged() {
            content.update(null, null, null);

            assertThat(content.getTitle()).isEqualTo("original title");
            assertThat(content.getDescription()).isEqualTo("original description");
            assertThat(content.getTags()).containsExactly("tag1", "tag2");
        }

        @Test
        @DisplayName("빈 태그 리스트로 업데이트하면 태그가 비워진다.")
        void givenEmptyTags_whenUpdate_thenTagsCleared() {
            content.update(null, null, List.of());

            assertThat(content.getTags()).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateReviewStats() - 리뷰 통계 업데이트")
    class UpdateReviewStats {

        @Test
        @DisplayName("유효한 avgRating과 reviewCount로 업데이트에 성공한다.")
        void givenValidStats_whenUpdateReviewStats_thenSuccess() {
            BigDecimal avgRating = new BigDecimal("4.5");
            int reviewCount = 10;

            content.updateReviewStats(avgRating, reviewCount);

            assertThat(content.getAvgRating()).isEqualByComparingTo(new BigDecimal("4.5"));
            assertThat(content.getReviewCount()).isEqualTo(10);
        }

        @Test
        @DisplayName("avgRating이 0.0이면 업데이트에 성공한다. (경계값)")
        void givenZeroAvgRating_whenUpdateReviewStats_thenSuccess() {
            content.updateReviewStats(BigDecimal.ZERO, 0);

            assertThat(content.getAvgRating()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(content.getReviewCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("avgRating이 5.0이면 업데이트에 성공한다. (경계값)")
        void givenMaxAvgRating_whenUpdateReviewStats_thenSuccess() {
            content.updateReviewStats(new BigDecimal("5.0"), 100);

            assertThat(content.getAvgRating()).isEqualByComparingTo(new BigDecimal("5.0"));
            assertThat(content.getReviewCount()).isEqualTo(100);
        }

        @Test
        @DisplayName("avgRating이 null이면 IllegalArgumentException이 발생한다.")
        void givenNullAvgRating_whenUpdateReviewStats_thenThrowsIllegalArgumentException() {
            assertThatThrownBy(() -> content.updateReviewStats(null, 5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null");
        }

        @Test
        @DisplayName("reviewCount가 음수이면 IllegalArgumentException이 발생한다.")
        void givenNegativeReviewCount_whenUpdateReviewStats_thenThrowsIllegalArgumentException() {
            assertThatThrownBy(() -> content.updateReviewStats(new BigDecimal("3.0"), -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("0 이상");
        }

        @Test
        @DisplayName("avgRating이 0.0 미만이면 IllegalArgumentException이 발생한다.")
        void givenNegativeAvgRating_whenUpdateReviewStats_thenThrowsIllegalArgumentException() {
            assertThatThrownBy(() -> content.updateReviewStats(new BigDecimal("-0.1"), 5))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("avgRating이 5.0 초과이면 IllegalArgumentException이 발생한다.")
        void givenAvgRatingOverMax_whenUpdateReviewStats_thenThrowsIllegalArgumentException() {
            assertThatThrownBy(() -> content.updateReviewStats(new BigDecimal("5.1"), 5))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("updateWatcherCount() - 시청자 수 업데이트")
    class UpdateWatcherCount {

        @Test
        @DisplayName("양수 watcherCount로 업데이트에 성공한다.")
        void givenPositiveWatcherCount_whenUpdateWatcherCount_thenSuccess() {
            content.updateWatcherCount(500);

            assertThat(content.getWatcherCount()).isEqualTo(500);
        }

        @Test
        @DisplayName("0으로 watcherCount 업데이트에 성공한다. (경계값)")
        void givenZeroWatcherCount_whenUpdateWatcherCount_thenSuccess() {
            ReflectionTestUtils.setField(content, "watcherCount", 100);

            content.updateWatcherCount(0);

            assertThat(content.getWatcherCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("음수 watcherCount이면 IllegalArgumentException이 발생한다.")
        void givenNegativeWatcherCount_whenUpdateWatcherCount_thenThrowsIllegalArgumentException() {
            assertThatThrownBy(() -> content.updateWatcherCount(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("0 이상");
        }

        @Test
        @DisplayName("watcherCount를 여러 번 업데이트하면 마지막 값으로 설정된다.")
        void givenMultipleUpdates_whenUpdateWatcherCount_thenLastValueSet() {
            content.updateWatcherCount(100);
            content.updateWatcherCount(200);

            assertThat(content.getWatcherCount()).isEqualTo(200);
        }
    }
}