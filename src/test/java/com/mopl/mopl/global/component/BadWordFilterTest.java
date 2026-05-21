package com.mopl.mopl.global.component;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BadWordFilterTest {

    @InjectMocks
    private BadWordFilter badWordFilter;

    @Mock
    private Resource badWordsResource;

    @BeforeEach
    void setUp() throws IOException {
        // 테스트용 욕설 데이터 설정
        String testBadWords = "바보\n멍청이\nbitch";
        InputStream inputStream = new ByteArrayInputStream(testBadWords.getBytes(StandardCharsets.UTF_8));
        given(badWordsResource.getInputStream()).willReturn(inputStream);

        badWordFilter.init();
    }

    @Test
    @DisplayName("기본적인 욕설이 포함된 경우 해당 단어를 마스킹한다.")
    void maskBadWord_Basic() {
        // given
        String message = "너는 정말 바보야";

        // when
        String result = badWordFilter.maskBadWord(message);

        // then
        assertThat(result).isEqualTo("너는 정말 **야");
    }

    @Test
    @DisplayName("욕설 사이에 숫자나 특수문자가 섞여있어도 마스킹한다.")
    void maskBadWord_WithSpecialChars() {
        // given
        String message1 = "너는 바123보";
        String message2 = "너는 멍!청?이";

        // when
        String result1 = badWordFilter.maskBadWord(message1);
        String result2 = badWordFilter.maskBadWord(message2);

        // then
        assertThat(result1).isEqualTo("너는 *****");
        assertThat(result2).isEqualTo("너는 *****");
    }

    @Test
    @DisplayName("영문 욕설의 경우 대소문자를 구분하지 않고 마스킹한다.")
    void maskBadWord_EnglishCaseInsensitive() {
        // given
        String message1 = "You are lucky BITCH";
        String message2 = "You are lucky bItCh";

        // when
        String result1 = badWordFilter.maskBadWord(message1);
        String result2 = badWordFilter.maskBadWord(message2);

        // then
        assertThat(result1).isEqualTo("You are lucky *****");
        assertThat(result2).isEqualTo("You are lucky *****");
    }

    @Test
    @DisplayName("욕설이 포함되지 않은 문자열은 그대로 반환한다.")
    void maskBadWord_NoBadWord() {
        // given
        String message = "안녕하세요 반가워요";

        // when
        String result = badWordFilter.maskBadWord(message);

        // then
        assertThat(result).isEqualTo("안녕하세요 반가워요");
    }

    @Test
    @DisplayName("중첩된 욕설이 있는 경우 모두 마스킹한다.")
    void maskBadWord_Multiple() {
        // given
        String message = "바보 멍청이 bitch";

        // when
        String result = badWordFilter.maskBadWord(message);

        // then
        assertThat(result).isEqualTo("** *** *****");
    }

    @Test
    @DisplayName("입력값이 null이거나 빈 문자열인 경우 그대로 반환한다.")
    void maskBadWord_EmptyOrNull() {
        // when & then
        assertThat(badWordFilter.maskBadWord(null)).isNull();
        assertThat(badWordFilter.maskBadWord("")).isEqualTo("");
        assertThat(badWordFilter.maskBadWord("   ")).isEqualTo("   ");
    }

    @Test
    @DisplayName("사전 파일에 빈 줄이나 공백만 있는 줄이 포함되어도 정상적으로 필터링하여 로드한다.")
    void init_FiltersEmptyLines() throws IOException {
        // given
        String testBadWords = "바보\n  \n\n멍청이";
        InputStream inputStream = new ByteArrayInputStream(testBadWords.getBytes(StandardCharsets.UTF_8));
        given(badWordsResource.getInputStream()).willReturn(inputStream);

        // when
        badWordFilter.init();

        // then
        // 빈 줄이 필터링되지 않았다면 빈 문자열이 키워드로 등록되어 모든 문자가 마스킹될 수 있음
        // 정상 필터링 시 "바보", "멍청이"만 등록됨
        String message = "안녕";
        assertThat(badWordFilter.maskBadWord(message)).isEqualTo("안녕");
        assertThat(badWordFilter.maskBadWord("바보")).isEqualTo("**");
    }

    @Test
    @DisplayName("사전 로드 중 예외가 발생하면 빈 Trie를 생성하고 마스킹을 수행하지 않는다.")
    void init_Exception() throws IOException {
        // given
        given(badWordsResource.getInputStream()).willThrow(new RuntimeException("사전 로드 중 예외 발생"));

        // when
        badWordFilter.init();

        // then
        String message = "너는 바보";
        String result = badWordFilter.maskBadWord(message);
        assertThat(result).isEqualTo(message);
    }
}
