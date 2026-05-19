package com.mopl.mopl.global.component;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
public class BadWordFilter {

    private Trie trie;

    // 정규식 패턴을 미리 컴파일하여 매 글자마다 컴파일 되는 비용을 제거
    private static final Pattern ALPHANUMERIC_KOREAN = Pattern.compile("[a-zA-Zㄱ-ㅎㅏ-ㅣ가-힣]");

    @Value("classpath:badwords.txt")
    private Resource badWordsResource;

    @PostConstruct
    public void init() {
        try {
            // 파일에서 단어 목록 읽어옴. (UTF-8 인코딩)
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(badWordsResource.getInputStream(), StandardCharsets.UTF_8)
            );
            List<String> badWords = reader.lines()
                    .map(String::trim)
                    .filter(word -> !word.isEmpty())
                    .toList();

            // Aho-Corasick Trie 자료구조 빌드
            Trie.TrieBuilder builder = Trie.builder()
                    .ignoreOverlaps()
                    .ignoreCase();

            for (String word : badWords) {
                builder.addKeyword(word);
            }

            this.trie = builder.build();
            log.info("[BadWordFilter] 욕설 사전에서 총 {}개의 단어가 로드되었습니다.", badWords.size());
        } catch (Exception e) {
            log.error("[BadWordFilter] 욕설 사전을 불러오는 중 오류 발생", e);
            // 파일 로드 실패 시 빈 Trie를 생성하여 서버가 다운되는 것을 방지한다.
            this.trie = Trie.builder().build();
        }
    }


    // 문자열 중간에 특수 문자, 숫자, 공백이 섞인 욕설까지 추적하여 마스킹한다.
    public String maskBadWord(String message) {
        if (message == null || message.trim().isEmpty()) {
            return message;
        }

        // 정규화 전처리, 원본 인덱스를 추적할 매핑 배열 생성
        StringBuilder normalized = new StringBuilder();
        List<Integer> originalIndices = new ArrayList<>();

        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);

            if (ALPHANUMERIC_KOREAN.matcher(String.valueOf(c)).matches()) {
                normalized.append(c);
                // 정규화된 글자가 원분 문자열의 몇 번째 인덱스였는지 기록한다.
                originalIndices.add(i);
            }
        }

        // 정규화된 문자열에서 욕설을 탐색한다.
        Collection<Emit> emits = trie.parseText(normalized.toString());
        if (emits.isEmpty()) {
            // 정규화에서 욕설이 없는 경우 원본 그대로 반환
            return message;
        }

        StringBuilder maskedMessage = new StringBuilder(message);

        for (Emit emit : emits) {
            int normStart = emit.getStart();
            int normEnd = emit.getEnd();

            int originStart = originalIndices.get(normStart);
            int originEnd = originalIndices.get(normEnd);

            // 원본에서 욕설 시작점부터 끝점까지 '*'로 치환
            for (int j=originStart; j<= originEnd; j++) {
                maskedMessage.setCharAt(j, '*');
            }
        }
        return maskedMessage.toString();
    }
}
