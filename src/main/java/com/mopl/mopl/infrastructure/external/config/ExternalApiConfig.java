package com.mopl.mopl.infrastructure.external.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

@Configuration
public class ExternalApiConfig
{
    @Value("${external.tmdb.api.base-url}")
    private String tmdbBaseUrl;

    @Value("${external.tmdb.api.token}")
    private String tmdbToken;

    @Value("${external.sportsdb.api.base-url}")
    private String sportsdbBaseUrl;

    // 현재는 공통 타임아웃으로 사용 -> 타임 아웃 테스트 후 지정
    @Value("${external.timeout.connect-timeout}")
    private int connectTimeout;

    @Value("${external.timeout.read-timeout}")
    private int readTimeout;

    @Bean
    public RestClient tmdbRestClient() {
        return RestClient.builder()
                .baseUrl(tmdbBaseUrl)
                .defaultHeader("Authorization", "Bearer " + tmdbToken)
                .requestFactory(clientHttpRequestFactory())
                .messageConverters(converters -> {
                    converters.clear();
                    converters.add(new MappingJackson2HttpMessageConverter(externalApiObjectMapper()));
                })
                .build();
    }

    @Bean
    public RestClient sportsdbRestClient() {
        return RestClient.builder()
                .baseUrl(sportsdbBaseUrl)
                .requestFactory(clientHttpRequestFactory())
                .build();
    }

    private ObjectMapper externalApiObjectMapper() {
        return new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return factory;
    }
}
