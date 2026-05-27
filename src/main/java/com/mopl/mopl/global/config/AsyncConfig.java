package com.mopl.mopl.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    public static final String WATCHING_SESSION_EXECUTOR = "watchingSessionExecutor";
    public static final String NOTIFICATION_EXECUTOR = "notificationExecutor";
    public static final String DIRECT_MESSAGE_EXECUTOR = "directMessageExecutor";
    public static final String USER_EXECUTOR = "userExecutor";
    public static final String AI_RECOMMEND_EXECUTOR = "aiRecommendExecutor";

    @Bean(name = AI_RECOMMEND_EXECUTOR)
    public Executor aiRecommendExecutor() {
        return createExecutor(2, 5, 50, "ai-rec-");
    }

    @Bean(name = WATCHING_SESSION_EXECUTOR)
    public Executor watchingSessionExecutor() {
        return createExecutor(5, 10, 200, "ws-watch-");
    }

    @Bean(name = NOTIFICATION_EXECUTOR)
    public Executor notificationExecutor() {
        // 알림은 많이 발생할 확률이 높으므로 큐 크기(500)를 여유롭게 설정
        // TODO: 추후 Kafka 기반 알림 비동기 큐가 도입되고 Retry 로직 구축 후에는 메인 스레드 지연을 막기 위해 AbortPolicy로 변경할 것
        return createExecutor(3, 10, 500, "noti-async-");
    }

    @Bean(name = DIRECT_MESSAGE_EXECUTOR)
    public Executor directMessageExecutor() {
        return createExecutor(3, 10, 200, "dm-async-");
    }

    @Bean(name = USER_EXECUTOR)
    public Executor userExecutor() {
        return createExecutor(3, 10, 100, "user-");
    }

    // 헬퍼 메서드
    private Executor createExecutor(int corePoolSize, int maxPoolSize, int queueCapacity, String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);

        // 큐가 꽉 찼을 경우 호출한 스레드가 직접 처리하게 함
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 종료 시 대기 중인 작업 완료 설정
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);

        executor.initialize();
        return executor;
    }
}
