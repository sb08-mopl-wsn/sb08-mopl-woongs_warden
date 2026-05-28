package com.mopl.mopl.global.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    /**
     * 카프카 컨슈머 에러 핸들러 (재시도 로직)
     * 메시지 소비 중 DB 에러나 일시적인 네트워크 오류가 발생했을 때,
     * 해당 메시지를 버리지 않고 2초 대기 후 최대 3번까지 재시도
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
        KafkaTemplate<String, Object> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // Recoverer 생성. 재시도 실패 시 해당 메시지를 원본토픽명.DLT로 발행
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);

        // 에러 발생 시 2000ms(2초) 간격으로 최대 3번 재시도 (총 4번 시도) 후, Recoverer 실행
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(2000L, 3L));
        errorHandler.addNotRetryableExceptions(JsonProcessingException.class);

        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}