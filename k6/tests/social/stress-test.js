import {sleep} from 'k6';
import {login} from '../../config/social_config.js';
import { runWarmup, runSubscriber, runTrigger } from '../../scenarios/_social.js';

/**
 * Stress Test — 한계점까지 점진적으로 올려서 어디서 깨지는지 확인
 *
 * 1. Warm-up:    0 → 30 VU   (1분)
 * 2. Normal:    30 VU        (3분)
 * 3. High:      30 → 100 VU  (1분)
 * 4. Peak:      100 VU       (3분)
 * 5. Overload:  100 → 200 VU (1분)
 * 6. Max:       200 VU       (3분)
 * 7. Recovery:  200 → 0 VU   (1분)
 */

// 💡 [수정 포인트 1] 톰캣 스레드 풀과 DB 커넥션을 최대 200명까지 압박할 조회 스케줄
const subscriberStages = [
    { duration: '1m', target: 30 },
    { duration: '3m', target: 30 },
    { duration: '1m', target: 100 },
    { duration: '3m', target: 100 },
    { duration: '1m', target: 200 }, // 최대 200명까지 증가
    { duration: '3m', target: 200 }, // 200명 유지하며 서버 한계 측정
    { duration: '1m', target: 0 },
];

// 💡 [수정 포인트 2] 데이터 충돌을 완전히 피하면서 시스템 압박 속 비즈니스 지연 속도를 측정할 1인 스케줄
const triggerStages = [
    { duration: '1m', target: 1 },
    { duration: '3m', target: 1 },
    { duration: '1m', target: 1 },
    { duration: '3m', target: 1 },
    { duration: '1m', target: 1 },
    { duration: '3m', target: 1 },
    { duration: '1m', target: 0 },
];

export const options = {
    scenarios: {
        social_warmup: {
            executor: 'ramping-vus',
            stages: [{ duration: '1m', target: 10 }], // 1분 동안 10명으로 가볍게 예열
            exec: 'socialWarmupTest',
        },
        social_subscribers: {
            executor: 'ramping-vus',
            stages: subscriberStages, // 💡 분리된 조회용 압박 스케줄 매핑
            exec: 'socialSubscriberTest',
            startTime: '1m', // 웜업이 끝나는 1분 뒤 정식 출발
        },
        social_triggers: {
            executor: 'ramping-vus',
            stages: triggerStages, // 💡 분리된 1인 코어 로직 측정 스케줄 매핑
            exec: 'socialTriggerTest',
            startTime: '1m',
        },
    },
    thresholds: {
        // 스트레스 테스트는 임계치 돌파를 확인하는 목적이므로 지연 시간 기준 완화 및 오류율 임계치 제외 권장
        http_req_duration: ['p(95)<2000', 'p(99)<4000'],

        'http_req_duration{name:GET /api/contents}': ['p(95)<300'],
        'http_req_duration{name:GET /api/contents/{contentId}}': ['p(95)<300'],
        'http_req_duration{name:GET /api/contents?keyword=}': ['p(95)<1000'],
    },
};

export function setup() {
    return login();
}

// ── 테스트 매핑 함수 ──
export function socialWarmupTest(data) {
    runWarmup(data.accessToken, data.csrfToken);
    sleep(1);
}

export function socialSubscriberTest(data) {
    runSubscriber(data.accessToken, data.csrfToken);
    sleep(1);
}

export function socialTriggerTest(data) {
    runTrigger(data.accessToken, data.csrfToken);
    sleep(1);
}