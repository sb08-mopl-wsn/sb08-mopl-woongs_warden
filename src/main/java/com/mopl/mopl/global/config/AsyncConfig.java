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

    // TODO: 추후 추가할 예정
//    @Override
//    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
//    }

    @Bean(name = WATCHING_SESSION_EXECUTOR)
    public Executor watchingSessionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("ws-watch-");

        // 큐가 꽉 찼을 경우 호출한 스레드가 직접 처리하게 하여 누락 방지
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 종료 시 대기 중인 작업 완료 설정
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);

        executor.initialize();
        return executor;
    }

    @Bean(name = NOTIFICATION_EXECUTOR)
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500); // 알림은 많이 발생할 확률이 높으므로 큐크기를 여유롭게 설정
        executor.setThreadNamePrefix("noti-async-");

        // 큐 500개도 꽉 차면, 알림을 발생시킨 스레드가 직접 처리하게 함
        // TODO: 추후 Kafka 기반 알림 비동기 큐가 도입되고 Retry 로직 구축 후에는 메인 스레드 지연을 막기 위해 AbortPolicy로 변경할 것
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        return executor;
    }
}
