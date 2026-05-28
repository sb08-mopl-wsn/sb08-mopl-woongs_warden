package com.mopl.mopl;

import com.mopl.mopl.infrastructure.elasticsearch.repository.ContentSearchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@SpringBootTest
@Testcontainers
class MoplApplicationTests {

	@Container
	static GenericContainer<?> redisContainer = new GenericContainer<>("redis:8-alpine")
			.withExposedPorts(6379);

	@DynamicPropertySource
	static void redisProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.host", redisContainer::getHost);
		registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort);
	}

	@MockitoBean
	ContentSearchRepository contentSearchRepository;

	@MockitoBean
	ElasticsearchOperations elasticsearchOperations;

	@Test
	void contextLoads() {
	}
}
