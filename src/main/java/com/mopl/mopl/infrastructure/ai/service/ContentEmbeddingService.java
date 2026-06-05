package com.mopl.mopl.infrastructure.ai.service;

import com.mopl.mopl.domain.content.entity.Content;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class ContentEmbeddingService
{
    private final EmbeddingModel embeddingModel;
    private final MeterRegistry meterRegistry;

    public void generateAndSave(Content content, String source) {
        try {
            String text = buildEmbeddingText(content);
            float[] embedding = embeddingModel.embed(text);
            content.updateEmbedding(embedding);
            log.debug("[임베딩 생성] contentId={}", content.getId());
        } catch (Exception e) {
            log.warn("[임베딩 생성 실패] contentId={}, error={}", content.getId(), e.getMessage(), e);
            meterRegistry.counter("mopl.batch.embedding.failed", "source", source).increment();
        }
    }

    private String buildEmbeddingText(Content content) {
        return String.format("%s %s %s",
                content.getTitle(),
                content.getDescription() != null ? content.getDescription() : "",
                content.getTags() != null ? String.join(" ", content.getTags()) : ""
        );
    }
}
