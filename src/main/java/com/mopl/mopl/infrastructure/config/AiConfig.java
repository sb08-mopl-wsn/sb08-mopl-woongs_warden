package com.mopl.mopl.infrastructure.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
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
}
