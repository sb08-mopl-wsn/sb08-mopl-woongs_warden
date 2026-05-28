package com.mopl.mopl;

import com.mopl.mopl.infrastructure.elasticsearch.repository.ContentSearchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test")
@SpringBootTest
class MoplApplicationTests
{
	@MockitoBean
	ContentSearchRepository contentSearchRepository;

	@MockitoBean
	ElasticsearchOperations elasticsearchOperations;

	@MockitoBean
	RedisConnectionFactory redisConnectionFactory;

	@MockitoBean
	RedisMessageListenerContainer redisMessageListenerContainer;
	@Test
	void contextLoads() {
	}
}
