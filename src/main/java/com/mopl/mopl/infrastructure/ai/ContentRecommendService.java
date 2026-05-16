package com.mopl.mopl.infrastructure.ai;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.repository.ContentRepository;
import com.mopl.mopl.infrastructure.ai.dto.AiRecommendation;
import com.mopl.mopl.infrastructure.ai.dto.ContentRecommendRequest;
import com.mopl.mopl.infrastructure.ai.dto.ContentRecommendResponse;
import com.mopl.mopl.infrastructure.ai.exception.AiParseFailedException;
import com.mopl.mopl.infrastructure.ai.exception.AiTimeoutExcpetion;
import com.mopl.mopl.infrastructure.ai.exception.AiUnavailableException;
import com.mopl.mopl.infrastructure.s3.ImageUrlConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ContentRecommendService
{
    private final ChatClient chatClient;
    private final ContentRepository contentRepository;
    private final ImageUrlConverter imageUrlConverter;

    private static final String SYSTEM_PROMPT = """
            너는 MOLE 플랫폼의 콘텐츠 추천 어시스턴트야.
            영화, 드라마, 스포츠 콘텐츠에 대한 질문에만 답변해.
            아래 콘텐츠 목록에서만 추천하고, 목록에 없는 콘텐츠는 절대 추천하지 마.
            반드시 아래 JSON 형식으로만 응답해:
            [{"id": "UUID", "reason": "추천 이유"}]
            최대 5개까지 추천해.
            콘텐츠와 관련 없는 질문에는 빈 배열 []로 응답해.
            """;

    public List<ContentRecommendResponse> recommend(ContentRecommendRequest request) {
        List<Content> contents = contentRepository.findAll();
        String contentContext = buildContentContext(contents);

        List<AiRecommendation> recommendations;
        try {
            recommendations = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user("콘텐츠 목록:\n" + contentContext + "\n\n사용자 질문: " + request.prompt())
                    .call()
                    .entity(new ParameterizedTypeReference<>() {});
        } catch (ResourceAccessException e) {
            throw new AiTimeoutExcpetion();
        } catch (NonTransientAiException e) {
            throw new AiUnavailableException();
        } catch (Exception e) {
            throw new AiParseFailedException();
        }

        return recommendations.stream()
                .map(rec -> {
                    Content content = contentRepository.findById(rec.id()).orElse(null);

                    if (content == null) return null;

                    return new ContentRecommendResponse(
                            content.getId(),
                            content.getTitle(),
                            content.getContentType().name(),
                            content.getAvgRating(),
                            imageUrlConverter.convert(content.getThumbnailKey()),
                            rec.reason()
                    );
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private String buildContentContext(List<Content> contents) {
        return contents.stream()
                .map(c -> "ID: %s | %s | %s | %s | %s점".formatted(
                        c.getId(), c.getTitle(), c.getContentType(), String.join(",", c.getTags()), c.getAvgRating()
                ))
                .collect(Collectors.joining("\n"));
    }
}
