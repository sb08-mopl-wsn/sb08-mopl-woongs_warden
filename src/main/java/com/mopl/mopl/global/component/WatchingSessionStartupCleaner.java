package com.mopl.mopl.global.component;

import com.mopl.mopl.domain.content.repository.ContentRepository;
import com.mopl.mopl.domain.watchingSession.repository.WatchingSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatchingSessionStartupCleaner implements ApplicationRunner {

    private final WatchingSessionRepository sessionRepository;
    private final ContentRepository contentRepository;
    private final TransactionTemplate transactionTemplate;

    private static final int CHUNK_SIZE = 1000;

    /*
     * 서버 시작 시 고아 시청 세션과 콘텐츠 시청자 수를 초기화한다.
     * 분할 처리(Chunk)를 통해 DB 락 타임을 최소화하고 메모리를 방어한다.
     * OOM, JVM 강제 종료 등 비정상 종료 시 DB에 잔류하는 시청 세션을 정리한다
     * 현재 기준점은 단일 인스턴스, 분산환경일 경우 수정할 예정
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("[WatchingSession StartUp] 데이터 초기화 작업 시작...");
        cleanUpWatchingSessions();
        resetWatcherCounts();
        log.info("[WatchingSession StartUp] 데이터 초기화 작업 완료");
    }

    private void cleanUpWatchingSessions() {

        int totalDeleted = 0;
        while (true) {
            Integer deletedCount = transactionTemplate.execute(status ->
                    sessionRepository.deleteSessionsInBatches(CHUNK_SIZE)
            );

            if (deletedCount == null || deletedCount == 0) {
                break;
            }

            totalDeleted += deletedCount;
            log.debug("... 진행중: 고아 시청 세션 {}건 삭제 완료", totalDeleted);
        }

        if (totalDeleted > 0) {
            log.info("[WatchingSession StartUp] 고아 시청 세션 총 {}건 삭제 완료", totalDeleted);
        }
    }

    private void resetWatcherCounts() {

        int totalUpdated = 0;
        while (true) {
            Integer updatedCount = transactionTemplate.execute(status ->
                    contentRepository.resetWatcherCountsInBatches(CHUNK_SIZE)
            );

            if (updatedCount == null || updatedCount == 0) {
                break;
            }
            totalUpdated += updatedCount;
            log.debug("... 진행중: 콘텐츠 시청자 수 {}건 초기화 완료", totalUpdated);
        }

        if (totalUpdated > 0) {
            log.info("[WatchingSession StartUp] 콘텐츠 시청자 수 총 {}건 초기화 완료", totalUpdated);
        }
    }
}
