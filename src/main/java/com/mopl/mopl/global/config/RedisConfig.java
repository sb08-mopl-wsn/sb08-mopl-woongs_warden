package com.mopl.mopl.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.component.UserUnbanProcessor;
import com.mopl.mopl.global.redis.component.RedisKeyExpiredListener;
import com.mopl.mopl.global.redis.component.WatchingSessionRedisConsumer;
import com.mopl.mopl.global.redis.service.RedisPublisher;
import com.mopl.mopl.global.redis.service.RedisSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    @Bean
    public ChannelTopic watchTopic() {
        return new ChannelTopic("mopl-contents-watch-channel");
    }

    @Bean
    public ChannelTopic chatTopic() {
        return new ChannelTopic("mopl-contents-chat-channel");
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory, ObjectMapper objectMapper) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        redisTemplate.setConnectionFactory(factory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(serializer);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    public RedisKeyExpiredListener redisKeyExpiredListener(
            RedisMessageListenerContainer redisMessageListenerContainer,
            UserRepository userRepository,
            UserUnbanProcessor userUnbanProcessor
    ) {
        return new RedisKeyExpiredListener(redisMessageListenerContainer, userRepository, userUnbanProcessor);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory factory,
            RedisSubscriber redisSubscriber,
            WatchingSessionRedisConsumer watchingConsumer,
            ChannelTopic watchTopic,
            ChannelTopic chatTopic
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);

        // SSE 토픽과 WS 토픽을 리스닝
        container.addMessageListener(redisSubscriber, RedisPublisher.SSE_TOPIC);
        container.addMessageListener(redisSubscriber, RedisPublisher.WS_TOPIC);

        container.addMessageListener(watchingConsumer, watchTopic);
        container.addMessageListener(watchingConsumer, chatTopic);

        return container;
    }
}
