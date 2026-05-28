package com.mopl.mopl.infrastructure.kafka.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.infrastructure.kafka.ContentIndexMessage;
import com.mopl.mopl.infrastructure.s3.S3ImageStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class ContentEventListener
{
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final S3ImageStorage s3ImageStorage;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIndex(ContentIndexEvent event) {
        try {
            Content content = event.content();
            String message = objectMapper.writeValueAsString(
                    new ContentIndexMessage(content.getId(), ContentIndexMessage.ActionType.INDEX));
            kafkaTemplate.send("content-index", content.getId().toString(), message);
            log.info("Kafka 메시지 발행: contentId={}, action={}", content.getId(), ContentIndexMessage.ActionType.INDEX);
        } catch (Exception e) {
            log.warn("Kafka 인덱스 메시지 발행 실패: contentId={}", event.content().getId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDelete(ContentDeleteEvent event) {
        if (event.thumbnailKey() != null) {
            try {
                s3ImageStorage.delete(event.thumbnailKey());
            } catch (Exception e) {
                log.warn("S3 이미지 삭제 실패: key={}", event.thumbnailKey(), e);
            }
        }

        try {
            String message = objectMapper.writeValueAsString(
                    new ContentIndexMessage(event.contentId(), ContentIndexMessage.ActionType.DELETE));
            kafkaTemplate.send("content-index", event.contentId().toString(), message);
            log.info("Kafka 메시지 발행: contentId={}, action={}", event.contentId(), ContentIndexMessage.ActionType.DELETE);
        } catch (Exception e) {
            log.warn("Kafka 인덱스 삭제 메시지 발행 실패: contentId={}", event.contentId(), e);
        }
    }
}
