package com.mopl.mopl.domain.content.entity;

import com.mopl.mopl.domain.content.exception.ContentInvalidTypeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ContentType 단위 테스트")
class ContentTypeTest {

    @Nested
    @DisplayName("from() - 문자열로부터 ContentType 변환")
    class From {

        @Test
        @DisplayName("'movie' 문자열을 ContentType.movie로 변환한다.")
        void givenMovieString_whenFrom_thenReturnsMovieType() {
            ContentType result = ContentType.from("movie");

            assertThat(result).isEqualTo(ContentType.movie);
        }

        @Test
        @DisplayName("'tvSeries' 문자열을 ContentType.tvSeries로 변환한다.")
        void givenTvSeriesString_whenFrom_thenReturnsTvSeriesType() {
            ContentType result = ContentType.from("tvSeries");

            assertThat(result).isEqualTo(ContentType.tvSeries);
        }

        @Test
        @DisplayName("'sport' 문자열을 ContentType.sport로 변환한다.")
        void givenSportString_whenFrom_thenReturnsSportType() {
            ContentType result = ContentType.from("sport");

            assertThat(result).isEqualTo(ContentType.sport);
        }

        @Test
        @DisplayName("대문자 'MOVIE'를 ContentType.movie로 변환한다. (lowercase 처리)")
        void givenUpperCaseMovieString_whenFrom_thenReturnsMovieType() {
            ContentType result = ContentType.from("MOVIE");

            assertThat(result).isEqualTo(ContentType.movie);
        }

        @Test
        @DisplayName("대문자 'SPORT'를 ContentType.sport로 변환한다. (lowercase 처리)")
        void givenUpperCaseSportString_whenFrom_thenReturnsSportType() {
            ContentType result = ContentType.from("SPORT");

            assertThat(result).isEqualTo(ContentType.sport);
        }

        @ParameterizedTest
        @DisplayName("알 수 없는 타입은 ContentInvalidTypeException을 던진다.")
        @ValueSource(strings = {"unknown", "anime", "documentary", "", "123"})
        void givenInvalidTypeString_whenFrom_thenThrowsContentInvalidTypeException(String invalidType) {
            assertThatThrownBy(() -> ContentType.from(invalidType))
                    .isInstanceOf(ContentInvalidTypeException.class);
        }

        @Test
        @DisplayName("혼합 대소문자 'TvSeries'를 ContentType.tvSeries로 변환한다.")
        void givenMixedCaseTvSeriesString_whenFrom_thenReturnsTvSeriesType() {
            ContentType result = ContentType.from("TvSeries");

            assertThat(result).isEqualTo(ContentType.tvSeries);
        }

        @Test
        @DisplayName("공백 문자열은 ContentInvalidTypeException을 던진다.")
        void givenBlankString_whenFrom_thenThrowsContentInvalidTypeException() {
            assertThatThrownBy(() -> ContentType.from(" "))
                    .isInstanceOf(ContentInvalidTypeException.class);
        }
    }
}