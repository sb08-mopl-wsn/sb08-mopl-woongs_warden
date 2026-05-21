package com.mopl.mopl.infrastructure.ai;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@RequiredArgsConstructor
@Slf4j
@Component
public class AiPerformanceRecorder
{
    private final MeterRegistry meterRegistry;

    public <T> T record(String stage, Supplier<T> task) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            T result = task.get();
            long elapsed = sample.stop(buildTimer(stage, "success"));
            log.info("[AI Recommend] {} - 완료 {}ms", stage, toMs(elapsed));
            return result;
        } catch (Exception e) {
            long elapsed = sample.stop(buildTimer(stage, "failure"));
            log.info("[AI Recommend] {} - 실제 {}ms | {}", stage, toMs(elapsed), e.getMessage());
            throw e;
        }
    }

    public void record(String stage, Runnable task) {
        record (stage, () -> {
            task.run();
            return null;
        });
    }

    public void recordTokenUsage(String stage, Usage usage) {
        if (usage == null) {
            log.warn("[AI Recommend] {} 토큰 사용량 정보가 없어 기록을 건너뜁니다.", stage);
            return;
        }

        double input = Math.max(0, usage.getPromptTokens());
        double output = Math.max(0, usage.getCompletionTokens());

        Counter.builder("mopl.ai.recommend.tokens")
                .tag("stage", stage)
                .tag("type", "input")
                .register(meterRegistry)
                .increment(input);

        Counter.builder("mopl.ai.recommend.tokens")
                .tag("stage", stage)
                .tag("type", "output")
                .register(meterRegistry)
                .increment(output);

        log.info("[AI Recommend] {} 토큰 사용량 - input: {} | output: {} | total: {}", stage, input, output, input + output);
    }

    private Timer buildTimer(String stage, String outcome) {
        return Timer.builder("ai.recommend.stage.duration")
                .tag("stage", stage)
                .tag("outcome", outcome)
                .register(meterRegistry);
    }

    private double toMs(long nanos) {
        return nanos / 1_000_000.0;
    }
}
