package com.mopl.mopl.infrastructure.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class AiConfig
{
    @Value("${spring.ai.google.genai.timeout.connect-timeout}")
    private Duration connectTimeout;

    @Value("${spring.ai.google.genai.timeout.read-timeout}")
    private Duration readTimeout;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
