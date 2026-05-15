package com.mopl.mopl.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig
{
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager simpleCacheManager = new SimpleCacheManager();

        simpleCacheManager.setCaches(List.of(
                new CaffeineCache("content",
                        Caffeine.newBuilder()
                                .maximumSize(500)
                                .expireAfterWrite(10, TimeUnit.MINUTES)
                                .recordStats()
                                .build()),
                new CaffeineCache("contents",
                        Caffeine.newBuilder()
                                .maximumSize(100)
                                .expireAfterWrite(5, TimeUnit.MINUTES)
                                .recordStats()
                                .build())
        ));

        return simpleCacheManager;
    }
}
