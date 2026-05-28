package com.mopl.mopl.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.domain.content.repository.ContentRepository;
import com.mopl.mopl.infrastructure.elasticsearch.ContentIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ContentIndexConsumer
{
    private final ContentIndexService contentIndexService;
    private final ContentRepository contentRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "content-index", groupId = "mopl-content-index")
    public void consume(String message) {
        try {
            ContentIndexMessage contentIndexMessage = objectMapper.readValue(message, ContentIndexMessage.class);

            switch (contentIndexMessage.action()) {
                case INDEX -> contentRepository.findById(contentIndexMessage.contentId()).ifPresent(contentIndexService::index);
                case DELETE -> contentIndexService.delete(contentIndexMessage.contentId());
            }
        } catch (Exception e) {
            log.error("콘텐츠 인덱스 메시지 처리 실패: {}", message, e);
            throw new RuntimeException(e);
        }
    }
}
