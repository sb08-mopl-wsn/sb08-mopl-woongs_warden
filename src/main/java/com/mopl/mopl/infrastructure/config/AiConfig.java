package com.mopl.mopl.infrastructure.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.google.genai.GoogleGenAiEmbeddingConnectionDetails;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingModel;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;

@Configuration
public class AiConfig
{
    @Value("${spring.ai.google.genai.timeout.connect-timeout}")
    private Duration connectTimeout;

    @Value("${spring.ai.google.genai.timeout.read-timeout}")
    private Duration readTimeout;

    @Value("${spring.ai.google.genai.api-key}")
    private String geminiApiKey;

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    @Bean("aiRestClientBuilder")
    public RestClient.Builder aiRestClientBuilder() {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .connectTimeout(connectTimeout)
                        .build());
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .requestFactory(requestFactory);
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        GoogleGenAiEmbeddingConnectionDetails connectionDetails =
                GoogleGenAiEmbeddingConnectionDetails.builder()
                        .apiKey(geminiApiKey)
                        .build();

        GoogleGenAiTextEmbeddingOptions options = GoogleGenAiTextEmbeddingOptions.builder()
                .model("gemini-embedding-001")
                .dimensions(768)
                .build();

        return new GoogleGenAiTextEmbeddingModel(connectionDetails, options);
    }
}
