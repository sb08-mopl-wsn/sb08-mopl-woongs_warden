package com.mopl.mopl.infrastructure.config;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.concurrent.TimeUnit;

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
                .build();
    }

    @Bean
    public RestClient sportsdbRestClient() {
        return RestClient.builder()
                .baseUrl(sportsdbBaseUrl)
                .requestFactory(clientHttpRequestFactory())
                .build();
    }

    private ClientHttpRequestFactory clientHttpRequestFactory() {
        ConnectionConfig connConfig = ConnectionConfig.custom()
                .setConnectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                .setSocketTimeout(readTimeout,  TimeUnit.MILLISECONDS)
                .build();

        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        connManager.setDefaultConnectionConfig(connConfig);

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connManager)
                .build();

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }
}
